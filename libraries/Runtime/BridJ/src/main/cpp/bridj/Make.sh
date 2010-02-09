#!/bin/bash

#BUILD_CONFIG=debug sh MakeAll.sh clean 

if [[ "$DYNCALL_HOME" == "" ]] ; then
	export DYNCALL_HOME=~/src/dyncall/dyncall ;
fi
	
CURR="`pwd`"
LD=gcc

BUILD_DIR=
#echo BUILD_DIR = $BUILD_DIR
#echo BUILD_CONFIG = $BUILD_CONFIG
#echo LINK_DIRS = $LINK_DIRS

#echo $DYNCALL_HOME/dyncall/$BUILD_DIR

svn diff ~/src/dyncall/dyncall > dyncall.diff

echo "# Making dyncall"
cd "$DYNCALL_HOME"
./configure --target-universal
make $@ || exit 1

echo "# Making bridj"
cd "$CURR"
make $@ || exit 1

echo "# Making test library"
cd "../../../test/cpp/test"
make $@ || exit 1

for D in build_out/*_release ; do 
	cp $D/*.so $CURR/$D
	cp $D/*.dylib $CURR/$D ;
done

cd "$CURR"

for D in `ls *_release` ; do
	ARCH_NAME="`echo $D| sed 's/_gcc_release//'`"
	MAIN_OUT="../../../resources/$ARCH_NAME"
	TEST_OUT="../../../../test/resources/$ARCH_NAME"
	
	cp $D/*.dylib $MAIN_OUT
	cp $D/*.so $MAIN_OUT 
	
	cp ../../../../test/cpp/test/$D/*.dylib $TEST_OUT
	cp ../../../../test/cpp/test/$D/*.so $TEST_OUT
	
	svn add $MAIN_OUT
	svn add $TEST_OUT ;
done
