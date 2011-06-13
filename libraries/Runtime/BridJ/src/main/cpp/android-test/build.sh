ndk-build -C .

if [[ ! -f build.properties.template ]] ; then
	echo "build.properties.template does not exist !"
	exit 1 ;
fi

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

ant release 

rm build.properties

