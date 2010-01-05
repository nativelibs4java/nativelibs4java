#ifndef CONCAT_2
	#define CONCAT_2_(a, b) a##b
	#define CONCAT_2(a, b) CONCAT_2_(a, b)
	#define CONCAT_3_(a, b, c) a##b##c
	#define CONCAT_3(a, b, c) CONCAT_3_(a, b, c)
#endif

jprimName JNICALL CONCAT_2(Java_com_nativelibs4java_runtime_Pointer_get_1, primName)(JNIEnv *env, jobject, jlong peer) {
	BEGIN_TRY();
	return *(jprimName*)((char*)peer);
	END_TRY(env);
}
void JNICALL CONCAT_2(Java_com_nativelibs4java_runtime_Pointer_set_1, primName)(JNIEnv *env, jobject, jlong peer, primName value) {
	BEGIN_TRY();
	*(jprimName*)((char*)peer) = value;
	END_TRY(env);
}
jprimArray JNICALL CONCAT_3(Java_com_nativelibs4java_runtime_Pointer_get_1, primName, _1array)(JNIEnv *env, jobject, jlong peer, jint length) {
	BEGIN_TRY();
	jprimArray array = env->CONCAT_3(New, primJNICapName, Array)(length);
	env->CONCAT_3(Set, primJNICapName, ArrayRegion)(array, 0, (jsize)length, (jprimName*)((char*)peer));
	return array;
	END_TRY(env);
}
void JNICALL CONCAT_3(Java_com_nativelibs4java_runtime_Pointer_set_1, primName, _1array)(JNIEnv *env, jobject, jlong peer, jprimArray values, jlong valuesOffset, jlong length) {
	BEGIN_TRY();
	env->CONCAT_3(Get, primJNICapName, ArrayRegion)(values, (jsize)valuesOffset, (jsize)length, (jprimName*)((char*)peer));
	END_TRY(env);
}
