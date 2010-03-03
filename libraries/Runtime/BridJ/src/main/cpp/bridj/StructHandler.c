#include "HandlersCommon.h"

extern jclass gStructFieldsIOClass;

char __cdecl doStructHandler(DCArgs* args, DCValue* result, StructFieldInfo *info)
{
	JNIEnv *env;
	DCCallVM* vm;
	jobject instance = initCallHandler(args, &vm, &env);
	
	
	dcMode(vm, DC_CALL_C_DEFAULT);
	
	dcArgPointer(vm, env);
	dcArgPointer(vm, gStructFieldsIOClass);
	dcArgPointer(vm, info->fMethod);
	dcArgPointer(vm, instance);
	dcArgInt(vm, info->fFieldIndex);
	
	followArgs(env, args, vm, info->fInfo.nParams, info->fInfo.fParamTypes) 
	&&
	followCall(env, info->fInfo.fReturnType, vm, result, info->fJNICallFunction);

	// Special case for setters that return this :
	if (!info->fInfo.nParams && info->fInfo.fReturnType != eVoidValue)
		result->p = instance;
	
	return info->fInfo.fDCReturnType;
}

char __cdecl StructHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata) {
	StructFieldInfo* info = (StructFieldInfo*)userdata;
	BEGIN_TRY();
	return doStructHandler(args, result, info);
	END_TRY_RET(info->fInfo.fEnv, 0);
}
