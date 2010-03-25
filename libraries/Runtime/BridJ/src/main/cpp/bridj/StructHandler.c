#include "HandlersCommon.h"

extern jclass gStructFieldsIOClass;

char __cdecl doStructHandler(DCArgs* args, DCValue* result, StructFieldInfo *info)
{
	JNIEnv *env;
	CallTempStruct* call;
	jobject instance = initCallHandler(args, &call, &env);
	
	
	dcMode(call->vm, DC_CALL_C_DEFAULT);

	dcArgPointer(call->vm, (void*)env);
	dcArgPointer(call->vm, gStructFieldsIOClass);
	dcArgPointer(call->vm, info->fMethod);
	dcArgPointer(call->vm, instance);
	dcArgInt(call->vm, info->fFieldIndex);
	
	followArgs(env, args, call, info->fInfo.nParams, info->fInfo.fParamTypes) 
	&&
	followCall(env, info->fInfo.fReturnType, call, result, info->fJNICallFunction);

	cleanupCallHandler(env, call);
	
	// Special case for setters that return this :
	if (info->fInfo.nParams == 1 && info->fInfo.fReturnType != eVoidValue) {
		result->p = instance;
		return DC_SIGCHAR_POINTER;
	} else
		return info->fInfo.fDCReturnType;
}

char __cdecl StructHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata) {
	StructFieldInfo* info = (StructFieldInfo*)userdata;
	BEGIN_TRY();
	return doStructHandler(args, result, info);
	END_TRY_RET(info->fInfo.fEnv, 0);
}
