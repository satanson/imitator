#!/bin/bash
basedir=$(cd $(dirname ${BASH_SOURCE:-$0});pwd)
export PATH="${basedir}:${PATH}"
cd ${basedir}
mvn assembly:assembly -DskipTests
${basedir}/proto.sh -I hdfs_proto --plugin=protoc-gen-custom=imitator.sh --custom_out=./build hdfs_proto/*.proto
