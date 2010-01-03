#include "com_nativelibs4java_runtime_Pointer.h"

#include <string.h>
#include <wchar.h>
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
	return !buffer ? 0 : (jlong)env->GetDirectBufferAddress(buffer);
	END_TRY(env);
}
jlong JNICALL Java_com_nativelibs4java_runtime_Pointer_getDirectBufferCapacity(JNIEnv *env, jobject jthis, jobject buffer) {
	BEGIN_TRY();
	return !buffer ? 0 : env->GetDirectBufferCapacity(buffer);
	END_TRY(env);
}

#define FUNC_VOID_3(name, t1, t2, t3, nt1, nt2, nt3) \
void JNICALL Java_com_nativelibs4java_runtime_Pointer_ ## name(JNIEnv *env, jclass, t1 a1, t2 a2, t3 a3) \
{ \
	BEGIN_TRY(); \
	name((nt1)a1, (nt2)a2, (nt3)a3); \
	END_TRY(env); \
}

#define FUNC_3(ret, name, t1, t2, t3, nt1, nt2, nt3) \
ret JNICALL Java_com_nativelibs4java_runtime_Pointer_ ## name(JNIEnv *env, jclass, t1 a1, t2 a2, t3 a3) \
{ \
	BEGIN_TRY(); \
	return (ret)name((nt1)a1, (nt2)a2, (nt3)a3); \
	END_TRY(env); \
}

#define FUNC_VOID_1(name, t1, nt1) \
void JNICALL Java_com_nativelibs4java_runtime_Pointer_ ## name(JNIEnv *env, jclass, t1 a1) \
{ \
	BEGIN_TRY(); \
	name((nt1)a1); \
	END_TRY(env); \
}

#define FUNC_1(ret, name, t1, nt1) \
ret JNICALL Java_com_nativelibs4java_runtime_Pointer_ ## name(JNIEnv *env, jclass, t1 a1) \
{ \
	BEGIN_TRY(); \
	return (ret)name((nt1)a1); \
	END_TRY(env); \
}

FUNC_1(jlong, malloc, jlong, size_t)
FUNC_VOID_1(free, jlong, void*)

FUNC_1(jlong, strlen, jlong, char*)
FUNC_1(jlong, wcslen, jlong, wchar_t*)

FUNC_VOID_3(memcpy, jlong, jlong, jlong, void*, void*, size_t)
FUNC_VOID_3(memmove, jlong, jlong, jlong, void*, void*, size_t)

FUNC_VOID_3(wmemcpy, jlong, jlong, jlong, wchar_t*, wchar_t*, size_t)
FUNC_VOID_3(wmemmove, jlong, jlong, jlong, wchar_t*, wchar_t*, size_t)

FUNC_3(jlong, memchr, jlong, jbyte, jlong, void*, unsigned char, size_t)
FUNC_3(jint, memcmp, jlong, jlong, jlong, void*, void*, size_t)
FUNC_VOID_3(memset, jlong, jbyte, jlong, void*, unsigned char, size_t)

