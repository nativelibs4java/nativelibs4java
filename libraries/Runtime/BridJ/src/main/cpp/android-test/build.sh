#
# See http://developer.android.com/guide/developing/building/building-cmdline.html
#
echo "You should ensure an emulator is running : android&"

PROJECT_HOME=../../../..
MVN_VERSION="`cat $PROJECT_HOME/pom.xml | grep '<version' | head -n 1 | sed -e 's/.*<version>\(.*\)<\/version>.*/\1/g'`"
BRIDJ_ANDROID_JAR=$PROJECT_HOME/target/bridj-$MVN_VERSION-android.jar
ANDROID_PROJECT_HOME=`pwd`

echo "BridJ version = $MVN_VERSION"

function buildBridJ {
	cd $PROJECT_HOME
	mvn package -DskipTests
	cd $ANDROID_PROJECT_HOME
}

if [[ ! -f "$BRIDJ_ANDROID_JAR" ]] ; then
	echo "$BRIDJ_ANDROID_JAR is missing. Building BridJ first..."
	buildBridJ 
	if [[ ! -f "$BRIDJ_ANDROID_JAR" ]] ; then
		echo "Failed to build the Android JAR !" 
		exit 1 ;
	fi ;
fi

if [[ ! -f build.properties.template ]] ; then
	echo "build.properties.template does not exist !"
	exit 1 ;
fi

function helpQuit {
	echo "#
# List of valid build commands (can be combined) :
#
#      package		Build BridJ JAR (will be done implicitly if the JAR doesn't exist)
#      emulator		Install into running emulator
#      device		Install into USB-plugged device
#      release		Build Release Android package
#      debug		Build Debug Android package
#      start			Start on emulator
#
# Default build commands = $DEFAULT_BUILD_CMDS
# Typical debug commands = package emulator start
#
	" 
	exit 1
}

DEFAULT_BUILD_CMDS="package release"
if [[ "$*" == "" ]] ; then 
	BUILD_CMDS=$DEFAULT_BUILD_CMDS ; 
else
	BUILD_CMDS=$* ;
fi
	
cp -f $BRIDJ_ANDROID_JAR lib

cat build.properties.template > build.properties

if [[  -z "$ANDROID_SDK_HOME" ]] ; then
	echo "ANDROID_SDK_HOME is not defined !"
	exit 1 ;
fi
echo "sdk.dir=$ANDROID_SDK_HOME" >> build.properties ;

if [[ ! -z "$KEYSTORE_PASS" ]] ; then
	echo "key.store.password=$KEYSTORE_PASS" >> build.properties ;
fi

APK_FILE=bin/TouchExampleActivity-release.apk

function compile {
	ndk-build -C .
	ant $*
	rm build.properties
}

echo "# Provided build commands = $BUILD_CMDS"

for C in $BUILD_CMDS ; do
	case $C in
		package)
			buildBridJ
			;;
		emulator)
			compile install
			;;
		device)
			compile debug
			adb -d install $APK_FILE
			;;
		debug)
			compile debug
			;;
		release)
			compile release
			;;
		start)
			adb shell am start -a android.intent.action.MAIN -n com.example.hellojni/com.example.hellojni.HelloJni
			;;
		*)
			helpQuit
			;;
	esac ;
done

echo "adb shell am start -a android.intent.action.MAIN -n com.example.hellojni/com.example.hellojni.HelloJni -W"
