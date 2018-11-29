package com.grakra

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest

fun main(vararg args: String) {
  CodeGeneratorRequest.parseFrom(System.`in`)?.let {
    System.out.write(saveCodeGenerateRequest(it))
    System.out.flush()
  }
}