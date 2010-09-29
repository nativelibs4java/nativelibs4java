#include "HandlersCommon.h"

#if defined (DC__OS_Darwin)
#include <objc/message.h>
char __cdecl doJavaToObjCCallHandler(DCArgs* args, DCValue* result, JavaToObjCCallInfo *info)
{
	CallTempStruct* call;
	jobject instance = initCallHandler(args, &call, NULL);
	JNIEnv* env = call->env;
	call->pCallIOs = info->fInfo.fCallIOs;
	
	void* targetId = info->fNativeClass ? JLONG_TO_PTR(info->fNativeClass) : getNativeObjectPointer(env, instance, NULL);
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

	dcMode(call->vm, info->fInfo.fDCMode);

	dcArgPointer(call->vm, targetId);
	dcArgPointer(call->vm, info->fSelector);
	
	followArgs(call, args, info->fInfo.nParams, info->fInfo.fParamTypes) 
	&&
	followCall(call, ePointerValue/*info->fInfo.fReturnType*/, result, callback, JNI_FALSE, JNI_FALSE);

	cleanupCallHandler(call);
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

