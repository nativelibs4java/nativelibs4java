#include "bridj.hpp"
#include <jni.h>
#include "Exceptions.h"

typedef char BOOL;
#define TRUE 1
#define FALSE 0

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

BOOL followArgs(JNIEnv* env, DCArgs* args, DCCallVM* vm, int nTypes, ValueType* pTypes) 
{	
	int iParam;
	for (iParam = 0; iParam < nTypes; iParam++) {
		ValueType type = pTypes[iParam];
		switch (type) {
			case eIntValue:
				dcArgInt(vm, dcbArgInt(args));
				break;
			case eCLongValue:
				dcArgLong(vm, (long)dcbArgLongLong(args));
				break;
			case eSizeTValue:
				if (sizeof(size_t) == 4)
					dcArgInt(vm, (int)dcbArgLong(args));
				else
					dcArgLongLong(vm, dcbArgLongLong(args));
				break;
			case eLongValue:
				dcArgLongLong(vm, dcbArgLongLong(args));
				break;
			case eShortValue:
				dcArgShort(vm, dcbArgShort(args));
				break;
			case eByteValue:
				dcArgChar(vm, dcbArgChar(args));
				break;
			case eFloatValue:
				dcArgFloat(vm, dcbArgFloat(args));
				break;
			case eDoubleValue:
				dcArgDouble(vm, dcbArgDouble(args));
				break;
			case ePointerValue:
				{
					jobject jptr = (jobject)dcbArgPointer(args);
					void* ptr = jptr ? getPointerPeer(env, (void*)jptr) : NULL;
					dcArgPointer(vm, ptr);
				}
				break;
			default:
				throwException(env, "Invalid argument value type !");
				return FALSE;
			
		}
	}
	return TRUE;
}

BOOL followCall(JNIEnv* env, ValueType returnType, DCCallVM* vm, DCValue* result, void* callback) 
{
	switch (returnType) {
#define CALL_CASE(valueType, capCase, hiCase, uni) \
		case valueType: \
			result->uni = dcCall ## capCase(vm, callback); \
			break;
		CALL_CASE(eIntValue, Int, INT, i)
		CALL_CASE(eLongValue, Long, LONGLONG, l)
		CALL_CASE(eShortValue, Short, SHORT, s)
		CALL_CASE(eFloatValue, Float, FLOAT, f)
		CALL_CASE(eDoubleValue, Double, DOUBLE, d)
		CALL_CASE(eByteValue, Char, CHAR, c)
		case eCLongValue:
			result->l = dcCallLong(vm, callback);
			break;
		case eSizeTValue:
			result->p = dcCallPointer(vm, callback);
			break;
		case eVoidValue:
			dcCallVoid(vm, callback);
			break;
		case ePointerValue:
			{
				void* ptr = dcCallPointer(vm, callback);
				result->p = createPointer(env, ptr, NULL);
			}
			break;
		case eWCharValue:
			// TODO
		default:
			throwException(env, "Invalid return value type !");
			return FALSE;
	}
	return TRUE;
}

jobject initCallHandler(DCArgs* args, DCCallVM** vmOut, JNIEnv** envOut) {
	THREAD_STATIC DCCallVM* vm = NULL;
	JNIEnv *env = NULL;
	jobject instance = NULL;
	
	if (args) {
		env = (JNIEnv*)dcbArgPointer(args); // first arg = Java env
		instance = dcbArgPointer(args); // skip second arg = jclass or jobject
	}
	
	if (!vm) {
		vm = dcNewCallVM(1024);
	} else {
		// reset is done by dcMode anyway ! dcReset(vm);
	}
	
	*vmOut = vm;
	*envOut = env;
	return instance;
}

char __cdecl doJavaToFunctionCallHandler(DCArgs* args, DCValue* result, FunctionCallInfo *info)
{
	DCCallVM* vm;
	JNIEnv *env;
	initCallHandler(args, &vm, &env);
	
	dcMode(vm, info->fInfo.fDCMode);
	followArgs(env, args, vm, info->fInfo.nParams, info->fInfo.fParamTypes) 
	&&
	followCall(env, info->fInfo.fReturnType, vm, result, info->fForwardedSymbol);

	return info->fInfo.fDCReturnType;
}

