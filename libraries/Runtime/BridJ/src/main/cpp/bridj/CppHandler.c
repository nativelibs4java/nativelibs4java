#include "HandlersCommon.h"

void callDefaultConstructor(JNIEnv* env, void* constructor, void* thisPtr, int callMode)
{
	CallTempStruct* call;
	initCallHandler(NULL, &call, env);
	
	dcMode(call->vm, callMode);
	dcArgPointer(call->vm, thisPtr);
	dcCallVoid(call->vm, constructor);

	cleanupCallHandler(call);
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
	jobject instance = initCallHandler(args, &call, NULL);
	JNIEnv* env = call->env;

	void* callback;
	int nParams = info->fInfo.nParams;
	ValueType *pParamTypes = info->fInfo.fParamTypes;
	void* thisPtr;
	
	//jobject objOrClass;
	
	dcMode(call->vm, info->fInfo.fDCMode);

	if (info->fHasThisPtrArg) {
		if (nParams == 0 || *pParamTypes != eSizeTValue) {
			throwException(env, "A C++ method must be bound with a method having a first argument of type long !");
			cleanupCallHandler(call);
			return info->fInfo.fDCReturnType;
		}
		thisPtr = dcbArgPointer(args);
		if (!thisPtr) {
			throwException(env, "Calling a method on a NULL C++ class pointer !");
			cleanupCallHandler(call);
			return info->fInfo.fDCReturnType;
		}
		nParams--;
		pParamTypes++;
		
	} else {
		thisPtr = getNativeObjectPointer(env, instance, info->fClass);
		if (!thisPtr) {
			throwException(env, "Failed to get the pointer to the target C++ instance of the method invocation !");
			cleanupCallHandler(call);
			return info->fInfo.fDCReturnType;
		}
		
		//nParams--;
		//pParamTypes++;
		
	}
	
	callback = getNthVirtualMethodFromThis(env, thisPtr, info->fVirtualTableOffset, info->fVirtualIndex);
	if (!callback) {
		cleanupCallHandler(call);
		return info->fInfo.fDCReturnType;
	}
		
	dcArgPointer(call->vm, thisPtr);

	followArgs(call, args, nParams, pParamTypes) 
	&&
	followCall(call, info->fInfo.fReturnType, result, callback);

	cleanupCallHandler(call);
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
	void* thisPtr;
	jobject instance = initCallHandler(args, &call, NULL);
	JNIEnv* env = call->env;
	
	dcMode(call->vm, info->fInfo.fDCMode);
	
	thisPtr = getNativeObjectPointer(call->env, instance, info->fClass);
	if (!thisPtr) {
		throwException(env, "Failed to get the pointer to the target C++ instance of the method invocation !");
		cleanupCallHandler(call);
		return info->fInfo.fDCReturnType;
	}
	dcArgPointer(call->vm, thisPtr);
	
	followArgs(call, args, info->fInfo.nParams, info->fInfo.fParamTypes) 
	&&
	followCall(call, info->fInfo.fReturnType, result, info->fForwardedSymbol);

	cleanupCallHandler(call);
	return info->fInfo.fDCReturnType;
}

char __cdecl JavaToCPPMethodCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata)
{
	FunctionCallInfo* info = (FunctionCallInfo*)userdata;
	BEGIN_TRY();
	return doJavaToCPPMethodCallHandler(args, result, (CPPMethodCallInfo*)userdata);
	END_TRY_RET(info->fInfo.fEnv, 0);
}

