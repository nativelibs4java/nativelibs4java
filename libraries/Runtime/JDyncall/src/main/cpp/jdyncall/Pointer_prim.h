#ifndef CONCAT_2
	#define CONCAT_2_(a, b) a##b
	#define CONCAT_2(a, b) CONCAT_2_(a, b)
	#define CONCAT_3_(a, b, c) a##b##c
	#define CONCAT_3(a, b, c) CONCAT_3_(a, b, c)
#endif

jprimName JNICALL CONCAT_2(Java_com_bridj_Pointer_get_1, primName)(JNIEnv *env, jobject, jlong peer, jbyte endianness) {
	BEGIN_TRY();
	//TODO handle endianness
	return *(jprimName*)((char*)peer);
	END_TRY_RET(env, (jprimName)0);
}
void JNICALL CONCAT_2(Java_com_bridj_Pointer_set_1, primName)(JNIEnv *env, jobject, jlong peer, primName value, jbyte endianness) {
	BEGIN_TRY();
	//TODO handle endianness
	*(jprimName*)((char*)peer) = value;
	END_TRY(env);
}
jprimArray JNICALL CONCAT_3(Java_com_bridj_Pointer_get_1, primName, _1array)(JNIEnv *env, jobject, jlong peer, jint length, jbyte endianness) {
	BEGIN_TRY();
	//TODO handle endianness
	jprimArray array = env->CONCAT_3(New, primJNICapName, Array)(length);
	env->CONCAT_3(Set, primJNICapName, ArrayRegion)(array, 0, (jsize)length, (jprimName*)((char*)peer));
	return array;
	END_TRY_RET(env, NULL);
}
void JNICALL CONCAT_3(Java_com_bridj_Pointer_set_1, primName, _1array)(JNIEnv *env, jobject, jlong peer, jprimArray values, jlong valuesOffset, jlong length, jbyte endianness) {
	BEGIN_TRY();
	//TODO handle endianness
	env->CONCAT_3(Get, primJNICapName, ArrayRegion)(values, (jsize)valuesOffset, (jsize)length, (jprimName*)((char*)peer));
	END_TRY(env);
}
