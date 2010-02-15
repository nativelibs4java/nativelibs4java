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

#if defined(DC__OS_Win64) || defined(DC__OS_Win32)
#include <Dbghelp.h>
#endif

#pragma warning(disable: 4152)

#define JNI_SIZEOF(type, escType) \
jint JNICALL Java_com_bridj_JNI_sizeOf_1 ## escType(JNIEnv *env, jclass clazz) { return sizeof(type); }

#define JNI_SIZEOF_t(type) JNI_SIZEOF(type ## _t, type ## _1t)

JNI_SIZEOF_t(size)
JNI_SIZEOF_t(wchar)
JNI_SIZEOF_t(ptrdiff)
JNI_SIZEOF(long, long)

jmethodID getPeerMethod = NULL;
jclass bridjClass = NULL;

jmethodID getGetPeerMethod(JNIEnv* env) {
	if (!getPeerMethod)
	{
		bridjClass = (jclass)(*env)->NewGlobalRef(env, (*env)->FindClass(env, "com/bridj/BridJ"));
		//getPeerMethod = (*env)->GetMethodID(env, bridjClass, "getPeer", "(Lcom/bridj/CPPObject;Ljava/lang/Class;)J");
		getPeerMethod = (*env)->GetMethodID(env, bridjClass, "getPeer", "(Lcom/lang/Object;Ljava/lang/Class;)J");
	}
	return getPeerMethod;
}

//void main() {}

void* getCPPInstancePointer(JNIEnv *env, jobject instance, jclass targetClass) {
	return (void*)(size_t)(*env)->CallLongMethod(env, NULL, getGetPeerMethod(env), instance, targetClass);
}
//void _DllMainCRTStartup();

void JNICALL Java_com_bridj_JNI_init(JNIEnv *env, jclass clazz)
{
/*#if defined(DC__OS_Win64) || defined(DC__OS_Win32)
	_DllMainCRTStartup();
#endif*/
	//bridjClass = (*env)->FindClass(env, "com/bridj/BridJ");
	//getPeerMethod = (*env)->GetMethodID(env, bridjClass, "getPeer", "(Lcom/bridj/CPPObject;Ljava/lang/Class;)J");
	//getPeerMethod = (*env)->GetMethodID(env, bridjClass, "getPeer", "(Lcom/lang/Object;Ljava/lang/Class;)J");
}

jlong JNICALL Java_com_bridj_JNI_getDirectBufferAddress(JNIEnv *env, jobject jthis, jobject buffer) {
	BEGIN_TRY();
	return !buffer ? 0 : (jlong)(size_t)(*env)->GetDirectBufferAddress(env, buffer);
	END_TRY_RET(env, 0);
}
jlong JNICALL Java_com_bridj_JNI_getDirectBufferCapacity(JNIEnv *env, jobject jthis, jobject buffer) {
	BEGIN_TRY();
	return !buffer ? 0 : (*env)->GetDirectBufferCapacity(env, buffer);
	END_TRY_RET(env, 0);
}

