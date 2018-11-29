package com.grakra

import com.google.protobuf.ByteString
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto
import com.google.protobuf.DescriptorProtos.DescriptorProto
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label
import java.io.File

sealed class ProtoADT {
  companion object {
    fun parseField(f: FieldDescriptorProto): LabeledProtoADT {
      val field = when (f.type) {
        Type.TYPE_DOUBLE -> ProtoADT_Double
        Type.TYPE_FLOAT -> ProtoADT_Float
        Type.TYPE_INT64 -> ProtoADT_Int64
        Type.TYPE_UINT64 -> ProtoADT_Uint64
        Type.TYPE_INT32 -> ProtoADT_Int32
        Type.TYPE_FIXED64 -> ProtoADT_Fixed64
        Type.TYPE_FIXED32 -> ProtoADT_Fixed32
        Type.TYPE_BOOL -> ProtoADT_Bool
        Type.TYPE_STRING -> ProtoADT_String
        Type.TYPE_BYTES -> ProtoADT_Bytes
        Type.TYPE_UINT32 -> ProtoADT_Uint32
        Type.TYPE_SFIXED32 -> ProtoADT_Sfixed32
        Type.TYPE_SFIXED64 -> ProtoADT_Sfixed64
        Type.TYPE_SINT32 -> ProtoADT_Sint32
        Type.TYPE_SINT64 -> ProtoADT_Sint64
        Type.TYPE_GROUP, Type.TYPE_MESSAGE, Type.TYPE_ENUM -> {
          ProtoADT_Reference(f.typeName)
        }
      }

      return when (f.label) {
        Label.LABEL_REPEATED -> ProtoADT_Repeated(field, f.number)
        Label.LABEL_OPTIONAL -> ProtoADT_Optional(field, f.number)
        Label.LABEL_REQUIRED -> ProtoADT_Required(field, f.number)
      }
    }

    fun parseFields(d: DescriptorProto): Map<String, LabeledProtoADT> {
      return d.fieldList.filter { it.hasNumber() }.map {
        it!!.name!! to parseField(it)
      }.toMap()
    }
  }
}

sealed class BasicProtoADT : ProtoADT()
sealed class ComplexProtoADT : ProtoADT()

object ProtoADTInvalid : ProtoADT()

object ProtoADT_Double : BasicProtoADT()
object ProtoADT_Float : BasicProtoADT()
object ProtoADT_Int64 : BasicProtoADT()
object ProtoADT_Uint64 : BasicProtoADT()
object ProtoADT_Int32 : BasicProtoADT()
object ProtoADT_Fixed64 : BasicProtoADT()
object ProtoADT_Fixed32 : BasicProtoADT()
object ProtoADT_Bool : BasicProtoADT()
object ProtoADT_String : BasicProtoADT()
object ProtoADT_Bytes : BasicProtoADT()
object ProtoADT_Uint32 : BasicProtoADT()
object ProtoADT_Sfixed32 : BasicProtoADT()
object ProtoADT_Sfixed64 : BasicProtoADT()
object ProtoADT_Sint32 : BasicProtoADT()
object ProtoADT_Sint64 : BasicProtoADT()

data class ProtoADT_Decorated(val type: BasicProtoADT, val generator: () -> Any) : ProtoADT()

sealed class LabeledProtoADT : ProtoADT()
data class ProtoADT_Repeated(val type: ProtoADT, val number: Int) : LabeledProtoADT()
data class ProtoADT_Optional(val type: ProtoADT, val number: Int) : LabeledProtoADT()
data class ProtoADT_Required(val type: ProtoADT, val number: Int) : LabeledProtoADT()

data class ProtoADT_Reference(val typeName: String) : ComplexProtoADT()
sealed class ReferencedProtoADT : ComplexProtoADT()
data class ProtoADT_Enum(
    val typeName: String,
    val pkgName: String,
    val protoName: String,
    val values: Map<String, Int>) : ReferencedProtoADT()

data class ProtoADT_Message(
    val name: String,
    val pkgName: String,
    val protoName: String,
    val fields: Map<String, LabeledProtoADT>) : ReferencedProtoADT()

fun getTypesOfMsg(
    msg: DescriptorProto,
    prefix: String,
    pkgName: String,
    protoName: String): Map<String, ReferencedProtoADT> {

  val types = mutableMapOf<String, ReferencedProtoADT>()
  val name = "$prefix.${msg.name}"
  types[name] = ProtoADT_Message(name, pkgName, protoName, ProtoADT.parseFields(msg))
  types.putAll(getEnumAndMsgTypes(name, protoName, pkgName, msg.enumTypeList, msg.nestedTypeList))
  return types
}

fun getEnumAndMsgTypes(
    prefix: String,
    protoName: String,
    pkgName: String,
    enums: List<EnumDescriptorProto>,
    msgs: List<DescriptorProto>): Map<String, ReferencedProtoADT> {
  val types = mutableMapOf<String, ReferencedProtoADT>()
  enums.forEach { enum ->
    val name = "$prefix.${enum.name}"
    val values = enum.valueList.map {
      it!!.name!! to it!!.number!!
    }.toMap()
    types[name] = ProtoADT_Enum(name, pkgName, protoName, values)
  }
  msgs.forEach { msg ->
    types.putAll(getTypesOfMsg(msg, prefix, pkgName, protoName))
  }
  return types
}

fun getAllTypes(r: CodeGeneratorRequest): Map<String, ReferencedProtoADT> {
  val types = mutableMapOf<String, ReferencedProtoADT>()
  r.protoFileList.forEach { proto ->
    val prefix = proto.`package`?.let { if (it.isEmpty()) "" else ".$it" } ?: ""
    types.putAll(getEnumAndMsgTypes(prefix, proto.name, proto.`package`, proto.enumTypeList, proto.messageTypeList))
  }
  return types
}

fun getPackageMap(r: CodeGeneratorRequest): Map<String, String> {
  return r.protoFileList.map { proto ->
    val javaPkg = proto.options?.javaPackage
    val javaOuterCls = proto.options?.javaOuterClassname
    val prefix = listOf(javaPkg, javaOuterCls).filter { it != null && !it.isEmpty() }.joinToString(".")
    prefix to proto.`package`
  }.toMap()
}

fun loadCodeGenerateRequest(file: File): CodeGeneratorRequest {
  val ip = file.inputStream()
  val req = CodeGeneratorRequest.parseFrom(ip.readBytes())
  ip.close()
  return req
}

fun saveCodeGenerateRequest(req: CodeGeneratorRequest): ByteArray {
  val f = CodeGeneratorResponse.File.newBuilder()
  f.name = "CodeGeneratorRequest.dat"
  f.contentBytes = ByteString.copyFrom(req.toByteArray())
  val outputStream = File("CodeGeneratorRequest.dat2").outputStream()
  outputStream.write(req.toByteArray())
  outputStream.close()
  val resp = CodeGeneratorResponse.newBuilder()
  resp.addFile(f.build())
  return resp.build().toByteArray()
}