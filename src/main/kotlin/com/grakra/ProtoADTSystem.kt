package com.grakra

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest

class ProtoADTSystem(val codeGeneratorRequest: CodeGeneratorRequest) {
  val types = getAllTypes(codeGeneratorRequest)
  val pkgMap = getPackageMap(codeGeneratorRequest)

  inline fun <reified T> getTargetFromClass(clz: Class<T>): ReferencedProtoADT {
    return clz.canonicalName.split('.').let{
      it.dropLast(1).joinToString(".") to it.last()
    }.let{(pkg, name) ->
      types[".${pkgMap[pkg]}.$name"]!!
    }
  }

  /*
  inline fun <reified T> genInstance(clz :Class<T>):T {
    val target = getTargetFromClass(clz)
    when(target.msg){
    }
  }
  */
}