jlong JNICALL Java_com_bridj_JNI_getObjectPointer(JNIEnv *env, jclass clazz, jobject object)
{
	return (jlong)(size_t)object;
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

jlong JNICALL Java_com_bridj_JNI_loadLibrarySymbols(JNIEnv *env, jclass clazz, jlong libHandle)
{
    DLSyms* pSyms = (DLSyms*)malloc(dlSyms_sizeof());
	int count;
	dlSymsInit(pSyms, (DLLib*)libHandle);
	count = dlSymsCount(pSyms);
	return (jlong)(size_t)pSyms;
}
void JNICALL Java_com_bridj_JNI_freeLibrarySymbols(JNIEnv *env, jclass clazz, jlong symbolsHandle)
{
	DLSyms* pSyms = (DLSyms*)symbolsHandle;
	dlSymsCleanup(pSyms);
	free(pSyms);
}

jarray JNICALL Java_com_bridj_JNI_getLibrarySymbols(JNIEnv *env, jclass clazz, jlong libHandle, jlong symbolsHandle)
{
	jclass stringClass;
    jarray ret;
    DLSyms* pSyms = (DLSyms*)symbolsHandle;
	int count, i;
	count = dlSymsCount(pSyms);
	
	stringClass = (*env)->FindClass(env, "java/lang/String");
	ret = (*env)->NewObjectArray(env, count, stringClass, 0);
    for (i = 0; i < count; i++) {
		const char* name = dlSymsName(pSyms, i);
		if (!name)
			continue;
		(*env)->SetObjectArrayElement(env, ret, i, (*env)->NewStringUTF(env, name));
    }
    return ret;
}


jstring JNICALL Java_com_bridj_JNI_findSymbolName(JNIEnv *env, jclass clazz, jlong libHandle, jlong symbolsHandle, jlong address)
{
	const char* name = dlSymsNameFromValue((DLSyms*)(size_t)symbolsHandle, (void*)(size_t)address);
	return name ? (*env)->NewStringUTF(env, name) : NULL;
	/*
#if defined(DC_UNIX)
	Dl_info info;
	if (!dladdr((void*)(size_t)address, &info))
		return NULL;
	if (!info.dli_sname || ((jlong)(size_t)info.dli_saddr) != address)
		return NULL;
	
	return (*env)->NewStringUTF(env, info.dli_sname);
#elif defined(DC__OS_Win64) || defined(DC__OS_Win32)
    DWORD64  dwAddress = (DWORD64)address;
    DWORD64  dwDisplacement;
    DWORD  error;
    HANDLE hProcess;
    ULONG64 buffer[(
        sizeof(SYMBOL_INFO) +
        MAX_SYM_NAME * sizeof(TCHAR) +
        sizeof(ULONG64) - 1) /
        sizeof(ULONG64)
    ];
    PSYMBOL_INFO pSymbol = (PSYMBOL_INFO) buffer;
        
    SymSetOptions(SYMOPT_UNDNAME | SYMOPT_DEFERRED_LOADS);

    hProcess = (HANDLE)libHandle;//GetCurrentProcess();

    if (!SymInitialize(hProcess, NULL, TRUE))
    {
        // SymInitialize failed
        error = GetLastError();
        printf("SymInitialize returned error : %d\n", error);
        return FALSE;
    }

    pSymbol->SizeOfStruct = sizeof(SYMBOL_INFO);
    pSymbol->MaxNameLen = MAX_SYM_NAME;

    if (SymFromAddr(hProcess, dwAddress, &dwDisplacement, pSymbol) && !dwDisplacement)
        return pSymbol->Name ? (*env)->NewStringUTF(env, pSymbol->Name) : NULL;
    return NULL;
#else
	return NULL;
#endif*/
}

jlong JNICALL Java_com_bridj_JNI_findSymbolInLibrary(JNIEnv *env, jclass clazz, jlong libHandle, jstring nameStr)
{
	const char* name = (*env)->GetStringUTFChars(env, nameStr, NULL);
	jlong ret = (jlong)(size_t)dlFindSymbol((DLLib*)(size_t)libHandle, name);
#if 0
#if defined(DC_UNIX)
	if (!ret) {
		const char* error = dlerror();
		throwException(env, error);
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

/*char __cdecl callInt(JNIEnv *env, jclass clazz, long args, long methodCallInfo) {
{
	DCArgs* args;
	, DCValue* result, MethodCallInfo *info
	JavaToNativeCallHandler(
}*/

char getDCReturnType(ValueType returnType) 
{
	switch (returnType) {
#define CALL_CASE(valueType, capCase, hiCase, uni) \
		case valueType: \
			return DC_SIGCHAR_ ## hiCase;
		CALL_CASE(eIntValue, Int, INT, i)
		CALL_CASE(eLongValue, Long, LONGLONG, l)
		CALL_CASE(eShortValue, Short, SHORT, s)
		CALL_CASE(eFloatValue, Float, FLOAT, f)
		CALL_CASE(eDoubleValue, Double, DOUBLE, d)
		CALL_CASE(eByteValue, Char, CHAR, c)
		case eCLongValue:
			return DC_SIGCHAR_LONG;
		case eSizeTValue:
			return DC_SIGCHAR_LONG;
		case eVoidValue:
			return DC_SIGCHAR_VOID;
		case eWCharValue:
			// TODO
		default:
			//cerr << "Return ValueType not supported yet: " << (int)info->fReturnType << " !\n";
			return DC_SIGCHAR_VOID;
	}
}

void initCommonCallInfo(
	struct CallInfo* info,
	JNIEnv *env,
	jint callMode,
	jint nParams,
	jint returnValueType, 
	jintArray paramsValueTypes
) {
	info->fEnv = env;
	info->fDCMode = callMode;
	info->fReturnType = (ValueType)returnValueType;
	info->nParams = nParams;
	if (nParams) {
		info->fParamTypes = (ValueType*)malloc(nParams * sizeof(jint));	
		(*env)->GetIntArrayRegion(env, paramsValueTypes, 0, nParams, (jint*)info->fParamTypes);
	}
	info->fDCReturnType = getDCReturnType(info->fReturnType);
}

void* getJNICallFunction(JNIEnv* env, ValueType valueType) {
	switch (valueType) {
	case eIntValue:
		return (*env)->CallIntMethod;
	case eSizeTValue:
	case eCLongValue:
	case eLongValue:
		return (*env)->CallLongMethod;
	case eFloatValue:
		return (*env)->CallFloatMethod;
	case eDoubleValue:
		return (*env)->CallDoubleMethod;
	case eByteValue:
		return (*env)->CallByteMethod;
	case eShortValue:
		return (*env)->CallShortMethod;
	case eWCharValue:
		return (*env)->CallCharMethod;
	case eVoidValue:
		return (*env)->CallVoidMethod;
	default:
		throwException(env, "Unhandled type in getJNICallFunction !");
		return NULL;
	}
}

#define NEW_STRUCT(type, name, assignTo, pCommon) \
	struct type *name = NULL; \
	name = (struct type*)malloc(sizeof(struct type)); \
	memset(name, 0, sizeof(struct type)); \
	assignTo = name; \
	pCommon = &name->fInfo;
	
		
JNIEXPORT jlong JNICALL Java_com_bridj_JNI_createCallback(
	JNIEnv *env, 
	jclass clazz,
	jclass declaringClass,
	jobject javaCallbackInstance,
	jobject method,
	jboolean startsWithThis,
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
	
	struct CallInfo *pCommonInfo = NULL;
	void *pInfo = NULL;
	void *callbackToRegister = NULL;
	
	if (javaCallbackInstance)
	{
		const char *dcSig, *javaSig, *methName;
		NEW_STRUCT(JavaCallbackCallInfo, info, pInfo, pCommonInfo);
		
		javaSig = (char*)(*env)->GetStringUTFChars(env, javaSignature, NULL);
		methName = (char*)(*env)->GetStringUTFChars(env, methodName, NULL);
		info->fMethod = (*env)->GetMethodID(env, declaringClass, methName, javaSig);
		(*env)->ReleaseStringUTFChars(env, javaSignature, javaSig);
		(*env)->ReleaseStringUTFChars(env, methodName, methName);
		
		// TODO DIRECT C++ virtual thunk
		dcSig = (*env)->GetStringUTFChars(env, dcSignature, NULL);
		info->fInfo.fDCCallback = dcbNewCallback(dcSig, NativeToJavaCallHandler, info);
		(*env)->ReleaseStringUTFChars(env, dcSignature, dcSig);
	} 
	else if (virtualIndex >= 0)
	{
	    const char* ds;
		NEW_STRUCT(VirtualMethodCallInfo, info, pInfo, pCommonInfo);
		
		info->fClass = declaringClass;
		info->fHasThisPtrArg = startsWithThis;
		info->fVirtualIndex = virtualIndex;
		info->fVirtualTableOffset = virtualTableOffset;
		
		// TODO DIRECT C++ virtual thunk
		ds = (*env)->GetStringUTFChars(env, dcSignature, NULL);
		info->fInfo.fDCCallback = dcbNewCallback(ds, JavaToVirtualMethodCallHandler, info);
		(*env)->ReleaseStringUTFChars(env, dcSignature, ds);
		
		callbackToRegister = info->fInfo.fDCCallback;
	} 
	else
	{
		NEW_STRUCT(FunctionCallInfo, info, pInfo, pCommonInfo);
		
		info->fForwardedSymbol = (void*)(size_t)forwardedPointer;
		if (direct && forwardedPointer)
			info->fInfo.fDCCallback = (DCCallback*)dcRawCallAdapterSkipTwoArgs((void (*)())forwardedPointer, callMode);
		
		if (!info->fInfo.fDCCallback) {
			const char* ds = (*env)->GetStringUTFChars(env, dcSignature, NULL);
			info->fInfo.fDCCallback = dcbNewCallback(ds, JavaToFunctionCallHandler, info);
			(*env)->ReleaseStringUTFChars(env, dcSignature, ds);
		}
		callbackToRegister = info->fInfo.fDCCallback;
	}
	
	initCommonCallInfo(pCommonInfo, env, callMode, nParams, returnValueType, paramsValueTypes);
	
	if (callbackToRegister) {
		JNINativeMethod meth;
		meth.fnPtr = callbackToRegister;
		meth.name = (char*)(*env)->GetStringUTFChars(env, methodName, NULL);
		meth.signature = (char*)(*env)->GetStringUTFChars(env, javaSignature, NULL);
		(*env)->RegisterNatives(env, declaringClass, &meth, 1);
		
		(*env)->ReleaseStringUTFChars(env, methodName, meth.name);
		(*env)->ReleaseStringUTFChars(env, javaSignature, meth.signature);
	}
	return (jlong)(size_t)pInfo;
}

JNIEXPORT void JNICALL Java_com_bridj_JNI_freeCallback(JNIEnv *env, jclass clazz, jlong nativeCallback)
{
	CallInfo* info = (CallInfo*)nativeCallback;
	if (info->nParams)
		free(info->fParamTypes);
	
	dcbFreeCallback((DCCallback*)info->fDCCallback);
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