char __cdecl doJavaToVirtualMethodCallHandler(DCArgs* args, DCValue* result, VirtualMethodCallInfo *info)
{
	DCCallVM* vm;
	JNIEnv *env;
	jobject instance = initCallHandler(args, &vm, &env);
	
	void* callback;
	int nParams = info->fInfo.nParams;
	ValueType *pParamTypes = info->fInfo.fParamTypes;
	void* thisPtr;
	
	//jobject objOrClass;
	
	dcMode(vm, info->fInfo.fDCMode);

	if (info->fHasThisPtrArg) {
		if (nParams == 0 || *pParamTypes != eSizeTValue) {
			throwException(env, "A C++ method must be bound with a method having a first argument of type long !");
			return info->fInfo.fDCReturnType;
		}
		thisPtr = dcbArgPointer(args);
		if (!thisPtr) {
			throwException(env, "Calling a method on a NULL C++ class pointer !");
			return info->fInfo.fDCReturnType;
		}
		nParams--;
		pParamTypes++;
		
	} else {
		thisPtr = getNativeObjectPointer(env, instance, info->fClass);
		if (!thisPtr) {
			throwException(env, "Failed to get the pointer to the target C++ instance of the method invocation !");
			return info->fInfo.fDCReturnType;
		}
		
	}
	
	callback = getNthVirtualMethodFromThis(env, thisPtr, info->fVirtualTableOffset, info->fVirtualIndex);
	if (!callback)
		return info->fInfo.fDCReturnType;
		
	dcArgPointer(vm, thisPtr);

	followArgs(env, args, vm, nParams, pParamTypes) 
	&&
	followCall(env, info->fInfo.fReturnType, vm, result, callback);

	return info->fInfo.fDCReturnType;
}

void callDefaultConstructor(void* constructor, void* thisPtr, int callMode)
{
	DCCallVM* vm;
	JNIEnv *env;
	initCallHandler(NULL, &vm, &env);
	
	dcMode(vm, callMode);
	dcArgPointer(vm, thisPtr);
	dcCallVoid(vm, constructor);
	return DC_SIGCHAR_VOID;
}

char __cdecl doNativeToJavaCallHandler(DCArgs* args, DCValue* result, JavaCallbackCallInfo *info)
{
	THREAD_STATIC DCCallVM* vm = NULL;
	JNIEnv *env = info->fInfo.fEnv;
	jthrowable exc;
    
	if (!vm) {
		vm = dcNewCallVM(1024);
	} else {
		// reset is done by dcMode anyway ! dcReset(vm);
	}
	
	dcMode(vm, 0);
	
	dcArgPointer(vm, (DCpointer)env);
	dcArgPointer(vm, info->fCallbackInstance);
	dcArgPointer(vm, info->fMethod);
	
	followArgs(env, args, vm, info->fInfo.nParams, info->fInfo.fParamTypes)
	&&
	followCall(env, info->fInfo.fReturnType, vm, result, info->fJNICallFunction);

	exc = (*env)->ExceptionOccurred(env);
	if (exc) {
		(*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
		// TODO rethrow in native world ?
	}
	return info->fInfo.fDCReturnType;
}


char __cdecl JavaToFunctionCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata)
{
	FunctionCallInfo* info = (FunctionCallInfo*)userdata;
	BEGIN_TRY();
	return doJavaToFunctionCallHandler(args, result, (FunctionCallInfo*)userdata);
	END_TRY_RET(info->fInfo.fEnv, 0);
}

char __cdecl JavaToVirtualMethodCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata)
{
	VirtualMethodCallInfo* info = (VirtualMethodCallInfo*)userdata;
	BEGIN_TRY();
	return doJavaToVirtualMethodCallHandler(args, result, info);
	END_TRY_RET(info->fInfo.fEnv, 0);
}


char __cdecl NativeToJavaCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata)
{
	JavaCallbackCallInfo* info = (JavaCallbackCallInfo*)userdata;
	BEGIN_TRY();
	return doNativeToJavaCallHandler(args, result, info);
	END_TRY_RET(info->fInfo.fEnv, 0);
}

