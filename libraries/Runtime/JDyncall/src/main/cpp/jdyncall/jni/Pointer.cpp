#include "com_nativelibs4java_runtime_Pointer.h"

#include <stdlib.h>

char* GetPeer(JNIEnv *env, jobject pointerInstance) {
	jclass pointerClass = env->FindClass("com/nativelibs4java/runtime/Pointer");
	jfieldID peerField = env->GetFieldID(pointerClass, "peer", "J");
	jlong peer = env->GetLongField(pointerInstance, peerField);
	return (char*)peer;
}

#define POINTER_GETSET(lo, hi) \
lo JNICALL Java_com_nativelibs4java_runtime_Pointer_get ## hi(JNIEnv *env, jobject jthis, jlong offset) { \
	char* peer = GetPeer(env, jthis); \
	return *(lo*)(peer + offset); \
} \
void JNICALL Java_com_nativelibs4java_runtime_Pointer_set ## hi(JNIEnv *env, jobject jthis, jlong offset, lo value) { \
	char* peer = GetPeer(env, jthis); \
	*(lo*)(peer + offset) = value; \
}


POINTER_GETSET(jint, Int)
POINTER_GETSET(jlong, Long)
POINTER_GETSET(jshort, Short)
POINTER_GETSET(jbyte, Byte)
//POINTER_GETSET(char, Char)
POINTER_GETSET(jfloat, Float)
POINTER_GETSET(jdouble, Double)
POINTER_GETSET(ptrdiff_t, Pointer_)

jlong JNICALL Java_com_nativelibs4java_runtime_Pointer_doAllocate(JNIEnv *, jclass, jint size)
{
	return (jlong)malloc(size);
}

void JNICALL Java_com_nativelibs4java_runtime_Pointer_doFree(JNIEnv *, jclass, jlong pointer)
{
	free((void*)pointer);
}
