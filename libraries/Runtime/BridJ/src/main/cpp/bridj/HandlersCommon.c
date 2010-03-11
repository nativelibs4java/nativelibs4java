#include "HandlersCommon.h"

jboolean followArgs(JNIEnv* env, DCArgs* args, DCCallVM* vm, int nTypes, ValueType* pTypes) 
{	
	int iParam;
	for (iParam = 0; iParam < nTypes; iParam++) {
		ValueType type = pTypes[iParam];
		switch (type) {
			case eIntFlagSet:
				dcArgInt(vm, (jint)getFlagValue(env, (jobject)dcbArgPointer(args)));
				break;
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
			case eBooleanValue:
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
				return JNI_FALSE;
			
		}
	}
	return JNI_TRUE;
}

jboolean followCall(JNIEnv* env, ValueType returnType, DCCallVM* vm, DCValue* result, void* callback) 
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
		case eBooleanValue:
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
		case eIntFlagSet:
			{
				int flags = dcCallInt(vm, callback);
				result->p = newFlagSet(env, flags);
			}
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
			return JNI_FALSE;
	}
	return JNI_TRUE;
}

void initVM(DCCallVM** vm) {
	if (!*vm) {
		*vm = dcNewCallVM(1024);
	} else {
		dcReset(*vm); // TODO remove me (reset is done by dcMode ?)
	}
}

jobject initCallHandler(DCArgs* args, DCCallVM** vmOut, JNIEnv** envOut) {
	THREAD_STATIC DCCallVM* vm = NULL;
	JNIEnv *env = NULL;
	jobject instance = NULL;
	
	if (args) {
		env = (JNIEnv*)dcbArgPointer(args); // first arg = Java env
		instance = dcbArgPointer(args); // skip second arg = jclass or jobject
	}
	
	initVM(&vm);
	
	*vmOut = vm;
	*envOut = env;
	return instance;
}

