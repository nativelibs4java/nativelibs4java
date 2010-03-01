#include "HandlersCommon.h"

#if defined (DC__OS_Darwin)
#include <objc/message.h>
char __cdecl doJavaToObjCCallHandler(DCArgs* args, DCValue* result, JavaToObjCCallInfo *info)
{
	DCCallVM* vm;
	JNIEnv *env;
	jobject instance = initCallHandler(args, &vm, &env);
	
	void* thisPtr = thisPtr = getNativeObjectPointer(env, instance, NULL);
	void* callback = //objc_msgSend_stret;//
		objc_msgSend;
	
#if defined(DC__Arch_Intel_x86)
	switch (info->fInfo.fReturnType) {
	case eDoubleValue:
	case eFloatValue:
		callback = objc_msgSend_fpret;
		break;
	}
#endif

	dcMode(vm, info->fInfo.fDCMode);

	dcArgPointer(vm, info->fSelector);
	dcArgPointer(vm, thisPtr);

	followArgs(env, args, vm, info->fInfo.nParams, info->fInfo.fParamTypes) 
	&&
	followCall(env, info->fInfo.fReturnType, vm, result, callback);

	return info->fInfo.fDCReturnType;
}

char __cdecl JavaToObjCCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata)
{
	JavaToObjCCallInfo* info = (JavaToObjCCallInfo*)userdata;
	BEGIN_TRY();
	return doJavaToObjCCallHandler(args, result, info);
	END_TRY_RET(info->fInfo.fEnv, 0);
}
#endif

