package com.grakra

import org.apache.hadoop.hdfs.protocol.proto.ClientNamenodeProtocolProtos
import org.apache.hadoop.hdfs.protocol.proto.HdfsProtos
import org.apache.hadoop.hdfs.qjournal.protocol.QJournalProtocolProtos
import java.io.File

fun main(vararg args: String) {
  File("build/CodeGeneratorRequest.dat").let{
  //File("CodeGeneratorRequest.dat2").let{
    loadCodeGenerateRequest(it)
  }.let{
    ProtoADTSystem(it)
  }.let{
    it.types.map { "${it.key}=>${it.value}" }.joinToString("\n").let{
      //println(it)
    }

    it.pkgMap.map{"${it.key}=>${it.value}"}.joinToString("\n").let{
     // print(it)
    }
  it.getADT(QJournalProtocolProtos.RequestInfoProto::class.java)
  }.let{
    println(it)
  }
}