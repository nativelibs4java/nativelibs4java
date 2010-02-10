#ifndef CONCAT_2
	#define CONCAT_2_(a, b) a##b
	#define CONCAT_2(a, b) CONCAT_2_(a, b)
	#define CONCAT_3_(a, b, c) a##b##c
	#define CONCAT_3(a, b, c) CONCAT_3_(a, b, c)
#endif

#ifndef SUPPORTS_UNALIGNED_ACCESS
jprimName CONCAT_2(unaligned_get_1, primName)(JNIEnv* env, jclass clazz, jlong peer, jprimName (JNICALL *getter)(JNIEnv*, jclass, jlong)) {
	int i;
	union { char bytes[primSize]; jprimName prim; } aligned;
	char* ptr = (char*)peer;
	for (i = 0; i < primSize; i++)
		aligned.bytes[i] = *(ptr++);
	
	return getter(env, clazz, (jlong)&aligned.bytes);
}
#endif // ifndef SUPPORTS_UNALIGNED_ACCESS

#ifdef REORDER_VALUE_BYTES
jprimName JNICALL CONCAT_3(Java_com_bridj_JNI_get_1, primName, _1disordered)(JNIEnv* env, jclass clazz, jlong peer) {
	short* hptr;
#ifndef SUPPORTS_UNALIGNED_ACCESS
#ifdef alignmentMask
	if (peer & alignmentMask)
		return CONCAT_2(unaligned_get_1, primName)(env, clazz, peer, CONCAT_3(Java_com_bridj_JNI_get_1, primName, _1disordered));
#endif
#endif
    //return ((((jprimName)((jprimName*)peer)[0]) << 16) | ((jprimName*)peer)[1]);
	return REORDER_VALUE_BYTES(peer);
}
#endif // ifdef REORDER_VALUE_BYTES

jprimName JNICALL CONCAT_2(Java_com_bridj_JNI_get_1, primName)(JNIEnv *env, jclass clazz, jlong peer, jbyte endianness) {
	BEGIN_TRY();
#ifndef SUPPORTS_UNALIGNED_ACCESS
#ifdef alignmentMask
	if (peer & alignmentMask)
		return CONCAT_2(unaligned_get_1, primName)(env, clazz, peer, CONCAT_2(Java_com_bridj_JNI_get_1, primName));
#endif
#endif
	return *(jprimName*)((char*)(size_t)peer);
	END_TRY_RET(env, (jprimName)0);
}

void JNICALL CONCAT_2(Java_com_bridj_JNI_set_1, primName)(JNIEnv *env, jclass clazz, jlong peer, jprimName value, jbyte endianness) {
	BEGIN_TRY();
	//TODO handle endianness
	*(jprimName*)((char*)(size_t)peer) = value;
	END_TRY(env);
}
jprimArray JNICALL CONCAT_3(Java_com_bridj_JNI_get_1, primName, _1array)(JNIEnv *env, jclass clazz, jlong peer, jint length, jbyte endianness) {
	jprimArray array;
	BEGIN_TRY();
	//TODO handle endianness
	array = (jprimArray)(void*)(size_t)(*env)->CONCAT_3(New, primJNICapName, Array)(env, length);
	(*env)->CONCAT_3(Set, primJNICapName, ArrayRegion)(env, array, 0, (jsize)length, (jprimName*)((char*)(size_t)peer));
	return array;
	END_TRY_RET(env, NULL);
}
void JNICALL CONCAT_3(Java_com_bridj_JNI_set_1, primName, _1array)(JNIEnv *env, jclass clazz, jlong peer, jprimArray values, jint valuesOffset, jint length, jbyte endianness) {
	BEGIN_TRY();
	//TODO handle endianness
	(*env)->CONCAT_3(Get, primJNICapName, ArrayRegion)(env, values, (jsize)valuesOffset, (jsize)length, (jprimName*)((char*)(size_t)peer));
	END_TRY(env);
}

JNIEXPORT jlong JNICALL CONCAT_3(Java_com_bridj_JNI_get, primJNICapName, ArrayElements)(JNIEnv *env, jclass clazz, jprimArray array, jbooleanArray aIsCopy)
{
	jboolean tr = JNI_TRUE;
	jboolean *pIsCopy = aIsCopy ? (*env)->GetBooleanArrayElements(env, aIsCopy, &tr) : NULL;
	jlong ret = (jlong)(*env)->CONCAT_3(Get, primJNICapName, ArrayElements)(env, array, pIsCopy);
	if (aIsCopy)
		(*env)->ReleaseBooleanArrayElements(env, aIsCopy, pIsCopy, JNI_COMMIT);
	return ret;
}

JNIEXPORT void JNICALL CONCAT_3(Java_com_bridj_JNI_release, primJNICapName, ArrayElements)(JNIEnv *env, jclass clazz, jprimArray array, jlong pointer, jint mode)
{
	(*env)->CONCAT_3(Release, primJNICapName, ArrayElements)(env, array, (jprimName*)(size_t)pointer, mode);
}

