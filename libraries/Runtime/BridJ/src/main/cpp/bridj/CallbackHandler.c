#include "HandlersCommon.h"

char __cdecl doNativeToJavaCallHandler(DCArgs* args, DCValue* result, NativeToJavaCallbackCallInfo *info)
{
	THREAD_STATIC DCCallVM* vm = NULL;
	JNIEnv *env = info->fInfo.fEnv;
	jthrowable exc;

	initVM(&vm);
	dcMode(vm, 0);
	
	dcArgPointer(vm, (DCpointer)env);
	dcArgPointer(vm, info->fCallbackInstance);
	dcArgPointer(vm, info->fMethod);
	
	followArgs(env, args, vm, info->fInfo.nParams, info->fInfo.fParamTypes)
	&&
	followCall(env, info->fInfo.fReturnType, vm, result, info->fJNICallFunction);

	exc = (*env)->ExceptionOccurred(env);
	if (exc) {
		(*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
		// TODO rethrow in native world ?
	}
	return info->fInfo.fDCReturnType;
}

char __cdecl doJavaToNativeCallHandler(DCArgs* args, DCValue* result, JavaToNativeCallbackCallInfo *info)
{
	void* callback;
    DCCallVM* vm;
	JNIEnv *env;
	jobject instance = initCallHandler(args, &vm, &env);
	
	dcMode(vm, info->fInfo.fDCMode);
	callback = getNativeObjectPointer(env, instance, NULL);
	
	followArgs(env, args, vm, info->fInfo.nParams, info->fInfo.fParamTypes)
	&&
	followCall(env, info->fInfo.fReturnType, vm, result, callback);

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

