#include "HandlersCommon.h"

char __cdecl doCPPToJavaCallHandler(DCArgs* args, DCValue* result, NativeToJavaCallbackCallInfo *info)
{
	void* cppObject;
	jobject javaObject;
	CallTempStruct* call;
	jthrowable exc;
	JNIEnv *env = info->fInfo.fEnv;
	initCallHandler(NULL, &call, env);
	call->pCallIOs = info->fInfo.fCallIOs;
	
	dcMode(call->vm, 0);
	
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
	dcArgPointer(call->vm, info->fMethod);
	
	followArgs(call, args, info->fInfo.nParams, info->fInfo.fParamTypes, JNI_TRUE)
	&&
	followCall(call, info->fInfo.fReturnType, result, info->fJNICallFunction, JNI_TRUE, JNI_FALSE);

	exc = (*env)->ExceptionOccurred(env);
	if (exc) {
		(*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
		// TODO rethrow in native world ?
	}
	
	cleanupCallHandler(call);
	return info->fInfo.fDCReturnType;
}

char __cdecl doCToJavaCallHandler(DCArgs* args, DCValue* result, NativeToJavaCallbackCallInfo *info)
{
	CallTempStruct* call;
	jthrowable exc;
	JNIEnv *env = info->fInfo.fEnv;
	initCallHandler(NULL, &call, env);
	call->pCallIOs = info->fInfo.fCallIOs;
	
	dcMode(call->vm, 0);
	
	if (!info->fCallbackInstance)
	{
		throwException(env, "Trying to call a null callback instance !");
		cleanupCallHandler(call);
		return info->fInfo.fDCReturnType;
	}

	if (0) {
		float value = dcbArgFloat(args);
		float ret = (*call->env)->CallFloatMethod(call->env, info->fCallbackInstance, info->fMethod, value);
		result->f = ret;
	} else {
	dcArgPointer(call->vm, (DCpointer)call->env);
	dcArgPointer(call->vm, info->fCallbackInstance);
	dcArgPointer(call->vm, info->fMethod);
	
	if (info->fIsGenericCallback) {
		followArgsGenericJavaCallback(call, args, info->fInfo.nParams, info->fInfo.fParamTypes)
		&&
		followCallGenericJavaCallback(call, info->fInfo.fReturnType, result, (void*)(*env)->CallObjectMethod);
	} else {
		followArgs(call, args, info->fInfo.nParams, info->fInfo.fParamTypes, JNI_TRUE)
		&&
		followCall(call, info->fInfo.fReturnType, result, info->fJNICallFunction, JNI_TRUE, JNI_FALSE);
	}
	
	exc = (*env)->ExceptionOccurred(env);
	if (exc) {
		(*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
		// TODO rethrow in native world ?
	}
	}
	cleanupCallHandler(call);
	return info->fInfo.fDCReturnType;
}

char __cdecl doJavaToCCallHandler(DCArgs* args, DCValue* result, JavaToNativeCallbackCallInfo *info)
{
	void* callback;
	CallTempStruct* call;
	jobject instance = initCallHandler(args, &call, NULL);
	call->pCallIOs = info->fInfo.fCallIOs;
	
	dcMode(call->vm, info->fInfo.fDCMode);
	callback = getNativeObjectPointer(call->env, instance, NULL);
	
	// printf("doJavaToCCallHandler(callback = %d) !!!\n", callback);
	followArgs(call, args, info->fInfo.nParams, info->fInfo.fParamTypes, JNI_FALSE)
	&&
	followCall(call, info->fInfo.fReturnType, result, callback, JNI_FALSE, JNI_FALSE);

	cleanupCallHandler(call);
	return info->fInfo.fDCReturnType;
}

char __cdecl CToJavaCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata)
{
	NativeToJavaCallbackCallInfo* info = (NativeToJavaCallbackCallInfo*)userdata;
	BEGIN_TRY();
	return doCToJavaCallHandler(args, result, info);
	END_TRY_RET(info->fInfo.fEnv, 0);
}
char __cdecl CPPToJavaCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata)
{
	NativeToJavaCallbackCallInfo* info = (NativeToJavaCallbackCallInfo*)userdata;
	BEGIN_TRY();
	return doCPPToJavaCallHandler(args, result, info);
	END_TRY_RET(info->fInfo.fEnv, 0);
}

char __cdecl JavaToCCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata)
{
	JavaToNativeCallbackCallInfo* info = (JavaToNativeCallbackCallInfo*)userdata;
	BEGIN_TRY();
	// printf("JavaToCCallHandler !!!\n");
	return doJavaToCCallHandler(args, result, info);
	END_TRY_RET(info->fInfo.fEnv, 0);
}

