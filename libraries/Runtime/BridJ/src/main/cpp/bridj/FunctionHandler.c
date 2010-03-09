#include "HandlersCommon.h"

char __cdecl doJavaToFunctionCallHandler(DCArgs* args, DCValue* result, FunctionCallInfo *info)
{
	DCCallVM* vm;
	JNIEnv *env;
	jobject instance = initCallHandler(args, &vm, &env);
	
	dcMode(vm, info->fInfo.fDCMode);
	followArgs(env, args, vm, info->fInfo.nParams, info->fInfo.fParamTypes) 
	&&
	followCall(env, info->fInfo.fReturnType, vm, result, info->fForwardedSymbol);

	return info->fInfo.fDCReturnType;
}

char __cdecl JavaToFunctionCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata)
{
	FunctionCallInfo* info = (FunctionCallInfo*)userdata;
	BEGIN_TRY();
	return doJavaToFunctionCallHandler(args, result, (FunctionCallInfo*)userdata);
	END_TRY_RET(info->fInfo.fEnv, 0);
}

