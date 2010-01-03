#include "com_nativelibs4java_runtime_Pointer.h"

#include <stdlib.h>
#include "Exceptions.h"

char* GetPeer(JNIEnv *env, jobject pointerInstance) {
	jclass pointerClass = env->FindClass("com/nativelibs4java/runtime/Pointer");
	jfieldID peerField = env->GetFieldID(pointerClass, "peer", "J");
	jlong peer = env->GetLongField(pointerInstance, peerField);
	return (char*)peer;
}

#define POINTER_GETSET(lo, hi) \
lo JNICALL Java_com_nativelibs4java_runtime_Pointer_get ## hi(JNIEnv *env, jobject jthis, jlong offset) { \
	BEGIN_TRY(); \
	char* peer = GetPeer(env, jthis); \
	return *(lo*)(peer + offset); \
	END_TRY(env); \
} \
void JNICALL Java_com_nativelibs4java_runtime_Pointer_set ## hi(JNIEnv *env, jobject jthis, jlong offset, lo value) { \
	BEGIN_TRY(); \
	char* peer = GetPeer(env, jthis); \
	*(lo*)(peer + offset) = value; \
	END_TRY(env); \
}


POINTER_GETSET(jint, Int)
POINTER_GETSET(jlong, Long)
POINTER_GETSET(jshort, Short)
POINTER_GETSET(jbyte, Byte)
//POINTER_GETSET(char, Char)
POINTER_GETSET(jfloat, Float)
POINTER_GETSET(jdouble, Double)
//POINTER_GETSET(ptrdiff_t, Pointer_)
POINTER_GETSET(jlong, PointerAddress)

jobject JNICALL Java_com_nativelibs4java_runtime_Pointer_getByteBuffer(JNIEnv *env, jobject jthis, jlong offset, jlong length) {
	BEGIN_TRY();
	char* peer = GetPeer(env, jthis);
	return env->NewDirectByteBuffer(peer + offset, length);
	END_TRY(env);
}
jlong JNICALL Java_com_nativelibs4java_runtime_Pointer_getDirectBufferAddress(JNIEnv *env, jobject jthis, jobject buffer) {
	BEGIN_TRY();
	return (jlong)env->GetDirectBufferAddress(buffer);
	END_TRY(env);
}
jlong JNICALL Java_com_nativelibs4java_runtime_Pointer_getDirectBufferCapacity(JNIEnv *env, jobject jthis, jobject buffer) {
	BEGIN_TRY();
	return env->GetDirectBufferCapacity(buffer);
	END_TRY(env);
}


jlong JNICALL Java_com_nativelibs4java_runtime_Pointer_doAllocate(JNIEnv *env, jclass, jint size)
{
	BEGIN_TRY();
	return (jlong)malloc(size);
	END_TRY(env);
}

void JNICALL Java_com_nativelibs4java_runtime_Pointer_doFree(JNIEnv *env, jclass, jlong pointer)
{
	BEGIN_TRY();
	free((void*)pointer);
	END_TRY(env);
}
