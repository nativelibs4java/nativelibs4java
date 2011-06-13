#!/bin/bash

#BUILD_CONFIG=debug sh MakeAll.sh clean 
export MAKE_CMD=make
if [[ "`which gmake`" != "" ]] ; then
	export MAKE_CMD=gmake ;
fi


if [[ "$DYNCALL_HOME" == "" ]] ; then
	export DYNCALL_HOME=~/src/dyncall/dyncall ;
fi

if [[ "$DEBUG" == "1" ]] ; then
	export OUT_PATTERN=debug ;
else
	export OUT_PATTERN=release ;
fi
	
CURR="`pwd`"
LD=gcc
COMPILE_PIC=1
BUILD_DIR=
#echo BUILD_DIR = $BUILD_DIR
#echo BUILD_CONFIG = $BUILD_CONFIG
#echo LINK_DIRS = $LINK_DIRS

#echo $DYNCALL_HOME/dyncall/$BUILD_DIR

#svn diff ~/src/dyncall/dyncall > dyncall.diff
svn diff ~/src/dyncall/dyncall | sed "s/${HOME//\//\\/}\/src\/dyncall\///" > dyncall.diff
#svn diff ~/src/dyncall/dyncall | sed "s/${HOME//\//\\/}\/src\/dyncall\///" | sed -E 's/^(---|\+\+\+)(.*)\(([^)]+)\)/\1\2/' > dyncall.diff

echo "# Making dyncall"
cd "$DYNCALL_HOME"

TARGET=${TARGET:-default}
ANDROID_NDK_HOME=${ANDROID_NDK_HOME:-~/bin/android-ndk-r5b}
case $TARGET in
	android)
		NEEDS_TEST=0
		SHAREDLIB_SUFFIX=so
		sh ./configure --with-androidndk=$ANDROID_NDK_HOME/toolchains/arm-linux-androideabi-4.4.3/prebuilt/darwin-x86/bin/arm-linux-androideabi- --target-arm-arm --with-sysroot=$ANDROID_NDK_HOME/platforms/android-9/arch-arm
		;;
	android-emulator)
		NEEDS_TEST=0
		sh ./configure --tool-androidndk --target-x86
		;;
	ios)
		#export PATH=/Developer/Platforms/iPhoneOS.platform/Developer/usr/bin:$PATH
		#export C_INCLUDE_PATH=/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS4.3.sdk/usr/include
		#export LIBRARY_PATH=/Developer/Platforms/iPhoneOS.platform/Developer/SDKs/iPhoneOS4.3.sdk/usr/lib
		#export CC="gcc -arch arm"
		#export CPPFLAGS
		NEEDS_TEST=1
		export PATH=/Developer/Platforms/iPhoneOS.platform/Developer/usr/bin:$PATH
		sh ./configure --target-iphoneos --with-iphonesdk=4.3
		;;
	default)
		NEEDS_TEST=1
		export PATH=/Developer-old/usr/bin:$PATH
		if [[ -d /System/Library/Frameworks/ && ! -d /Applications/MobilePhone.app ]] ; then sh ./configure --target-universal ; 
		else sh ./configure ; fi
		;;
	*)
		echo "Unknown TARGET : $TARGET
		Valid targets are android, android-emulator and default" && exit 1
	;;	
esac

if [[ -z "$SHAREDLIB_SUFFIX" ]] ; then
	if [[ -d /System/Library/Frameworks/ ]] ; then
		SHAREDLIB_SUFFIX=dylib ;
	else 
		SHAREDLIB_SUFFIX=so ;
	fi ;
fi

$MAKE_CMD $@ || exit 1

echo "# Making bridj"
cd "$CURR"
$MAKE_CMD $@ || exit 1

if [[ "$NEEDS_TEST" == "1" ]] ; then
	echo "# Making test library"
	cd "../../../test/cpp/test"
	$MAKE_CMD $@ || exit 1 ;
fi

cd "$CURR"

if [[ -d build_out ]] ; then
	cd build_out

	for D in `ls . | grep _$OUT_PATTERN` ; do
		ARCH_NAME="`echo $D| sed "s/_gcc_$OUT_PATTERN//"| sed "s/_androidndk_$OUT_PATTERN//"`"
		MAIN_OUT="../../../resources/org/bridj/lib/$ARCH_NAME"
	
		echo ARCH_NAME: $ARCH_NAME ;
		TEST_OUT="../../../../test/resources/org/bridj/lib/$ARCH_NAME"
	
		mkdir -p $MAIN_OUT
		mkdir -p $TEST_OUT
		
		cp $D/*.$SHAREDLIB_SUFFIX $MAIN_OUT
		cp ../../../../test/cpp/test/build_out/$D/*.$SHAREDLIB_SUFFIX $TEST_OUT ;
		
		nm $TEST_OUT/*.so > $TEST_OUT/test.so.nm
		nm $TEST_OUT/*.dylib > $TEST_OUT/test.dylib.nm
		echo "Done for $D" ;
	#	svn add $MAIN_OUT
	#	svn add $TEST_OUT ;
	done ;
fi
