#include "HandlersCommon.h"

char __cdecl JavaToFunctionCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata)
{
	FunctionCallInfo* info = (FunctionCallInfo*)userdata;
	CallTempStruct* call;
	JNIEnv* env;
	initCallHandler(args, &call, NULL, &info->fInfo);
	env = call->env;
	BEGIN_TRY(env, call);
	
	call->pCallIOs = info->fInfo.fCallIOs;
	
	if (info->fCheckLastError)
		clearLastError(info->fInfo.fEnv);
	
	dcMode(call->vm, info->fInfo.fDCMode);
	followArgs(call, args, info->fInfo.nParams, info->fInfo.fParamTypes, JNI_FALSE, JNI_FALSE) 
	&&
	followCall(call, info->fInfo.fReturnType, result, info->fForwardedSymbol, JNI_FALSE, JNI_FALSE);

	if (info->fCheckLastError)
		throwIfLastError(info->fInfo.fEnv);
	
	END_TRY(info->fInfo.fEnv, call);
	cleanupCallHandler(call);
	
	return info->fInfo.fDCReturnType;
}

