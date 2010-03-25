#include "HandlersCommon.h"

jboolean followArgs(JNIEnv* env, DCArgs* args, CallTempStruct* call, int nTypes, ValueType* pTypes) 
{	
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
				dcArgLong(call->vm, (long)dcbArgLongLong(args));
				break;
			case eSizeTValue:
				if (sizeof(size_t) == 4)
					dcArgInt(call->vm, (int)dcbArgLong(args));
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
					dcArgPointer(call->vm, ptr);
				}
				break;
			default:
				throwException(env, "Invalid argument value type !");
				return JNI_FALSE;
			
		}
	}
	return JNI_TRUE;
}

jboolean followCall(JNIEnv* env, ValueType returnType, CallTempStruct* call, DCValue* result, void* callback) 
{
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
			result->l = dcCallLong(call->vm, callback);
			break;
		case eSizeTValue:
			result->p = dcCallPointer(call->vm, callback);
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
				result->p = createPointer(env, ptr, NULL);
			}
			break;
		case eWCharValue:
			// TODO
		default:
			throwException(env, "Invalid return value type !");
			return JNI_FALSE;
	}
	return JNI_TRUE;
}

jobject initCallHandler(DCArgs* args, CallTempStruct** callOut, JNIEnv** envOut) {
	JNIEnv *env = NULL;
	jobject instance = NULL;
	
	if (args) {
		env = (JNIEnv*)dcbArgPointer(args); // first arg = Java env
		instance = dcbArgPointer(args); // skip second arg = jclass or jobject
	}
	
	*callOut = getTempCallStruct(env);
	
	if (envOut)
		*envOut = env;
	
	return instance;
}

void cleanupCallHandler(JNIEnv* env, CallTempStruct* call)
{
	dcReset(call->vm);
	releaseTempCallStruct(env, call);
}
	
