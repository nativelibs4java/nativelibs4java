#include "HandlersCommon.h"

jboolean followArgs(CallTempStruct* call, DCArgs* args, int nTypes, ValueType* pTypes, jboolean toJava) 
{	
	JNIEnv* env = call->env;
	int iParam;
	//printf("ARGS : %d args\n", (int)nTypes);
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
					// printf("ARG POINTER = %d\n", ptr);
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
			case eEllipsis: {
				if (toJava) {
					throwException(env, "Calling Java ellipsis is not supported yet !");
					return JNI_FALSE;
				} else {
					//if (1)
					//	return JNI_FALSE;
					//printf("BEFORE GET ELLIPSIS\n");
					jobjectArray arr = (jobjectArray)dcbArgPointer(args);
					//printf("AFTER GET ELLIPSIS = %p\n", arr);
					jsize n = (*env)->GetArrayLength(env, arr), i;
					//printf("ELLIPSIS :  %d args\n", (int)n);
					
					for (i = 0; i < n; i++) {
						//printf("BEFORE GET ELEMENT\n");
						jobject arg = (*env)->GetObjectArrayElement(env, arr, i);
						//printf("AFTER GET ELEMENT %p\n", arg);
						#define TEST_INSTANCEOF(cl, st) \
							if ((*env)->IsInstanceOf(env, arg, cl)) st;
					
						/*if ((*env)->IsInstanceOf(env, arg, gIntClass)) {
							printf("BEFORE CallIntMethod\n");
							int i = (*env)->CallIntMethod(env, arg,
								(*env)->GetMethodID(env, gIntClass, "intValue", "()I"));
								//gIntValueMethod);
							//int i = UnboxInt(env, arg);
							printf("GOT INT ARG : %d\n", (int)i);
						}*/
						TEST_INSTANCEOF(gIntClass, dcArgInt(call->vm, UnboxInt(env, arg)))
						else
						TEST_INSTANCEOF(gLongClass, dcArgLongLong(call->vm, UnboxLong(env, arg)))
						else
						TEST_INSTANCEOF(gShortClass, dcArgShort(call->vm, UnboxShort(env, arg)))
						else
						TEST_INSTANCEOF(gByteClass, dcArgChar(call->vm, UnboxByte(env, arg)))
						else
						TEST_INSTANCEOF(gBooleanClass, dcArgChar(call->vm, (char)UnboxBoolean(env, arg)))
						else
						TEST_INSTANCEOF(gCharClass, dcArgShort(call->vm, (short)UnboxChar(env, arg)))
						else
						TEST_INSTANCEOF(gDoubleClass, dcArgDouble(call->vm, UnboxDouble(env, arg)))
						else
						TEST_INSTANCEOF(gFloatClass, dcArgFloat(call->vm, UnboxFloat(env, arg)))
						else
						TEST_INSTANCEOF(gCLongClass, dcArgLong(call->vm, (long)UnboxCLong(env, arg)))
						else
						TEST_INSTANCEOF(gSizeTClass, sizeof(size_t) == 4 ? dcArgInt(call->vm, (int)UnboxSizeT(env, arg)) : dcArgLongLong(call->vm, UnboxSizeT(env, arg)))
						else
						TEST_INSTANCEOF(gPointerClass, dcArgPointer(call->vm, getPointerPeer(env, (void*)arg)))
						else {
							throwException(env, "Invalid value type in ellipsis");
							return JNI_FALSE;
						}
					}
				}
				break;
			}
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
		CALL_CASE(eLongValue, LongLong, LONGLONG, l)
		CALL_CASE(eShortValue, Short, SHORT, s)
		CALL_CASE(eFloatValue, Float, FLOAT, f)
		CALL_CASE(eDoubleValue, Double, DOUBLE, d)
		case eBooleanValue:
		CALL_CASE(eByteValue, Char, CHAR, c)
		case eCLongValue:
			result->L = (jlong)dcCallLong(call->vm, callback);
			break;
		case eSizeTValue:
			result->L = (size_t)dcCallPointer(call->vm, callback);
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
					jobject callIO = call && call->pCallIOs ? *(call->pCallIOs++) : NULL;
					//printf("RETURNED POINTER = %d\n", ptr);
					result->p = createPointerFromIO(env, ptr, callIO);
				}
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
	
