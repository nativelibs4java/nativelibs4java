#include "jdyncall.hpp"
#include <sstream>
#include <iostream>

using namespace std;

ValueType GetJavaTypeSignature(JNIEnv *env, jobject type, ostringstream* javasig, ostringstream* dcsig, const Options& options, bool& bIsAdaptableAsRaw) {
	//return eVoidValue;
	///*

	if (!type || env->IsSameObject(type, Void_TYPE)) {
		if (dcsig)
			(*dcsig) << (char)DC_SIGCHAR_VOID;
		if (javasig)
			(*javasig) << 'V';
		return eVoidValue;
	}
	if (env->CallBooleanMethod(type, Class_isArray))
	{
		bIsAdaptableAsRaw = false;
		if (dcsig)
			(*dcsig) << (char)DC_SIGCHAR_POINTER;
		if (javasig)
			(*javasig) << '[';
		ValueType componentType = GetJavaTypeSignature(env, env->CallObjectMethod(type, Class_getComponentType), javasig, NULL, options, bIsAdaptableAsRaw);
		switch (componentType) {
			case eIntValue:
				return eIntArrayValue;
			case eLongValue:
				return eLongArrayValue;
			case eShortValue:
				return eShortArrayValue;
			case eByteValue:
				return eByteArrayValue;
			case eDoubleValue:
				return eDoubleArrayValue;
			//case eCharValue:
			//	return eCharArrayValue;
			case eFloatValue:
				return eFloatArrayValue;
			case eAddressableValue:
			case eCallbackValue:
				return eArrayValue;
			case eSizeTValue:
				//TODO
			case eCLongValue:
				//TODO
			case eWCharValue:
				//TODO
			case eLongPtrValue:
				//TODO
			case eBufferValue:
				//TODO
			case eCharArrayValue:
			case eBooleanArrayValue:
			case eFloatArrayValue:
			case eDoubleArrayValue:
			case eShortArrayValue:
			case eIntArrayValue:
			case eByteArrayValue:
			case eLongArrayValue:
				//TODO ?
			case eArrayValue:
			case eVoidValue:
			default:
				cerr << "Unhandled array component type in GetJavaTypeSignature: " << componentType << " !\n";
		}
		
	}
	ValueType typeOut;
	
	if (env->CallBooleanMethod(type, Class_isPrimitive)) {
		if (env->IsSameObject(type, Integer_TYPE)) {
			if (dcsig)
				(*dcsig) << (char)DC_SIGCHAR_INT;
			if (javasig)
				(*javasig) << 'I';
			typeOut = eIntValue;
		} else if (env->IsSameObject(type, Long_TYPE)) {
			if (javasig)
				(*javasig) << 'J';
			if (options.bIsPointer) {
				if (sizeof(void*) != sizeof(jlong))
					bIsAdaptableAsRaw = false;
				if (dcsig)
					(*dcsig) << (char)DC_SIGCHAR_POINTER;
				typeOut = eLongPtrValue;
			} else if (options.bIsCLong) {
				if (sizeof(long) != sizeof(jlong))
					bIsAdaptableAsRaw = false;
				if (dcsig)
					(*dcsig) << (char)DC_SIGCHAR_LONG;
				typeOut = eCLongValue;
			} else if (options.bIsSizeT) {
				if (sizeof(size_t) != sizeof(jlong))
					bIsAdaptableAsRaw = false;
				if (dcsig)
					(*dcsig) << (char)(sizeof(size_t) == 4 ? DC_SIGCHAR_INT : DC_SIGCHAR_LONGLONG);
				typeOut = eSizeTValue;
			} else {
				if (dcsig)
					(*dcsig) << (char)DC_SIGCHAR_LONGLONG;
				typeOut = eLongValue;
			}
		} else if (env->IsSameObject(type, Short_TYPE)) {
			if (dcsig)
					(*dcsig) << (char)DC_SIGCHAR_SHORT;
			if (javasig)
				(*javasig) << 'S';
			typeOut = eShortValue;
		} else if (env->IsSameObject(type, Double_TYPE)) {
			if (dcsig)
				(*dcsig) << (char)DC_SIGCHAR_DOUBLE;
			if (javasig)
				(*javasig) << 'D';
			typeOut = eDoubleValue;
		} else if (env->IsSameObject(type, Float_TYPE)) {
			if (dcsig)
				(*dcsig) << (char)DC_SIGCHAR_FLOAT;
			if (javasig)
				(*javasig) << 'F';
			typeOut = eFloatValue;
		} else if (env->IsSameObject(type, Boolean_TYPE)) {
			if (dcsig)
				(*dcsig) << (char)DC_SIGCHAR_CHAR;
			if (javasig)
				(*javasig) << 'Z';
			typeOut = eByteValue;
		} else if (env->IsSameObject(type, Byte_TYPE)) {
			if (dcsig)
				(*dcsig) << (char)DC_SIGCHAR_CHAR;
			if (javasig)
				(*javasig) << 'B';
			typeOut = eByteValue;
		} else if (env->IsSameObject(type, Character_TYPE)) {
			bIsAdaptableAsRaw = false;
			if (options.bIsWideChar) {
				switch (sizeof(wchar_t)) {
					case 1:
						if (dcsig)
							(*dcsig) << (char)DC_SIGCHAR_CHAR;
						break;
					case 2:
						if (dcsig)
							(*dcsig) << (char)DC_SIGCHAR_SHORT;
						break;
					case 4:
						if (dcsig)
							(*dcsig) << (char)DC_SIGCHAR_INT;
						break;
					default:
						cerr << "Unhandled sizeof(wchar_t) in GetJavaTypeSignature: " << sizeof(wchar_t) << " !\n";
				}
				typeOut = eWCharValue;
			} else {
				if (dcsig)
					(*dcsig) << (char)DC_SIGCHAR_CHAR;
				typeOut = eByteValue;
			}
			if (javasig)
				(*javasig) << 'C';
		} else {
			bIsAdaptableAsRaw = false;
			const JString name(env, (jstring)env->CallObjectMethod(type, Class_getName));
			typeOut = eVoidValue;
			cerr << "Unhandled primitive type in GetJavaTypeSignature : " << (const char*)name << " !\n";
		}
	} else {
		bIsAdaptableAsRaw = false;
		const JString name(env, (jstring)env->CallObjectMethod(type, Class_getName));
		if (dcsig)
			(*dcsig) << (char)DC_SIGCHAR_POINTER;
		if (javasig)
			(*javasig) << 'L';
		for (size_t i = 0, len = name.length(); i < len; i++) {
			char c = name[i];
			if (c == '.')
				c = '/';
			if (javasig)
				(*javasig) << c;
		}
		if (javasig)
			(*javasig) << ';';
		typeOut = eAddressableValue;
	}
	return typeOut;//*/
}