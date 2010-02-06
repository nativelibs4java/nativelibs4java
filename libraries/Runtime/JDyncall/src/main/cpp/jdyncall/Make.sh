#!/bin/bash

#BUILD_CONFIG=debug sh MakeAll.sh clean 

if [[ "$DYNCALL_HOME" == "" ]] ; then
	export DYNCALL_HOME=~/src/dyncall/dyncall ;
fi
	
CURR="`pwd`"
LD=gcc

BUILD_DIR=
echo BUILD_DIR = $BUILD_DIR
echo BUILD_CONFIG = $BUILD_CONFIG
echo LINK_DIRS = $LINK_DIRS

echo $DYNCALL_HOME/dyncall/$BUILD_DIR

echo "# Making dyncall"
cd "$DYNCALL_HOME"
make $@ || exit 1

echo "# Making jdyncall"
cd "$CURR"
make $@ || exit 1

echo "# Making test library"
cd "../../../test/cpp/test"
make $@ || exit 1

for D in build_out/* ; do 
	cp $D/*.dylib $CURR/$D ; 
done

cd "$CURR"

