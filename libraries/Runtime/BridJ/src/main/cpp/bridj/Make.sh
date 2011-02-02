#!/bin/bash

#BUILD_CONFIG=debug sh MakeAll.sh clean 

if [[ "$DYNCALL_HOME" == "" ]] ; then
	export DYNCALL_HOME=~/src/dyncall/dyncall ;
fi

if [[ "$DEBUG" == "" ]] ; then
	export OUT_PATTERN=release ;
else
	export OUT_PATTERN=debug ;
fi
	
CURR="`pwd`"
LD=gcc

BUILD_DIR=
#echo BUILD_DIR = $BUILD_DIR
#echo BUILD_CONFIG = $BUILD_CONFIG
#echo LINK_DIRS = $LINK_DIRS

#echo $DYNCALL_HOME/dyncall/$BUILD_DIR

#svn diff ~/src/dyncall/dyncall > dyncall.diff
svn diff ~/src/dyncall/dyncall | sed "s/${HOME//\//\\/}/~/" | sed -E 's/\((revision [0-9]+|working copy)\)//' > dyncall.diff

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

	for D in `ls . | grep _$OUT_PATTERN` ; do
		ARCH_NAME="`echo $D| sed "s/_gcc_$OUT_PATTERN//"`"
		MAIN_OUT="../../../resources/$ARCH_NAME"
	
		echo ARCH_NAME: $ARCH_NAME ;
		TEST_OUT="../../../../test/resources/$ARCH_NAME"
	
		mkdir -p $MAIN_OUT
		mkdir -p $TEST_OUT
		if [[ -d /System/Library/Frameworks/ ]] ; then
			cp $D/*.dylib $MAIN_OUT
			cp ../../../../test/cpp/test/build_out/$D/*.dylib $TEST_OUT ;
		else 
			cp $D/*.so $MAIN_OUT
			cp ../../../../test/cpp/test/build_out/$D/*.so $TEST_OUT ;
		fi 
		
		nm $TEST_OUT/*.so > $TEST_OUT/test.so.nm
		nm $TEST_OUT/*.dylib > $TEST_OUT/test.dylib.nm ;
	#	svn add $MAIN_OUT
	#	svn add $TEST_OUT ;
	done ;
fi
