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
if [[ -d /System/Library/Frameworks/ ]] ; then sh ./configure --target-universal ; 
else sh ./configure ; fi
make $@ || exit 1

echo "# Making bridj"
cd "$CURR"
make $@ || exit 1

echo "# Making test library"
cd "../../../test/cpp/test"
make $@ || exit 1

cd "$CURR"

if [[ -d build_out ]] ; then
	cd build_out

	for D in `ls . | grep _release` ; do
		ARCH_NAME="`echo $D| sed 's/_gcc_release//'`"
		MAIN_OUT="../../../resources/$ARCH_NAME"
	
		echo ARCH_NAME: $ARCH_NAME ;
		TEST_OUT="../../../../test/resources/$ARCH_NAME"
	
		if [[ -d /System/Library/Frameworks/ ]] ; then 
			cp $D/*.dylib $MAIN_OUT
			cp ../../../../test/cpp/test/build_out/$D/*.dylib $TEST_OUT ;
		else 
			cp $D/*.so $MAIN_OUT 
			cp ../../../../test/cpp/test/build_out/$D/*.so $TEST_OUT ;
		fi ;
	
	#	svn add $MAIN_OUT
	#	svn add $TEST_OUT ;
	done ;
fi
