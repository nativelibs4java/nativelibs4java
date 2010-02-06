#include "com_nativelibs4java_runtime_JNI.h"

#include "dyncallback/dyncall_callback.h"
#include "dynload/dynload.h"
#include "RawNativeForwardCallback.h"

#include "jdyncall.hpp"
#include <string.h>
#include <stdlib.h>
#include "Exceptions.h"

#define JNI_SIZEOF(type, escType) \
jint JNICALL Java_com_nativelibs4java_runtime_JNI_sizeOf_1 ## escType(JNIEnv *env, jclass clazz) { return sizeof(type); }

#define JNI_SIZEOF_t(type) JNI_SIZEOF(type ## _t, type ## _1t)

JNI_SIZEOF_t(size)
JNI_SIZEOF_t(wchar)
JNI_SIZEOF_t(ptrdiff)

void JNICALL Java_com_nativelibs4java_runtime_JNI_init(JNIEnv *env, jclass clazz)
{
	//DefineCommonClassesAndMethods(env);
}

jlong JNICALL Java_com_nativelibs4java_runtime_JNI_getDirectBufferAddress(JNIEnv *env, jobject jthis, jobject buffer) {
	BEGIN_TRY();
	return !buffer ? 0 : (jlong)(*env)->GetDirectBufferAddress(env, buffer);
	END_TRY(env);
}
jlong JNICALL Java_com_nativelibs4java_runtime_JNI_getDirectBufferCapacity(JNIEnv *env, jobject jthis, jobject buffer) {
	BEGIN_TRY();
	return !buffer ? 0 : (*env)->GetDirectBufferCapacity(env, buffer);
	END_TRY(env);
}

jlong JNICALL Java_com_nativelibs4java_runtime_JNI_getObjectPointer(JNIEnv *env, jclass clazz, jobject object)
{
	return (jlong)object;
}
 
jlong JNICALL Java_com_nativelibs4java_runtime_JNI_loadLibrary(JNIEnv *env, jclass clazz, jstring pathStr)
{
	const char* path = (*env)->GetStringUTFChars(env, pathStr, NULL);
	jlong ret = (jlong)dlLoadLibrary(path);
	(*env)->ReleaseStringUTFChars(env, pathStr, path);
	return ret;
}

void JNICALL Java_com_nativelibs4java_runtime_JNI_freeLibrary(JNIEnv *env, jclass clazz, jlong libHandle)
{
	dlFreeLibrary((DLLib*)libHandle);
}

jlong JNICALL Java_com_nativelibs4java_runtime_JNI_findSymbolInLibrary(JNIEnv *env, jclass clazz, jlong libHandle, jstring nameStr)
{
	const char* name = (*env)->GetStringUTFChars(env, nameStr, NULL);
	jlong ret = (jlong)dlFindSymbol((DLLib*)libHandle, name);
	(*env)->ReleaseStringUTFChars(env, nameStr, name);
	return ret;
}

jobject JNICALL Java_com_nativelibs4java_runtime_JNI_newDirectByteBuffer(JNIEnv *env, jobject jthis, jlong peer, jlong length) {
	BEGIN_TRY();
	return (*env)->NewDirectByteBuffer(env, (void*)peer, length);
	END_TRY(env);
}

JNIEXPORT jint JNICALL Java_com_nativelibs4java_runtime_JNI_getMaxDirectMappingArgCount(JNIEnv *env, jclass clazz) {
#ifdef _WIN64
	return 4;
#else	
#ifdef _WIN32
	return 65000;
#else
	return -1;
#endif
#endif
}

JNIEXPORT jlong JNICALL Java_com_nativelibs4java_runtime_JNI_createCallback(
	JNIEnv *env, 
	jclass clazz,
	jclass declaringClass,
	jstring methodName,
	jint callMode,
	jlong forwardedPointer, 
	jboolean direct, 
	jstring javaSignature, 
	jstring dcSignature,
	jint nParams,
	jint returnValueType, 
	jintArray paramsValueTypes
) {
	JNINativeMethod meth;
	struct MethodCallInfo *info = NULL;
	if (!forwardedPointer)
		return NULL;
	
	info = (struct MethodCallInfo*)malloc(sizeof(struct MethodCallInfo));
	memset(info, 0, sizeof(MethodCallInfo));
	
	info->fForwardedSymbol = (void*)(size_t)forwardedPointer;
	info->fEnv = env;
	info->fDCMode = callMode;
	info->fReturnType = (ValueType)returnValueType;
	info->nParams = nParams;
	if (nParams) {
		info->fParamTypes = (ValueType*)malloc(nParams * sizeof(jint));	
		(*env)->GetIntArrayRegion(env, paramsValueTypes, 0, nParams, (jint*)info->fParamTypes);
	}
	
	meth.fnPtr = NULL;
	if (direct)
		info->fCallback = (DCCallback*)dcRawCallAdapterSkipTwoArgs((void (*)())forwardedPointer);
	if (!info->fCallback) {
		const char* ds = (*env)->GetStringUTFChars(env, dcSignature, NULL);
		info->fCallback = dcbNewCallback(ds, JavaToNativeCallHandler, info);
		(*env)->ReleaseStringUTFChars(env, dcSignature, ds);
	}
	meth.fnPtr = info->fCallback;
	meth.name = (char*)(*env)->GetStringUTFChars(env, methodName, NULL);
	meth.signature = (char*)(*env)->GetStringUTFChars(env, javaSignature, NULL);
	(*env)->RegisterNatives(env, declaringClass, &meth, 1);
	
	(*env)->ReleaseStringUTFChars(env, methodName, meth.name);
	(*env)->ReleaseStringUTFChars(env, javaSignature, meth.signature);
	
	return (jlong)info;
}

JNIEXPORT void JNICALL Java_com_nativelibs4java_runtime_JNI_freeCallback(JNIEnv *env, jclass clazz, jlong nativeCallback)
{
	MethodCallInfo* info = (MethodCallInfo*)nativeCallback;
	if (info->nParams)
		free(info->fParamTypes);
	
	dcbFreeCallback((DCCallback*)info->fCallback);
	free(info);
}


#include "PrimDefs_int.h"
#include "JNI_prim.h"

#include "PrimDefs_long.h"
#include "JNI_prim.h"

#include "PrimDefs_short.h"
#include "JNI_prim.h"

#include "PrimDefs_byte.h"
#include "JNI_prim.h"

#include "PrimDefs_char.h"
#include "JNI_prim.h"

#include "PrimDefs_boolean.h"
#include "JNI_prim.h"

#include "PrimDefs_float.h"
#include "JNI_prim.h"

#include "PrimDefs_double.h"
#include "JNI_prim.h"
