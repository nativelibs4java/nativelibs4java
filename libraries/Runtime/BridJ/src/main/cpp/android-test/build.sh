#
# See http://developer.android.com/guide/developing/building/building-cmdline.html
#
echo "You should ensure an emulator is running : android&"

PROJECT_HOME=../../../..
MVN_VERSION="`cat $PROJECT_HOME/pom.xml | grep '<version' | head -n 1 | sed -e 's/.*<version>\(.*\)<\/version>.*/\1/g'`"
BRIDJ_ANDROID_JAR=$PROJECT_HOME/target/bridj-$MVN_VERSION-android.jar
ANDROID_PROJECT_HOME=`pwd`

echo "BridJ version = $MVN_VERSION"

if [[ ! -f "$BRIDJ_ANDROID_JAR" ]] ; then
	echo "$BRIDJ_ANDROID_JAR is missing. Building BridJ first..."
	cd $PROJECT_HOME
	mvn package -DskipTests
	cd $ANDROID_PROJECT_HOME 
	if [[ ! -f "$BRIDJ_ANDROID_JAR" ]] ; then
		echo "Failed to build the Android JAR !" 
		exit 1 ;
	fi ;
fi

ndk-build -C .

if [[ ! -f build.properties.template ]] ; then
	echo "build.properties.template does not exist !"
	exit 1 ;
fi

cp -f $BRIDJ_ANDROID_JAR lib

rm build.properties
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

case $1 in
	install)
		ant install 
		rm build.properties
		;;
	install-device)
		ant install
		rm build.properties
		adb -d install $APK_FILE
		;;
	debug)
		ant debug
		rm build.properties
		;;
	*)
		ant release
		rm build.properties 
		;;
esac

