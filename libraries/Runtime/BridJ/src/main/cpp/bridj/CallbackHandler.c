#include "HandlersCommon.h"

#if defined(DC__OS_Win32) && !defined(DC__OS_Win64)
#define JNI_CALL_MODE DC_CALL_C_X86_WIN32_STD
#else
#define JNI_CALL_MODE DC_CALL_C_DEFAULT
#endif

char __cdecl CToJavaCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata)
{
	CallTempStruct* call;
	jthrowable exc;
	NativeToJavaCallbackCallInfo* info = (NativeToJavaCallbackCallInfo*)userdata;
	JNIEnv *env = GetEnv();
	initCallHandler(NULL, &call, env, &info->fInfo);
	
	BEGIN_TRY(env, call);
	
	call->pCallIOs = info->fInfo.fCallIOs;
	
	dcMode(call->vm, JNI_CALL_MODE);
	
	if (!info->fCallbackInstance)
	{
		throwException(env, "Trying to call a null callback instance !");
		cleanupCallHandler(call);
		return info->fInfo.fDCReturnType;
	}

	if (0) {
		float value = dcbArgFloat(args);
		float ret = (*call->env)->CallFloatMethod(call->env, info->fCallbackInstance, info->fInfo.fMethodID, value);
		result->f = ret;
	} else {
	dcArgPointer(call->vm, (DCpointer)call->env);
	dcArgPointer(call->vm, info->fCallbackInstance);
	dcArgPointer(call->vm, info->fInfo.fMethodID);
	
	if (info->fIsGenericCallback) {
		followArgsGenericJavaCallback(call, args, info->fInfo.nParams, info->fInfo.fParamTypes)
		&&
		followCallGenericJavaCallback(call, info->fInfo.fReturnType, result, (void*)(*env)->CallObjectMethod);
	} else {
		followArgs(call, args, info->fInfo.nParams, info->fInfo.fParamTypes, JNI_TRUE, JNI_TRUE)
		&&
		followCall(call, info->fInfo.fReturnType, result, info->fJNICallFunction, JNI_TRUE, JNI_FALSE);
	}
	
	exc = (*env)->ExceptionOccurred(env);
	if (exc) {
		(*env)->ExceptionDescribe(env);
        printStackTrace(env, exc);
		//(*env)->ExceptionClear(env);
	}
	}
	cleanupCallHandler(call);
	END_TRY_BASE(info->fInfo.fEnv, call, cleanupCallHandler(call););
	
	return info->fInfo.fDCReturnType;
	
}
char __cdecl CPPToJavaCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata)
{
	void* cppObject;
	jobject javaObject;
	CallTempStruct* call;
	jthrowable exc;
	NativeToJavaCallbackCallInfo* info = (NativeToJavaCallbackCallInfo*)userdata;
	JNIEnv *env = GetEnv();
	initCallHandler(NULL, &call, env, &info->fInfo);
	BEGIN_TRY(env, call);
	
	call->pCallIOs = info->fInfo.fCallIOs;
	
	dcMode(call->vm, JNI_CALL_MODE);
	
	if (info->fCallbackInstance)
	{
		throwException(env, "Not expecting a callback instance here !");
		cleanupCallHandler(call);
		return info->fInfo.fDCReturnType;
	}
	
	cppObject = dcbArgPointer(args);
	javaObject = getJavaObjectForNativePointer(env, cppObject);
	dcArgPointer(call->vm, (DCpointer)call->env);
	dcArgPointer(call->vm, javaObject);
	dcArgPointer(call->vm, info->fInfo.fMethodID);
	
	followArgs(call, args, info->fInfo.nParams, info->fInfo.fParamTypes, JNI_TRUE, JNI_TRUE)
	&&
	followCall(call, info->fInfo.fReturnType, result, info->fJNICallFunction, JNI_TRUE, JNI_FALSE);

	exc = (*env)->ExceptionOccurred(env);
	if (exc) {
		(*env)->ExceptionDescribe(env);
        printStackTrace(env, exc);
		//(*env)->ExceptionClear(env);
		// TODO rethrow in native world ?
	}
	
	cleanupCallHandler(call);
	END_TRY_BASE(info->fInfo.fEnv, call, cleanupCallHandler(call););
	
	return info->fInfo.fDCReturnType;
}

char __cdecl JavaToCCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata)
{
	void* callbackPtr;
	CallTempStruct* call;
	JavaToNativeCallbackCallInfo* info = (JavaToNativeCallbackCallInfo*)userdata;
	jobject instance = initCallHandler(args, &call, NULL, &info->fInfo);
	BEGIN_TRY(env, call);
	
	// printf("JavaToCCallHandler !!!\n");
	call->pCallIOs = info->fInfo.fCallIOs;
	
	dcMode(call->vm, info->fInfo.fDCMode);
	callbackPtr = getNativeObjectPointer(call->env, instance, NULL);
	
	// printf("doJavaToCCallHandler(callback = %d) !!!\n", callback);
	followArgs(call, args, info->fInfo.nParams, info->fInfo.fParamTypes, JNI_FALSE, JNI_FALSE)
	&&
	followCall(call, info->fInfo.fReturnType, result, callbackPtr, JNI_FALSE, JNI_FALSE);

	cleanupCallHandler(call);
	END_TRY_BASE(info->fInfo.fEnv, call, cleanupCallHandler(call););
	
	return info->fInfo.fDCReturnType;
}

