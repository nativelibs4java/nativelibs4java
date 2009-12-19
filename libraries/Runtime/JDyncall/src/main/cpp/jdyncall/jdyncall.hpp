#ifndef JDYNCALL_H
#define JDYNCALL_H

#ifndef JDYNCALL_API
#define JDYNCALL_API
#endif

#pragma warning(error: 4061)
#pragma warning(disable: 4127)
#pragma warning(disable: 4706) // assignment inside a conditional expression

#include "jni.h"
#include "dyncallback/dyncall_callback.h"
#include "JNIUtils.hpp"

#include <vector>
#include <map>
#include <string>
#include <iostream>
#include <sstream>

#include "CommonClassesAndMethods.h"


#ifdef _MSC_VER
#	define THREAD_STATIC __declspec(thread) static
#else 
#	define THREAD_STATIC static __thread
#endif

#define MAX(x, y) (x < y ? y : x)

#define Modifier_ABSTRACT	1024
#define Modifier_FINAL	16
#define Modifier_INTERFACE	512
#define Modifier_NATIVE	256
#define Modifier_PRIVATE	2
#define Modifier_PROTECTED	4
#define Modifier_PUBLIC	1
#define Modifier_STATIC	8
#define Modifier_STRICT	2048
#define Modifier_SYNCHRONIZED	32
#define Modifier_TRANSIENT	128
#define Modifier_VOLATILE	64

enum ValueType {
	eVoidValue = 0,
	eArrayValue,
	eWCharValue,
	eCallbackValue,
	eAddressableValue,
	eLongPtrValue,
	eCLongValue,
	eSizeTValue,
	eBufferValue,
	eIntValue,
	eShortValue,
	eByteValue,
	eLongValue,
	eDoubleValue,
	eFloatValue,
	eIntArrayValue,
	eShortArrayValue,
	eByteArrayValue,
	eLongArrayValue,
	eDoubleArrayValue,
	eFloatArrayValue,
	eBooleanArrayValue,
	eCharArrayValue
};

struct Options {
	bool bIsWideChar: 1;
	bool bIsConst: 1;
	bool bIsPointer: 1;
	bool bIsVirtual: 1;
	bool bIsByValue: 1;
	bool bIsSizeT: 1;
	bool bIsCLong: 1;
	int index;

	Options() {
		memset(this, 0, sizeof(Options));
		index= -1;
	}
};

struct FieldOptions : public Options {
	char bits;
	int arraySize;

	FieldOptions() : Options() {
		bits = -1;
		arraySize = -1;
	}
};

ValueType GetValueType(JNIEnv *env, jobject obj, ValueType *typeConstraint);
ValueType GetValueType(JNIEnv *env, jclass c);

struct MethodCallInfo {
	jobject fMethod;
	jclass fReturnTypeClass;
	jobject fAddressableReturnFactory;
	JNIEnv* fEnv;
	std::string fDCSignature;
	std::string fJavaSignature;
	std::vector<ValueType> fArgTypes;
	std::vector<Options> fArgOptions;
	Options fMethodOptions;
	ValueType fReturnType;
	bool fIsVarArgs: 1;
	bool fIsStatic: 1;
	bool fIsCPlusPlus: 1;
	bool fIsAdaptableAsRaw;
	DCCallback* fCallback;
	void* fForwardedSymbol;
	int fDCMode;
	
	MethodCallInfo(JNIEnv *env, jclass declaringClass, jobject method, void* forwardedSymbol);
	~MethodCallInfo();

	const std::string& GetDCSignature();
	const std::string& GetJavaSignature();
	void* GetCallback();
};


ValueType GetJavaTypeSignature(JNIEnv *env, jobject type, std::ostringstream* javasig, std::ostringstream* dcsig, const Options& options, bool& bIsAdaptableAsRaw);

char __cdecl JavaToNativeCallHandler(DCCallback* pcb, DCArgs* args, DCValue* result, void* userdata);
jlong BindCallback(jobject obj);

#endif

