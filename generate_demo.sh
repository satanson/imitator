#!/bin/bash
basedir=$(cd $(dirname ${BASH_SOURCE:-$0});pwd)
export PATH="${basedir}:${PATH}"
cd ${basedir}
mvn assembly:assembly -DskipTests
rm -fr ${basedir:?"undefined basedir"}/build
mkdir -p ${basedir:?"undefined basedir"}/build
${basedir}/proto.sh -I protos --plugin=protoc-gen-custom=imitator.sh --custom_out=./build $@
