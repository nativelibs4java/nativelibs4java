#include "jdyncall.hpp"
#include "RawNativeForwardCallback.h"
#include "Options.hpp"
#include <sstream>
#include <iostream>

ValueType GetJavaTypeSignature(JNIEnv *env, jobject type, std::ostringstream* javasig, std::ostringstream* dcsig, const Options& options, bool& bIsAdaptableAsRaw);

using namespace std;

MethodCallInfo::MethodCallInfo(JNIEnv *env, jclass declaringClass, jobject method, void* forwardedSymbol) :
	fCallback(NULL),
	fMethod(NULL),
	fReturnTypeClass(NULL),
	fAddressableReturnFactory(NULL),
	fForwardedSymbol(forwardedSymbol),
	fIsVarArgs(false),
	fIsAdaptableAsRaw(true),
	fIsCPlusPlus(false),
	fEnv(env),
	fDCMode(0)
{
	fMethod = env->NewGlobalRef(method);
	jobjectArray paramsTypes = (jobjectArray)env->CallObjectMethod(method, Method_getParameterTypes);
	jobjectArray paramsAnnotations = (jobjectArray)env->CallObjectMethod(method, Method_getParameterAnnotations);
	jobject returnType = env->CallObjectMethod(method, Method_getReturnType);
	jobject genericReturnType = env->CallObjectMethod(method, Method_getGenericReturnType);
	jint modifiers = env->CallIntMethod(method, Member_getModifiers);
	fIsStatic = (modifiers & Modifier_STATIC) != 0;
	fIsVarArgs = env->CallBooleanMethod(method, Method_isVarArgs) != 0;
	//bool isStatic = true; //TODO

	fReturnTypeClass = (jclass)env->NewGlobalRef(returnType);
	jsize nParams = env->GetArrayLength(paramsTypes);
	fArgTypes.resize(nParams);
	fArgOptions.resize(nParams);

#ifdef _WIN64
	fIsAdaptableAsRaw = nParams <= 4;
#else	
	fIsAdaptableAsRaw = true;
#endif
	fIsCPlusPlus = env->IsAssignableFrom(declaringClass, CPPObject_class) != 0;

	GetOptions(env, fMethodOptions, method);
	
	//TODO
	/*fDCMode = fIsCPlusPlus ?
		fIsStatic */
	
	if (env->IsAssignableFrom((jclass)returnType, Pointable_class)) {
		fAddressableReturnFactory = env->NewGlobalRef(env->CallStaticObjectMethod(DynCall_class, DynCall_newAddressableFactory, returnType, genericReturnType));
	}

	ostringstream jsig, dcsig;
	jsig << '(';
	dcsig << DC_SIGCHAR_POINTER << DC_SIGCHAR_POINTER; // JNIEnv*, jobject: always present in native-bound functions

	for (jsize iParam = 0; iParam < nParams; iParam++) {
		Options& paramOptions = fArgOptions[iParam];
		GetOptions(env, paramOptions, method, (jobjectArray)env->GetObjectArrayElement(paramsAnnotations, iParam));
	
		jobject param = env->GetObjectArrayElement(paramsTypes, iParam);
		ValueType argType = GetJavaTypeSignature(env, param, &jsig, &dcsig, paramOptions, fIsAdaptableAsRaw);
		fArgTypes[iParam] = argType;
		
		//if (argType == e
	}
	jsig << ')';
	dcsig << ')';
	fReturnType = GetJavaTypeSignature(env, returnType, &jsig, &dcsig, fMethodOptions, fIsAdaptableAsRaw);

	fJavaSignature = jsig.str();
	fDCSignature = dcsig.str();
}
MethodCallInfo::~MethodCallInfo()
{

}

const std::string& MethodCallInfo::GetDCSignature() {
	return fDCSignature;
}
const std::string& MethodCallInfo::GetJavaSignature() {
	return fJavaSignature;
}

void test() {
	DCCallback* pcb = (DCCallback*)1;
	DCArgs* args = (DCArgs*)2;
	DCValue* result = (DCValue*)3; 
	void* userdata = (void*)4;
	char r = JavaToNativeCallHandler(pcb, args, result, userdata);
	cerr << r;
}

void* MethodCallInfo::GetCallback()
{
	//test();
	if (!fCallback) {
		void* userdata = this;
		static bool allowRawAdapters = false;//!getenv("NO_RAW_FWD");
		if (allowRawAdapters && fIsAdaptableAsRaw) {
			fCallback = (DCCallback*)dcRawCallAdapterSkipTwoArgs((void (*)())fForwardedSymbol);
		} 
		else
		if (!fCallback)
		{
			fCallback = dcNewCallback(GetDCSignature().c_str(), JavaToNativeCallHandler, userdata);
		}
		//cbToUserData.push_back(make_pair((ptrdiff_t)fCallback, (ptrdiff_t)userdata));
		//cbToUserData[(ptrdiff_t)cb] = (ptrdiff_t)userdata;
	}
	return fCallback;
}


