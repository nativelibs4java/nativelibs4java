#include "HandlersCommon.h"

char __cdecl doNativeToJavaCallHandler(DCArgs* args, DCValue* result, NativeToJavaCallbackCallInfo *info)
{
	CallTempStruct* call;
	JNIEnv *env = info->fInfo.fEnv;
	jthrowable exc;

	initCallHandler(NULL, &call, NULL);
	
	dcMode(call->vm, 0);
	
	dcArgPointer(call->vm, (DCpointer)env);
	dcArgPointer(call->vm, info->fCallbackInstance);
	dcArgPointer(call->vm, info->fMethod);
	
	followArgs(env, args, call, info->fInfo.nParams, info->fInfo.fParamTypes)
	&&
	followCall(env, info->fInfo.fReturnType, call, result, info->fJNICallFunction);

	exc = (*env)->ExceptionOccurred(env);
	if (exc) {
		(*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
		// TODO rethrow in native world ?
	}
	
	cleanupCallHandler(env, call);
	return info->fInfo.fDCReturnType;
}

char __cdecl doJavaToNativeCallHandler(DCArgs* args, DCValue* result, JavaToNativeCallbackCallInfo *info)
{
	void* callback;
	CallTempStruct* call;
	JNIEnv *env;
	jobject instance = initCallHandler(args, &call, &env);
	
	dcMode(call->vm, info->fInfo.fDCMode);
	callback = getNativeObjectPointer(env, instance, NULL);
	
	followArgs(env, args, call, info->fInfo.nParams, info->fInfo.fParamTypes)
	&&
	followCall(env, info->fInfo.fReturnType, call, result, callback);

	cleanupCallHandler(env, call);
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

