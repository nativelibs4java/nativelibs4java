#include "HandlersCommon.h"

char __cdecl doJavaToFunctionCallHandler(DCArgs* args, DCValue* result, FunctionCallInfo *info)
{
	CallTempStruct* call;
	JNIEnv* env;
	initCallHandler(args, &call, NULL);
	env = call->env;
	call->pCallIOs = info->fInfo.fCallIOs;
	
	dcMode(call->vm, info->fInfo.fDCMode);
	followArgs(call, args, info->fInfo.nParams, info->fInfo.fParamTypes) 
	&&
	followCall(call, info->fInfo.fReturnType, result, info->fForwardedSymbol);

	cleanupCallHandler(call);
	return info->fInfo.fDCReturnType;
}

char __cdecl JavaToFunctionCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata)
{
	FunctionCallInfo* info = (FunctionCallInfo*)userdata;
	BEGIN_TRY();
	return doJavaToFunctionCallHandler(args, result, info);
	END_TRY_RET(info->fInfo.fEnv, 0);
}

