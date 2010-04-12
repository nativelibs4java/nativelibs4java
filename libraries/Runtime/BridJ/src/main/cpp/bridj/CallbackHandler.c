#include "HandlersCommon.h"

char __cdecl doNativeToJavaCallHandler(DCArgs* args, DCValue* result, NativeToJavaCallbackCallInfo *info)
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
	dcArgPointer(call->vm, (DCpointer)call->env);
	dcArgPointer(call->vm, info->fCallbackInstance);
	dcArgPointer(call->vm, info->fMethod);
	
	followArgs(call, args, info->fInfo.nParams, info->fInfo.fParamTypes)
	&&
	followCall(call, info->fInfo.fReturnType, result, info->fJNICallFunction, JNI_TRUE);

	exc = (*env)->ExceptionOccurred(env);
	if (exc) {
		(*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
		// TODO rethrow in native world ?
	}
	
	cleanupCallHandler(call);
	return info->fInfo.fDCReturnType;
}

char __cdecl doJavaToNativeCallHandler(DCArgs* args, DCValue* result, JavaToNativeCallbackCallInfo *info)
{
	void* callback;
	CallTempStruct* call;
	jobject instance = initCallHandler(args, &call, NULL);
	call->pCallIOs = info->fInfo.fCallIOs;
	
	dcMode(call->vm, info->fInfo.fDCMode);
	callback = getNativeObjectPointer(call->env, instance, NULL);
	
	followArgs(call, args, info->fInfo.nParams, info->fInfo.fParamTypes)
	&&
	followCall(call, info->fInfo.fReturnType, result, callback, JNI_FALSE);

	cleanupCallHandler(call);
	return info->fInfo.fDCReturnType;
}

char __cdecl NativeToJavaCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata)
{
	NativeToJavaCallbackCallInfo* info = (NativeToJavaCallbackCallInfo*)userdata;
	BEGIN_TRY();
	return doNativeToJavaCallHandler(args, result, info);
	END_TRY_RET(info->fInfo.fEnv, 0);
}

char __cdecl JavaToNativeCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata)
{
	JavaToNativeCallbackCallInfo* info = (JavaToNativeCallbackCallInfo*)userdata;
	BEGIN_TRY();
	return doJavaToNativeCallHandler(args, result, info);
	END_TRY_RET(info->fInfo.fEnv, 0);
}

