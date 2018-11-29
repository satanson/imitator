package com.grakra

import com.google.protobuf.ByteString
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest
import java.util.*
import kotlin.math.abs

class ProtoADTSystem(val codeGeneratorRequest: CodeGeneratorRequest) {
  val types = getAllTypes(codeGeneratorRequest)
  val pkgMap = getPackageMap(codeGeneratorRequest)

  inline fun getADT(clz: Class<*>): ReferencedProtoADT {
    return clz.canonicalName.split('.').let {
      it.dropLast(1).joinToString(".") to it.last()
    }.let { (pkg, name) ->
      listOf(pkgMap[pkg]!!, name).filter { !it.isEmpty() }.joinToString("") { ".$it" }
    }.let {
      types[it]!!
    }
  }

  companion object {
    const val newBuilder: String = "newBuilder"
    const val valueOf: String = "valueOf"
    const val addAll: String = "addAll"
    const val set: String = "set"
    const val build: String = "build"
    const val int: String = "int"
    const val Builder: String = "Builder"
  }

  inline fun <reified T> synthesize(clz: Class<T>): T {
    val adt = getADT(clz)
    return when (adt) {
      is ProtoADT_Enum -> synthesizeEnum(clz, adt)
      is ProtoADT_Message -> synthesizeMessage(clz, adt)
    } as T
  }

  fun synthesizeEnum(clz: Class<*>, adt: ProtoADT_Enum): Any {
    val values = adt.values.values.toTypedArray()
    val valueOfMethod = clz.methods.filter {
      it.name == valueOf
          && it.parameters.size == 1
          && it.parameters[0].type.name == int
    }.first()
    return valueOfMethod.invoke(clz, values[abs(Random().nextInt(values.size))])
  }

  fun synthesizeMessage(clz: Class<*>, adt: ProtoADT_Message): Any {
    val builder = clz.getMethod(newBuilder).invoke(clz)!!
    val builderClz = builder.javaClass
    adt.fields.forEach { (f, fadt) ->
      val setMethods = builderClz.methods.filter {
        it.name == "$set${f.capitalize()}"
      }
      val unarySetMethods = setMethods.filter {
        it.parameters.size == 1
            && !it.parameters[0].type.name.endsWith(Builder)

      }
      val binarySetMethod = setMethods.filter {
        it.parameters.size == 2
            && it.parameters[0].type.name == int
            && !it.parameters[1].type.name.endsWith(Builder)
      }

      val (setMethod, fieldClz) = when {
        !binarySetMethod.isEmpty() -> binarySetMethod.first().let {
          it to it.parameters[1].type
        }
        else -> unarySetMethods.first().let {
          it to it.parameters[0].type
        }
      }

      when (fadt) {
        is ProtoADT_Repeated -> {
          val addAllMethod = builderClz.methods.filter {
            it.name == "$addAll${f.capitalize()}" && it.parameters.size == 1
          }!!.first()
          addAllMethod.invoke(builder, synthesizeRepeated(fieldClz, fadt.type))
        }
        is ProtoADT_Optional -> {
          setMethod.invoke(builder, synthesize(fieldClz, fadt.type))
        }
        is ProtoADT_Required -> {
          setMethod.invoke(builder, synthesize(fieldClz, fadt.type))
        }
      }
    }
    return builderClz.getMethod(build).invoke(builder)
  }

  fun synthesizeRepeated(clz: Class<*>, adt: ProtoADT): List<Any> {
    return Array(abs(Random().nextInt(10))) {
      synthesize(clz, adt)
    }.toList()
  }

  fun synthesize(clz: Class<*>, adt: ProtoADT): Any {
    return when (adt) {
      is BasicProtoADT -> synthesizeBasic(clz, adt)
      is ComplexProtoADT -> synthesizeComplexADT(clz, adt)
      else -> throw Error("panic $adt")
    }
  }

  fun synthesizeBasic(clz: Class<*>, adt: BasicProtoADT): Any {
    val rand = Random()
    return when (adt) {
      ProtoADT_Double -> rand.nextDouble()
      ProtoADT_Float -> rand.nextFloat()

      ProtoADT_Fixed32,
      ProtoADT_Int32,
      ProtoADT_Uint32,
      ProtoADT_Sfixed32,
      ProtoADT_Sint32 -> rand.nextInt()

      ProtoADT_Int64,
      ProtoADT_Uint64,
      ProtoADT_Fixed64,
      ProtoADT_Sfixed64,
      ProtoADT_Sint64 -> rand.nextLong()

      ProtoADT_Bool -> rand.nextBoolean()
      ProtoADT_String -> randomString()
      ProtoADT_Bytes -> ByteString.copyFromUtf8(randomString())
    }
  }

  fun randomString(): String {
    val population = listOf('a'..'z', 'A'..'Z', '0'..'9')
        .flatMap { it.toList() }.toCharArray()
    val rand = Random()
    return String(CharArray(abs(rand.nextInt(100) + 1)) {
      population[abs(rand.nextInt(population.size))]
    })
  }

  fun synthesizeComplexADT(clz: Class<*>, adt: ComplexProtoADT): Any {
    return when (adt) {
      is ProtoADT_Reference -> synthesizeComplexADT(clz, types[adt.typeName]!!)
      is ProtoADT_Enum -> synthesizeEnum(clz, adt)
      is ProtoADT_Message -> synthesizeMessage(clz, adt)
    }
  }

/*
  fun <T> genInstance(clz: Class<T>): T {
    val target = getADT(clz)
    when (target) {
      is ProtoADT_Enum -> {
        target.values.values.toTypedArray().let {
          it[Math.abs(Random().nextInt(it.size))]!! as T
        }
      }
      is ProtoADT_Message -> {
        target.fields.forEach{

        }
      }
    }
  }
*/
}