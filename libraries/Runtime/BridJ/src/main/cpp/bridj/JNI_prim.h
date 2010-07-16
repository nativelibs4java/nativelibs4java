#ifndef CONCAT_2
	#define CONCAT_2_(a, b) a##b
	#define CONCAT_2(a, b) CONCAT_2_(a, b)
	#define CONCAT_3_(a, b, c) a##b##c
	#define CONCAT_3(a, b, c) CONCAT_3_(a, b, c)
#endif

#ifndef SUPPORTS_UNALIGNED_ACCESS
static jprimName CONCAT_2(unaligned_get_1, primName)(JNIEnv* env, jclass clazz, jlong peer, jprimName (JNICALL *getter)(JNIEnv*, jclass, jlong)) {
#if 1
	throwException(env, "Unaligned pointer access !");
	return (jprimName)0;
#else
	int i;
	union { char bytes[primSize]; jprimName prim; } aligned;
	char* ptr = (char*)JLONG_TO_PTR(peer);
	for (i = 0; i < primSize; i++)
		aligned.bytes[i] = *(ptr++);
	
	return getter(env, clazz, PTR_TO_JLONG(&aligned.bytes));
#endif
}
#endif // ifndef SUPPORTS_UNALIGNED_ACCESS

#ifndef SUPPORTS_UNALIGNED_ACCESS
static void CONCAT_2(unaligned_set_1, primName)(JNIEnv* env, jclass clazz, jlong peer, jprimName value, void (JNICALL *setter)(JNIEnv*, jclass, jlong, jprimName)) {
	int i;
	char* ptr;
	union { char bytes[primSize]; jprimName prim; } aligned;
	setter(env, clazz, PTR_TO_JLONG(&aligned.bytes), value);
	
	ptr = (char*)JLONG_TO_PTR(peer);
	for (i = 0; i < primSize; i++)
		*(ptr++) = aligned.bytes[i];
}
#endif // ifndef SUPPORTS_UNALIGNED_ACCESS

#ifdef REORDER_VALUE_BYTES
jprimName JNICALL CONCAT_3(Java_com_bridj_JNI_get_1, primName, _1disordered)(JNIEnv* env, jclass clazz, jlong peer) {
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

jprimName JNICALL CONCAT_2(Java_com_bridj_JNI_get_1, primName)(JNIEnv *env, jclass clazz, jlong peer) {
	BEGIN_TRY();
#ifndef SUPPORTS_UNALIGNED_ACCESS
#ifdef alignmentMask
	if (peer & alignmentMask)
		return CONCAT_2(unaligned_get_1, primName)(env, clazz, peer, CONCAT_2(Java_com_bridj_JNI_get_1, primName));
#endif
#endif
	return *(jprimName*)((char*)JLONG_TO_PTR(peer));
	END_TRY_RET(env, (jprimName)0);
}
void JNICALL CONCAT_2(Java_com_bridj_JNI_set_1, primName)(JNIEnv *env, jclass clazz, jlong peer, jprimName value) {
#ifndef SUPPORTS_UNALIGNED_ACCESS
#ifdef alignmentMask
	if (peer & alignmentMask) {
		CONCAT_2(unaligned_set_1, primName)(env, clazz, peer, value, CONCAT_2(Java_com_bridj_JNI_set_1, primName));
		return;
	}
#endif
#endif
    BEGIN_TRY();
	*(jprimName*)((char*)JLONG_TO_PTR(peer)) = value;
	END_TRY(env);
}

#ifdef REORDER_VALUE_BYTES
void JNICALL CONCAT_3(Java_com_bridj_JNI_set_1, primName, _1disordered)(JNIEnv* env, jclass clazz, jlong peer, jprimName value) {
#ifndef SUPPORTS_UNALIGNED_ACCESS
#ifdef alignmentMask
	if (peer & alignmentMask) {
		CONCAT_2(unaligned_set_1, primName)(env, clazz, peer, value, CONCAT_3(Java_com_bridj_JNI_set_1, primName, _1disordered));
		return;
	}
#endif
#endif
    BEGIN_TRY();
	*(jprimName*)((char*)JLONG_TO_PTR(peer)) = REORDER_VALUE_BYTES(&value);
	END_TRY(env);
}
#endif // ifdef REORDER_VALUE_BYTES

jprimArray JNICALL CONCAT_3(Java_com_bridj_JNI_get_1, primName, _1array)(JNIEnv *env, jclass clazz, jlong peer, jint length) {
	jprimArray array;
	BEGIN_TRY();
	array = (jprimArray)JLONG_TO_PTR((*env)->CONCAT_3(New, primJNICapName, Array)(env, length));
	(*env)->CONCAT_3(Set, primJNICapName, ArrayRegion)(env, array, 0, (jsize)length, (jprimName*)((char*)JLONG_TO_PTR(peer)));
	return array;
	END_TRY_RET(env, NULL);
}
void JNICALL CONCAT_3(Java_com_bridj_JNI_set_1, primName, _1array)(JNIEnv *env, jclass clazz, jlong peer, jprimArray values, jint valuesOffset, jint length) {
	BEGIN_TRY();
	(*env)->CONCAT_3(Get, primJNICapName, ArrayRegion)(env, values, (jsize)valuesOffset, (jsize)length, (jprimName*)((char*)JLONG_TO_PTR(peer)));
	END_TRY(env);
}

#ifdef REORDER_VALUE_BYTES
jprimArray JNICALL CONCAT_3(Java_com_bridj_JNI_get_1, primName, _1array_1disordered)(JNIEnv *env, jclass clazz, jlong peer, jint length) {
	jint i;
	jprimArray array;
	jprimName* nativeArray;
	jprimName tempVal;
	BEGIN_TRY();
	array = (jprimArray)JLONG_TO_PTR((*env)->CONCAT_3(New, primJNICapName, Array)(env, length));
	nativeArray = (jprimName*)((char*)JLONG_TO_PTR(peer));
	for (i = 0; i < length; i++) {
		tempVal = REORDER_VALUE_BYTES(&nativeArray[i]);
		(*env)->CONCAT_3(Set, primJNICapName, ArrayRegion)(env, array, i, 1, &tempVal);
	}
	return array;
	END_TRY_RET(env, NULL);
}
void JNICALL CONCAT_3(Java_com_bridj_JNI_set_1, primName, _1array_1disordered)(JNIEnv *env, jclass clazz, jlong peer, jprimArray values, jint valuesOffset, jint length) {
	jint i;
	jprimName* nativeArray;
	jprimName tempVal;
	BEGIN_TRY();
	nativeArray = (jprimName*)((char*)JLONG_TO_PTR(peer));
	for (i = 0; i < length; i++) {
		(*env)->CONCAT_3(Get, primJNICapName, ArrayRegion)(env, values, valuesOffset + i, 1, &tempVal);
		nativeArray[i] = REORDER_VALUE_BYTES(&tempVal);
	}
	END_TRY(env);
}
#endif // ifdef REORDER_VALUE_BYTES

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
	(*env)->CONCAT_3(Release, primJNICapName, ArrayElements)(env, array, (jprimName*)JLONG_TO_PTR(pointer), mode);
}

