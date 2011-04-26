#include "HandlersCommon.h"

char __cdecl JavaToFunctionCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata)
{
	FunctionCallInfo* info = (FunctionCallInfo*)userdata;
	CallTempStruct* call;
	JNIEnv* env;
	initCallHandler(args, &call, NULL);
	env = call->env;
	BEGIN_TRY(env, call);
	
	call->pCallIOs = info->fInfo.fCallIOs;
	
	dcMode(call->vm, info->fInfo.fDCMode);
	followArgs(call, args, info->fInfo.nParams, info->fInfo.fParamTypes, JNI_FALSE, JNI_FALSE) 
	&&
	followCall(call, info->fInfo.fReturnType, result, info->fForwardedSymbol, JNI_FALSE, JNI_FALSE);

	cleanupCallHandler(call);
	END_TRY_BASE(info->fInfo.fEnv, call, cleanupCallHandler(call););
	
	return info->fInfo.fDCReturnType;
}

