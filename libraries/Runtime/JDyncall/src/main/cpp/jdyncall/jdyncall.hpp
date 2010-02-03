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
#include "Exceptions.h"

#ifndef _WIN32
#define __cdecl
#endif

#ifdef _MSC_VER
#	define THREAD_STATIC __declspec(thread) static
#else 
#	define THREAD_STATIC 
//TODO http://www.opengroup.org/onlinepubs/009695399/functions/pthread_key_create.html
//static __thread
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
	eWCharValue,
	eCLongValue,
	eSizeTValue,
	eIntValue,
	eShortValue,
	eByteValue,
	eLongValue,
	eDoubleValue,
	eFloatValue
};

struct MethodCallInfo {
	void* fCallback;
	void* fForwardedSymbol;
	ValueType fReturnType;
	ValueType* fParamTypes;
	jint nParams;
	int fDCMode;
};

char __cdecl JavaToNativeCallHandler(DCCallback* pcb, DCArgs* args, DCValue* result, void* userdata);
//jlong BindCallback(jobject obj);

#endif

