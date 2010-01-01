#!/bin/bash

#BUILD_CONFIG=debug sh MakeAll.sh clean 

if [[ "$DYNCALL_HOME" == "" ]] ; then
	export DYNCALL_HOME=~/src/dyncall/dyncall ;
fi
	
CURR="`pwd`"

BUILD_DIR=
echo BUILD_DIR = $BUILD_DIR
echo BUILD_CONFIG = $BUILD_CONFIG
echo LINK_DIRS = $LINK_DIRS

echo $DYNCALL_HOME/dyncall/$BUILD_DIR

echo "# Making dyncall"
cd "$DYNCALL_HOME"
pwd
make $@

echo "# Making jdyncall"
cd "$CURR"
pwd
make $@
