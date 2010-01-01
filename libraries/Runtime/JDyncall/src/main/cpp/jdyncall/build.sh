#!/bin/bash

#BUILD_CONFIG=debug sh MakeAll.sh clean 

export DYNCALL_HOME=~/src/dyncall/dyncall
CURR="`pwd`"

cd "$DYNCALL_HOME"
#make clean
make $@

cd "$CURR"
#make clean
make $@
