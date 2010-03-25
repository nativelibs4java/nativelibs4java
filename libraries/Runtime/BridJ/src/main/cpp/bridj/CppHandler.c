#include "HandlersCommon.h"

void callDefaultConstructor(void* constructor, void* thisPtr, int callMode)
{
	CallTempStruct* call;
	JNIEnv *env;
	initCallHandler(NULL, &call, &env);
	
	dcMode(call->vm, callMode);
	dcArgPointer(call->vm, thisPtr);
	dcCallVoid(call->vm, constructor);
}

void* getNthVirtualMethodFromThis(JNIEnv* env, void* thisPtr, size_t virtualTableOffset, size_t virtualIndex) {
	// Get virtual pointer table
	void* ret;
	void** vptr = (void**)*((void**)thisPtr);
	if (!vptr) {
		throwException(env, "Null virtual pointer table !");
		return NULL;
	}
	ret = (void*)vptr[virtualIndex];
	if (!ret)
		throwException(env, "Failed to get the method pointer from the virtual table !");
	
	return ret;
}

char __cdecl doJavaToVirtualMethodCallHandler(DCArgs* args, DCValue* result, VirtualMethodCallInfo *info)
{
	CallTempStruct* call;
	JNIEnv *env;
	jobject instance = initCallHandler(args, &call, &env);
	
	void* callback;
	int nParams = info->fInfo.nParams;
	ValueType *pParamTypes = info->fInfo.fParamTypes;
	void* thisPtr;
	
	//jobject objOrClass;
	
	dcMode(call->vm, info->fInfo.fDCMode);

	if (info->fHasThisPtrArg) {
		if (nParams == 0 || *pParamTypes != eSizeTValue) {
			throwException(env, "A C++ method must be bound with a method having a first argument of type long !");
			cleanupCallHandler(env, call);
			return info->fInfo.fDCReturnType;
		}
		thisPtr = dcbArgPointer(args);
		if (!thisPtr) {
			throwException(env, "Calling a method on a NULL C++ class pointer !");
			cleanupCallHandler(env, call);
			return info->fInfo.fDCReturnType;
		}
		nParams--;
		pParamTypes++;
		
	} else {
		thisPtr = getNativeObjectPointer(env, instance, info->fClass);
		if (!thisPtr) {
			throwException(env, "Failed to get the pointer to the target C++ instance of the method invocation !");
			cleanupCallHandler(env, call);
			return info->fInfo.fDCReturnType;
		}
		
		//nParams--;
		//pParamTypes++;
		
	}
	
	callback = getNthVirtualMethodFromThis(env, thisPtr, info->fVirtualTableOffset, info->fVirtualIndex);
	if (!callback) {
		cleanupCallHandler(env, call);
		return info->fInfo.fDCReturnType;
	}
		
	dcArgPointer(call->vm, thisPtr);

	followArgs(env, args, call->vm, nParams, pParamTypes) 
	&&
	followCall(env, info->fInfo.fReturnType, call->vm, result, callback);

	cleanupCallHandler(env, call);
	return info->fInfo.fDCReturnType;
}


char __cdecl JavaToVirtualMethodCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata)
{
	VirtualMethodCallInfo* info = (VirtualMethodCallInfo*)userdata;
	BEGIN_TRY();
	return doJavaToVirtualMethodCallHandler(args, result, info);
	END_TRY_RET(info->fInfo.fEnv, 0);
}

char __cdecl doJavaToCPPMethodCallHandler(DCArgs* args, DCValue* result, CPPMethodCallInfo *info)
{
	CallTempStruct* call;
	JNIEnv *env;
	void* thisPtr;
	jobject instance = initCallHandler(args, &call, &env);
	
	dcMode(call->vm, info->fInfo.fDCMode);
	
	thisPtr = getNativeObjectPointer(env, instance, info->fClass);
	if (!thisPtr) {
		throwException(env, "Failed to get the pointer to the target C++ instance of the method invocation !");
		cleanupCallHandler(env, call);
		return info->fInfo.fDCReturnType;
	}
	dcArgPointer(call->vm, thisPtr);
	
	followArgs(env, args, call->vm, info->fInfo.nParams, info->fInfo.fParamTypes) 
	&&
	followCall(env, info->fInfo.fReturnType, call->vm, result, info->fForwardedSymbol);

	cleanupCallHandler(env, call);
	return info->fInfo.fDCReturnType;
}

char __cdecl JavaToCPPMethodCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata)
{
	FunctionCallInfo* info = (FunctionCallInfo*)userdata;
	BEGIN_TRY();
	return doJavaToCPPMethodCallHandler(args, result, (CPPMethodCallInfo*)userdata);
	END_TRY_RET(info->fInfo.fEnv, 0);
}

