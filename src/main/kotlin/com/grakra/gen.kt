package com.grakra

import java.io.File

fun main(vararg args: String) {
  File("build/CodeGeneratorRequest.dat").let{
  //File("CodeGeneratorRequest.dat2").let{
    loadCodeGenerateRequest(it)
  }.let{
    ProtoADTSystem(it)
  }.let{
    it.types.map { "${it.key}=>${it.value}" }.joinToString("\n").let{
      println(it)
    }
  //  it.getTargetFromClass(QJournalProtocolProtos.RequestInfoProto::class.java)
  //}.let{
  //  println(it)
  }
}