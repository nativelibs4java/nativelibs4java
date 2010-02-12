#include "com_bridj_JNI.h"

#include "dyncallback/dyncall_callback.h"
#include "dynload/dynload.h"
#include "RawNativeForwardCallback.h"

#include "bridj.hpp"
#include <string.h>
#include <stdlib.h>
#include "Exceptions.h"

#if defined(DC_UNIX)
#include <dlfcn.h>
#endif

#define JNI_SIZEOF(type, escType) \
jint JNICALL Java_com_bridj_JNI_sizeOf_1 ## escType(JNIEnv *env, jclass clazz) { return sizeof(type); }

#define JNI_SIZEOF_t(type) JNI_SIZEOF(type ## _t, type ## _1t)

JNI_SIZEOF_t(size)
JNI_SIZEOF_t(wchar)
JNI_SIZEOF_t(ptrdiff)

void JNICALL Java_com_bridj_JNI_init(JNIEnv *env, jclass clazz)
{
	//DefineCommonClassesAndMethods(env);
}

jlong JNICALL Java_com_bridj_JNI_getDirectBufferAddress(JNIEnv *env, jobject jthis, jobject buffer) {
	BEGIN_TRY();
	return !buffer ? 0 : (jlong)(*env)->GetDirectBufferAddress(env, buffer);
	END_TRY_RET(env, 0);
}
jlong JNICALL Java_com_bridj_JNI_getDirectBufferCapacity(JNIEnv *env, jobject jthis, jobject buffer) {
	BEGIN_TRY();
	return !buffer ? 0 : (*env)->GetDirectBufferCapacity(env, buffer);
	END_TRY_RET(env, 0);
}

jlong JNICALL Java_com_bridj_JNI_getObjectPointer(JNIEnv *env, jclass clazz, jobject object)
{
	return (jlong)object;
}
 
jlong JNICALL Java_com_bridj_JNI_loadLibrary(JNIEnv *env, jclass clazz, jstring pathStr)
{
	const char* path = (*env)->GetStringUTFChars(env, pathStr, NULL);
	jlong ret = (jlong)(size_t)dlLoadLibrary(path);
	(*env)->ReleaseStringUTFChars(env, pathStr, path);
	return ret;
}

void JNICALL Java_com_bridj_JNI_freeLibrary(JNIEnv *env, jclass clazz, jlong libHandle)
{
	dlFreeLibrary((DLLib*)(size_t)libHandle);
}

jarray JNICALL Java_com_bridj_JNI_getLibrarySymbols(JNIEnv *env, jclass clazz, jlong libHandle)
{
	jclass stringClass;
    jarray ret;
    DLSyms* pSyms = (DLSyms*)malloc(dlSyms_sizeof());
	int count, i;
	dlSymsInit((DLLib*)libHandle, pSyms);
	count = dlSymsCount(pSyms);
	
	stringClass = (*env)->FindClass(env, "java/lang/String");
	ret = (*env)->NewObjectArray(env, count, stringClass, 0);
    for (i = 0; i < count; i++) {
		jstring str;
		const char* name = dlSymsName(pSyms, i);
		if (!name)
			continue;
		(*env)->SetObjectArrayElement(env, ret, i, (*env)->NewStringUTF(env, name));
    }
	dlSymsCleanup(pSyms);
	free(pSyms);
    return ret;
}

jstring JNICALL Java_com_bridj_JNI_findSymbolName(JNIEnv *env, jclass clazz, jlong libHandle, jlong address)
{
#if defined(DC_UNIX)
	Dl_info info;
	if (!dladdr((void*)(size_t)address, &info))
		return NULL;
	if (!info.dli_sname || ((jlong)info.dli_saddr) != address)
		return NULL;
	
	return (*env)->NewStringUTF(env, info.dli_sname);
#else
	return NULL;
#endif
}

jlong JNICALL Java_com_bridj_JNI_findSymbolInLibrary(JNIEnv *env, jclass clazz, jlong libHandle, jstring nameStr)
{
	const char* name = (*env)->GetStringUTFChars(env, nameStr, NULL);
	jlong ret = (jlong)dlFindSymbol((DLLib*)(size_t)libHandle, name);
#if 0
#if defined(DC_UNIX)
	if (!ret) {
		const char* error = dlerror();
		printf(error);
		printf("\n");
	}
#endif
#endif
	(*env)->ReleaseStringUTFChars(env, nameStr, name);
	return ret;
}

jobject JNICALL Java_com_bridj_JNI_newDirectByteBuffer(JNIEnv *env, jobject jthis, jlong peer, jlong length) {
	BEGIN_TRY();
	return (*env)->NewDirectByteBuffer(env, (void*)peer, length);
	END_TRY_RET(env, NULL);
}

