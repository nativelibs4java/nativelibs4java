#include "HandlersCommon.h"

void callSinglePointerArgVoidFunction(JNIEnv* env, void* constructor, void* thisPtr, int callMode)
{
	CallTempStruct* call;
	initCallHandler(NULL, &call, env, NULL);
	
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
		//throwException(env, "Failed to get the method pointer from the virtual table !");
		THROW_EXCEPTION(env, "Failed to get the method pointer from the virtual table ! Virtual index = %lld, vtable ptr = 0x%llx", (jlong)virtualIndex, PTR_TO_JLONG(vptr));
	
	return ret;
}
char __cdecl JavaToVirtualMethodCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata)
{
	VirtualMethodCallInfo* info = (VirtualMethodCallInfo*)userdata;
	CallTempStruct* call;
	jobject instance = initCallHandler(args, &call, NULL, &info->fInfo);
	JNIEnv* env = call->env;
	BEGIN_TRY(env, call)
	
	void* callback;
	int nParams = info->fInfo.nParams;
	ValueType *pParamTypes = info->fInfo.fParamTypes;
	void* thisPtr;
	call->pCallIOs = info->fInfo.fCallIOs;
	
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

	followArgs(call, args, nParams, pParamTypes, JNI_FALSE, JNI_FALSE) 
	&&
	followCall(call, info->fInfo.fReturnType, result, callback, JNI_FALSE, JNI_FALSE);

	cleanupCallHandler(call);
	END_TRY_BASE(info->fInfo.fEnv, call, cleanupCallHandler(call););
	
	return info->fInfo.fDCReturnType;
}

char __cdecl JavaToCPPMethodCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata)
{
	FunctionCallInfo* info = (FunctionCallInfo*)userdata;
	CallTempStruct* call;
	void* thisPtr;
	jobject instance = initCallHandler(args, &call, NULL, &info->fInfo);
	JNIEnv* env = call->env;
	BEGIN_TRY(env, call)
	
	call->pCallIOs = info->fInfo.fCallIOs;
	
	dcMode(call->vm, info->fInfo.fDCMode);
	
	thisPtr = getNativeObjectPointer(call->env, instance, info->fClass);
	if (!thisPtr) {
		throwException(env, "Failed to get the pointer to the target C++ instance of the method invocation !");
		cleanupCallHandler(call);
		return info->fInfo.fDCReturnType;
	}
	dcArgPointer(call->vm, thisPtr);
	
	followArgs(call, args, info->fInfo.nParams, info->fInfo.fParamTypes, JNI_FALSE, JNI_FALSE) 
	&&
	followCall(call, info->fInfo.fReturnType, result, info->fForwardedSymbol, JNI_FALSE, JNI_FALSE);

	cleanupCallHandler(call);
	END_TRY_BASE(info->fInfo.fEnv, call, cleanupCallHandler(call););
	
	return info->fInfo.fDCReturnType;
}

