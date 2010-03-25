#include "HandlersCommon.h"

char __cdecl doJavaToFunctionCallHandler(DCArgs* args, DCValue* result, FunctionCallInfo *info)
{
	CallTempStruct* call;
	JNIEnv *env;
	jobject instance = initCallHandler(args, &call, &env);
	
	dcMode(call->vm, info->fInfo.fDCMode);
	followArgs(env, args, call, info->fInfo.nParams, info->fInfo.fParamTypes) 
	&&
	followCall(env, info->fInfo.fReturnType, call, result, info->fForwardedSymbol);

	cleanupCallHandler(env, call);
	return info->fInfo.fDCReturnType;
}

char __cdecl JavaToFunctionCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata)
{
	FunctionCallInfo* info = (FunctionCallInfo*)userdata;
	BEGIN_TRY();
	return doJavaToFunctionCallHandler(args, result, (FunctionCallInfo*)userdata);
	END_TRY_RET(info->fInfo.fEnv, 0);
}

