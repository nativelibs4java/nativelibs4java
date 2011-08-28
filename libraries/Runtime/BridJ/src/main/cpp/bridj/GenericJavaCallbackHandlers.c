#include "HandlersCommon.h"
#include <assert.h>

extern jclass gObjectClass;
extern jclass gCLongClass;
extern jclass gSizeTClass;

jboolean followArgsGenericJavaCallback(CallTempStruct* call, DCArgs* args, int nTypes, ValueType* pTypes) 
{	
	JNIEnv* env = call->env;
	int iParam;
	jobjectArray arr = (*env)->NewObjectArray(env, nTypes, gObjectClass, NULL);
	
	for (iParam = 0; iParam < nTypes; iParam++) {
		ValueType type = pTypes[iParam];
		jobject arg = NULL;
		switch (type) {
			case eIntFlagSet:
				arg = BoxInt(env, (jint)getFlagValue(env, (jobject)dcbArgPointer(args)));
				break;
			case eIntValue:
				arg = BoxInt(env, dcbArgInt(args));
				break;
			case eTimeTObjectValue:
			case eSizeTObjectValue:
			case eCLongObjectValue:
				arg = dcbArgPointer(args);
				break;
			case eCLongValue: {
				jlong v;
				if (sizeof(long) == 4)
					v = dcbArgLong(args);
				else
					v = dcbArgLongLong(args);
				arg = BoxCLong(env, v);
				break;
			}
			case eSizeTValue: {
				jlong v;
				if (sizeof(size_t) == 4)
					v = dcbArgInt(args);
				else
					v = dcbArgLongLong(args);
				arg = BoxSizeT(env, v);
				break;
			}
			case eLongValue:
				arg = BoxLong(env, dcbArgLongLong(args));
				break;
			case eShortValue:
				arg = BoxShort(env, dcbArgShort(args));
				break;
			case eBooleanValue:
			case eByteValue:
				arg = BoxByte(env, dcbArgChar(args));
				break;
			case eFloatValue:
				arg = BoxFloat(env, dcbArgFloat(args));
				break;
			case eDoubleValue:
				arg = BoxDouble(env, dcbArgDouble(args));
				break;
			case ePointerValue:
				{
					jobject callIO = call && call->pCallIOs ? *(call->pCallIOs++) : NULL;
					void* ptr = dcbArgPointer(args);
					arg = createPointerFromIO(env, ptr, callIO);
				}
				break;
			case eWCharValue:
				switch (sizeof(wchar_t)) {
				case 1:
					arg = BoxChar(env, dcbArgChar(args));
					break;
				case 2:
					arg = BoxChar(env, dcbArgShort(args));
					break;
				case 4:
					arg = BoxInt(env, dcbArgInt(args));
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
		(*env)->SetObjectArrayElement(env, arr, iParam, arg);
	}
	dcArgPointer(call->vm, arr);
	
	if ((*env)->ExceptionCheck(env))
		return JNI_FALSE;
	return JNI_TRUE;
}

jboolean followCallGenericJavaCallback(CallTempStruct* call, ValueType returnType, DCValue* result, void* callback) 
{
	JNIEnv* env = call->env;
	jobject ret = dcCallPointer(call->vm, callback);
	if ((*env)->ExceptionCheck(env))
		return JNI_FALSE;
	
	switch (returnType) {
		case eIntValue:
			result->i = UnboxInt(env, ret);
			break;
		case eLongValue:
			result->l = UnboxLong(env, ret);
			break;
		case eShortValue:
			result->s = UnboxShort(env, ret);
			break;
		case eByteValue:
			result->c = UnboxByte(env, ret);
			break;
		case eFloatValue:
			result->f = UnboxFloat(env, ret);
			break;
		case eDoubleValue:
			result->d = UnboxDouble(env, ret);
			break;
		case eBooleanValue:
			result->c = UnboxBoolean(env, ret);
			break;
		case eCLongValue: {
			jlong v;
			if ((*env)->IsInstanceOf(env, ret, gCLongClass))
				v = UnboxCLong(env, ret);
			else
				v = UnboxLong(env, ret);
			if (sizeof(long) == 4)
				result->i = (int)v;
			else
				result->l = v;
			break;
		}
		case eCLongObjectValue: {
			jobject v;
			if ((*env)->IsInstanceOf(env, ret, gCLongClass))
				v = ret;
			else
				v = BoxCLong(env, UnboxLong(env, ret));
			result->p = v;
			break;
		}
		case eSizeTValue: {
			jlong v;
			if ((*env)->IsInstanceOf(env, ret, gSizeTClass))
				v = UnboxSizeT(env, ret);
			else
				v = UnboxLong(env, ret);
			if (sizeof(size_t) == 4)
				result->i = (int)v;
			else
				result->L = v;
			break;
		}
		case eSizeTObjectValue: {
			jobject v;
			if ((*env)->IsInstanceOf(env, ret, gSizeTClass))
				v = ret;
			else
				v = BoxSizeT(env, UnboxLong(env, ret));
			result->p = v;
			break;
		}
		case eTimeTObjectValue: {
			jobject v;
			if ((*env)->IsInstanceOf(env, ret, gTimeTClass))
				v = ret;
			else
				v = BoxTimeT(env, UnboxLong(env, ret));
			result->p = v;
			break;
		}
		case eVoidValue:
			assert(ret == NULL);
			break;
		case eIntFlagSet:
			result->i = (jint)getFlagValue(env, ret);
			break;
		case ePointerValue:
			result->p = ret ? getPointerPeer(env, (void*)ret) : NULL;
			call->pCallIOs++;
			break;
		case eWCharValue:
			switch (sizeof(wchar_t)) {
			case 1:
				result->c = (char)UnboxChar(env, ret);
				break;
			case 2:
				result->s = (short)UnboxChar(env, ret);
				break;
			case 4:
				result->i = UnboxInt(env, ret);
				break;
			default:
				throwException(env, "Invalid wchar_t size !");
				return JNI_FALSE;
			}
			break;
		default:
			throwException(env, "Invalid return value type !");
			return JNI_FALSE;
	}
	return JNI_TRUE;
}
