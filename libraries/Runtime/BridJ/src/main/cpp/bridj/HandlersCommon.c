#include "HandlersCommon.h"

jboolean followArgs(CallTempStruct* call, DCArgs* args, int nTypes, ValueType* pTypes, jboolean toJava) 
{	
	JNIEnv* env = call->env;
	int iParam;
	for (iParam = 0; iParam < nTypes; iParam++) {
		ValueType type = pTypes[iParam];
		switch (type) {
			case eIntFlagSet:
				dcArgInt(call->vm, (jint)getFlagValue(env, (jobject)dcbArgPointer(args)));
				break;
			case eIntValue:
				dcArgInt(call->vm, dcbArgInt(args));
				break;
			case eCLongValue:
				if (sizeof(long) == 4) {
					if (toJava)
						dcArgLongLong(call->vm, dcbArgLong(args));
					else
						dcArgLong(call->vm, (int)dcbArgLongLong(args));
				}
				else
					dcArgLong(call->vm, (long)dcbArgLongLong(args));
				break;
			case eSizeTValue:
				if (sizeof(size_t) == 4) {
					if (toJava)
						dcArgLongLong(call->vm, dcbArgInt(args));
					else
						dcArgInt(call->vm, (int)dcbArgLongLong(args));
				}
				else
					dcArgLongLong(call->vm, dcbArgLongLong(args));
				break;
			case eLongValue:
				dcArgLongLong(call->vm, dcbArgLongLong(args));
				break;
			case eShortValue:
				dcArgShort(call->vm, dcbArgShort(args));
				break;
			case eBooleanValue:
			case eByteValue:
				dcArgChar(call->vm, dcbArgChar(args));
				break;
			case eFloatValue:
				dcArgFloat(call->vm, dcbArgFloat(args));
				break;
			case eDoubleValue:
				dcArgDouble(call->vm, dcbArgDouble(args));
				break;
			case ePointerValue:
				{
					jobject jptr = (jobject)dcbArgPointer(args);
					void* ptr = jptr ? getPointerPeer(env, (void*)jptr) : NULL;
					call->pCallIOs++;
					dcArgPointer(call->vm, ptr);
				}
				break;
			case eWCharValue:
				switch (sizeof(wchar_t)) {
				case 1:
					dcArgChar(call->vm, dcbArgChar(args));
					break;
				case 2:
					dcArgShort(call->vm, dcbArgShort(args));
					break;
				case 4:
					dcArgInt(call->vm, dcbArgInt(args));
					break;
				default:
					throwException(env, "Invalid wchar_t size for argument !");
					return JNI_FALSE;
				}
				break;
			default:
				throwException(env, "Invalid argument value type !");
				return JNI_FALSE;
			
		}
	}
	if ((*env)->ExceptionCheck(env))
		return JNI_FALSE;
	return JNI_TRUE;
}

jboolean followCall(CallTempStruct* call, ValueType returnType, DCValue* result, void* callback, jboolean bCallingJava, jboolean forceVoidReturn) 
{
	JNIEnv* env = call->env;
	switch (returnType) {
#define CALL_CASE(valueType, capCase, hiCase, uni) \
		case valueType: \
			result->uni = dcCall ## capCase(call->vm, callback); \
			break;
		CALL_CASE(eIntValue, Int, INT, i)
		CALL_CASE(eLongValue, Long, LONGLONG, l)
		CALL_CASE(eShortValue, Short, SHORT, s)
		CALL_CASE(eFloatValue, Float, FLOAT, f)
		CALL_CASE(eDoubleValue, Double, DOUBLE, d)
		case eBooleanValue:
		CALL_CASE(eByteValue, Char, CHAR, c)
		case eCLongValue:
			result->l = (jlong)dcCallLong(call->vm, callback);
			break;
		case eSizeTValue:
		    result->l = PTR_TO_JLONG(dcCallPointer(call->vm, callback));
			break;
		case eVoidValue:
			dcCallVoid(call->vm, callback);
			break;
		case eIntFlagSet:
			{
				int flags = dcCallInt(call->vm, callback);
				result->p = newFlagSet(env, flags);
			}
			break;
		case ePointerValue:
			{
				void* ptr = dcCallPointer(call->vm, callback);
				if (bCallingJava)
					result->p = ptr;
				else
				{
					jobject callIO = *call->pCallIOs;
					result->p = createPointerFromIO(env, ptr, callIO);
				}
				call->pCallIOs++;
			}
			break;
		case eWCharValue:
			switch (sizeof(wchar_t)) {
			case 1:
				result->c = dcCallChar(call->vm, callback);
				break;
			case 2:
				result->s = dcCallShort(call->vm, callback);
				break;
			case 4:
				result->i = dcCallInt(call->vm, callback);
				break;
			default:
				throwException(env, "Invalid wchar_t size !");
				return JNI_FALSE;
			}
			break;
		default:
			if (forceVoidReturn) 
			{
				dcCallVoid(call->vm, callback);
				break;
			}
			throwException(env, "Invalid return value type !");
			return JNI_FALSE;
	}
	if (bCallingJava && (*env)->ExceptionCheck(env))
		return JNI_FALSE;
	return JNI_TRUE;
}

jobject initCallHandler(DCArgs* args, CallTempStruct** callOut, JNIEnv* env) 
{
	jobject instance = NULL;

	if (args) {
		env = (JNIEnv*)dcbArgPointer(args); // first arg = Java env
		instance = dcbArgPointer(args); // skip second arg = jclass or jobject
	}
	if (env) {
		*callOut = getTempCallStruct(env);
		(*callOut)->env = env;
	} else
		*callOut = NULL;

	return instance;
}

void cleanupCallHandler(CallTempStruct* call)
{
	dcReset(call->vm);
	releaseTempCallStruct(call->env, call);
}
	
