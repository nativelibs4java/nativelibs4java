#!/bin/bash

SRC_HOME=${SRC_HOME:-~/src}
BIN_HOME=${BIN_HOME:-~/bin}

#BUILD_CONFIG=debug sh MakeAll.sh clean 
export MAKE_CMD=make
if [[ "`which gmake`" != "" ]] ; then
	export MAKE_CMD=gmake ;
fi


if [[ "$DYNCALL_HOME" == "" ]] ; then
	export DYNCALL_HOME=$SRC_HOME/dyncall/dyncall ;
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

#svn diff $SRC_HOME/dyncall/dyncall > dyncall.diff
svn diff $SRC_HOME/dyncall/dyncall | sed "s/${HOME//\//\\/}\/src\/dyncall\///" > dyncall.diff
#svn diff $SRC_HOME/dyncall/dyncall | sed "s/${HOME//\//\\/}\/src\/dyncall\///" | sed -E 's/^(---|\+\+\+)(.*)\(([^)]+)\)/\1\2/' > dyncall.diff

echo "# Making dyncall"
cd "$DYNCALL_HOME" || ( echo "Please set DYNCALL_HOME" && exit 1 )

TARGET=${TARGET:-default}
ANDROID_NDK_HOME=${ANDROID_NDK_HOME:-$BIN_HOME/android-ndk-r5c}
case $TARGET in
	android)
		NEEDS_TEST=0
		SHAREDLIB_SUFFIX=so
		
		ANDROID_PREBUILT_DIR=$ANDROID_NDK_HOME/toolchains/arm-linux-androideabi-4.4.3/prebuilt
		
		sh ./configure --with-androidndk=$ANDROID_PREBUILT_DIR/`ls $ANDROID_PREBUILT_DIR | grep -`/bin/arm-linux-androideabi- --target-arm-arm --with-sysroot=$ANDROID_NDK_HOME/platforms/android-9/arch-arm
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
		if [[ "$ARCH_NAME" == "android_arm32_arm" ]] ; then
			RES_SUB="lib/armeabi" ;
		else
			RES_SUB="org/bridj/lib/$ARCH_NAME" ;
		fi
		MAIN_OUT="../../../resources/$RES_SUB"
	
		echo ARCH_NAME: $ARCH_NAME
		echo RES_SUB: $RES_SUB
		TEST_OUT="../../../../test/resources/$RES_SUB"
	
		mkdir -p $MAIN_OUT
		cp $D/*.$SHAREDLIB_SUFFIX $MAIN_OUT
		
		if [[ "$NEEDS_TEST" == "1" ]] ; then
			mkdir -p $TEST_OUT 
			cp ../../../../test/cpp/test/build_out/$D/*.$SHAREDLIB_SUFFIX $TEST_OUT
		
			nm $TEST_OUT/*.so > $TEST_OUT/test.so.nm
			nm $TEST_OUT/*.dylib > $TEST_OUT/test.dylib.nm ;
		fi
		
		echo "Done for $D" ;
	#	svn add $MAIN_OUT
	#	svn add $TEST_OUT ;
	done ;
fi
