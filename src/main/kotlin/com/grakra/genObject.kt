package com.grakra

import org.apache.hadoop.hdfs.protocol.proto.ClientNamenodeProtocolProtos
import org.apache.hadoop.hdfs.qjournal.protocol.QJournalProtocolProtos
import java.io.File

fun main(){

  val adtSystem = File("build/CodeGeneratorRequest.dat").let{
    //File("CodeGeneratorRequest.dat2").let{
    loadCodeGenerateRequest(it)
  }.let{
    ProtoADTSystem(it)
  }

  val clz = QJournalProtocolProtos.RequestInfoProto::class.java
  //val builder = clz.getMethod("newBuilder").invoke(clz)
  //val builderCls = builder.javaClass
  //val method = builderCls.methods.filter { it.name == "setJournalId" }.first()!!
  //println(method.name)
  //println(method.parameters.size)
  val req = adtSystem.synthesize(clz)
  println(req.toString())
  val clz2 = ClientNamenodeProtocolProtos.CreateRequestProto::class.java
  val builder= ClientNamenodeProtocolProtos.CreateRequestProto.newBuilder()
  //builder.setCryptoProtocolVersion()
  println(adtSystem.synthesize(clz2))
}