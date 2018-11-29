#!/bin/bash
yarn_deploy=/home/grakra/byted_workspace/yarn_deploy
export JAVA_HOME=${JAVA_HOME:?"undefined JAVA_HOME"}
export LD_LIBRARY_PATH=${yarn_deploy}/tools/proto250/lib/:$LD_LIBRARY_PATH
${yarn_deploy}/tools/proto250/bin/protoc $@
