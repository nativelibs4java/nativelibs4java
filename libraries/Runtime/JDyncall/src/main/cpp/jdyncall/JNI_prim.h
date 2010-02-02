#ifndef CONCAT_2
	#define CONCAT_2_(a, b) a##b
	#define CONCAT_2(a, b) CONCAT_2_(a, b)
	#define CONCAT_3_(a, b, c) a##b##c
	#define CONCAT_3(a, b, c) CONCAT_3_(a, b, c)
#endif

jprimName JNICALL CONCAT_2(Java_com_nativelibs4java_runtime_JNI_get_1, primName)(JNIEnv *env, jobject, jlong peer, jbyte endianness) {
	BEGIN_TRY();
	//TODO handle endianness
	return *(jprimName*)((char*)peer);
	END_TRY(env);
}
void JNICALL CONCAT_2(Java_com_nativelibs4java_runtime_JNI_set_1, primName)(JNIEnv *env, jobject, jlong peer, jprimName value, jbyte endianness) {
	BEGIN_TRY();
	//TODO handle endianness
	*(jprimName*)((char*)peer) = value;
	END_TRY(env);
}
jprimArray JNICALL CONCAT_3(Java_com_nativelibs4java_runtime_JNI_get_1, primName, _1array)(JNIEnv *env, jobject, jlong peer, jint length, jbyte endianness) {
	BEGIN_TRY();
	//TODO handle endianness
	jprimArray array = env->CONCAT_3(New, primJNICapName, Array)(length);
	env->CONCAT_3(Set, primJNICapName, ArrayRegion)(array, 0, (jsize)length, (jprimName*)((char*)peer));
	return array;
	END_TRY(env);
}
void JNICALL CONCAT_3(Java_com_nativelibs4java_runtime_JNI_set_1, primName, _1array)(JNIEnv *env, jobject, jlong peer, jprimArray values, jlong valuesOffset, jlong length, jbyte endianness) {
	BEGIN_TRY();
	//TODO handle endianness
	env->CONCAT_3(Get, primJNICapName, ArrayRegion)(values, (jsize)valuesOffset, (jsize)length, (jprimName*)((char*)peer));
	END_TRY(env);
}

JNIEXPORT jlong JNICALL CONCAT_3(Java_com_nativelibs4java_runtime_JNI_get, primJNICapName, ArrayElements)(JNIEnv *env, jclass, jprimArray array, jbooleanArray aIsCopy)
{
	jboolean tr = true;
	jboolean *pIsCopy = aIsCopy ? env->GetBooleanArrayElements(aIsCopy, &tr) : NULL;
	jlong ret = (jlong)env->CONCAT_3(Get, primJNICapName, ArrayElements)(array, pIsCopy);
	if (aIsCopy)
		env->ReleaseBooleanArrayElements(aIsCopy, pIsCopy, JNI_COMMIT);
	return ret;
}

JNIEXPORT void JNICALL CONCAT_3(Java_com_nativelibs4java_runtime_JNI_get, primJNICapName, ArrayElements)(JNIEnv *env, jclass, jprimArray array, jlong pointer, jint mode)
{
	env->CONCAT_3(Release, primJNICapName, ArrayElements)(array, (jprimName*)(size_t)pointer, mode);
}