JNIEXPORT jint JNICALL Java_com_bridj_JNI_getMaxDirectMappingArgCount(JNIEnv *env, jclass clazz) {
#if defined(_WIN64)
	return 4;
#elif defined(DC__OS_Darwin) && defined(DC__Arch_AMD64)
	return 4;
#elif defined(_WIN32)
	return 65000;
#else
	return -1;
#endif
}

JNIEXPORT jlong JNICALL Java_com_bridj_JNI_createCallback(
	JNIEnv *env, 
	jclass clazz,
	jclass declaringClass,
	jstring methodName,
	jint callMode,
	jlong forwardedPointer, 
	jint virtualTableOffset,
	jint virtualIndex,
	jboolean direct, 
	jstring javaSignature, 
	jstring dcSignature,
	jint nParams,
	jint returnValueType, 
	jintArray paramsValueTypes
) {
	JNINativeMethod meth;
	struct MethodCallInfo *info = NULL;
	//if (!forwardedPointer && virtualIndex < 0)
	//	return 0;
	
	info = (struct MethodCallInfo*)malloc(sizeof(struct MethodCallInfo));
	memset(info, 0, sizeof(MethodCallInfo));
	
	info->fForwardedSymbol = (void*)(size_t)forwardedPointer;
	info->fEnv = env;
	info->fDCMode = callMode;
	info->fReturnType = (ValueType)returnValueType;
	info->nParams = nParams;
	info->fVirtualIndex = virtualIndex;
	info->fVirtualTableOffset = virtualTableOffset;
	if (nParams) {
		info->fParamTypes = (ValueType*)malloc(nParams * sizeof(jint));	
		(*env)->GetIntArrayRegion(env, paramsValueTypes, 0, nParams, (jint*)info->fParamTypes);
	}
	
	meth.fnPtr = NULL;
	if (direct && forwardedPointer) // TODO DIRECT C++ virtual thunk
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
	
	return (jlong)(size_t)info;
}

JNIEXPORT void JNICALL Java_com_bridj_JNI_freeCallback(JNIEnv *env, jclass clazz, jlong nativeCallback)
{
	MethodCallInfo* info = (MethodCallInfo*)nativeCallback;
	if (info->nParams)
		free(info->fParamTypes);
	
	dcbFreeCallback((DCCallback*)info->fCallback);
	free(info);
}


#define FUNC_VOID_3(name, t1, t2, t3, nt1, nt2, nt3) \
void JNICALL Java_com_bridj_JNI_ ## name(JNIEnv *env, jclass clazz, t1 a1, t2 a2, t3 a3) \
{ \
	BEGIN_TRY(); \
	name((nt1)a1, (nt2)a2, (nt3)a3); \
	END_TRY(env); \
}

#define FUNC_3(ret, name, t1, t2, t3, nt1, nt2, nt3) \
ret JNICALL Java_com_bridj_JNI_ ## name(JNIEnv *env, jclass clazz, t1 a1, t2 a2, t3 a3) \
{ \
	BEGIN_TRY(); \
	return (ret)name((nt1)a1, (nt2)a2, (nt3)a3); \
	END_TRY_RET(env, (ret)0); \
}

#define FUNC_VOID_1(name, t1, nt1) \
void JNICALL Java_com_bridj_JNI_ ## name(JNIEnv *env, jclass clazz, t1 a1) \
{ \
	BEGIN_TRY(); \
	name((nt1)a1); \
	END_TRY(env); \
}

#define FUNC_1(ret, name, t1, nt1) \
ret JNICALL Java_com_bridj_JNI_ ## name(JNIEnv *env, jclass clazz, t1 a1) \
{ \
	BEGIN_TRY(); \
	return (ret)name((nt1)a1); \
	END_TRY_RET(env, (ret)0); \
}

FUNC_1(jlong, malloc, jlong, size_t)
FUNC_VOID_1(free, jlong, void*)

FUNC_1(jlong, strlen, jlong, char*)
FUNC_1(jlong, wcslen, jlong, wchar_t*)

FUNC_VOID_3(memcpy, jlong, jlong, jlong, void*, void*, size_t)
FUNC_VOID_3(memmove, jlong, jlong, jlong, void*, void*, size_t)

//FUNC_VOID_3(wmemcpy, jlong, jlong, jlong, wchar_t*, wchar_t*, size_t)
//FUNC_VOID_3(wmemmove, jlong, jlong, jlong, wchar_t*, wchar_t*, size_t)

FUNC_3(jlong, memchr, jlong, jbyte, jlong, void*, unsigned char, size_t)
FUNC_3(jint, memcmp, jlong, jlong, jlong, void*, void*, size_t)
FUNC_VOID_3(memset, jlong, jbyte, jlong, void*, unsigned char, size_t)

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
