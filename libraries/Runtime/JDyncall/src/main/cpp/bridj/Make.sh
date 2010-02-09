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

for D in build_out/darwin_universal_gcc_release ; do 
	cp $D/libbridj.dylib ../../resources/darwin
	cp $D/libtest.dylib ../../../test/resources/darwin ;
done

for D in build_out/linux_x86_gcc_release ; do 
	cp $D/bridj.so ../../resources/linux_x86
	cp $D/test.so ../../../test/resources/linux_x86 ;
done

