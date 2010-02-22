#ifndef BRIDJ_H
#define BRIDJ_H

#ifndef BRIDJ_API
#define BRIDJ_API
#endif

#pragma warning(error: 4061)
#pragma warning(disable: 4127)
#pragma warning(disable: 4100) // unreferenced formal parameter
#pragma warning(disable: 4706) // assignment inside a conditional expression

#ifndef _WIN32
#define __cdecl
#endif

//#include "jni.h"
#include "dyncallback/dyncall_callback.h"
#include <jni.h>
//#include "Exceptions.h"

#ifdef _MSC_VER
#	define THREAD_STATIC __declspec(thread) static
#else 
#	define THREAD_STATIC static 
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

typedef enum ValueType {
	eVoidValue = 0,
	eWCharValue,
	eCLongValue,
	eSizeTValue,
	eIntValue,
	eShortValue,
	eByteValue,
	eLongValue,
	eDoubleValue,
	eFloatValue,
	ePointerValue
} ValueType;

typedef struct CommonCallbackInfo {
	int nParams;
	char fDCReturnType;
	enum ValueType fReturnType;
	enum ValueType* fParamTypes;
	int fDCMode;
	void* fDCCallback;
	JNIEnv* fEnv;
} CommonCallbackInfo;

typedef struct VirtualMethodCallInfo {
	struct CommonCallbackInfo fInfo;
	jclass fClass;
	jboolean fHasThisPtrArg;
	int fVirtualIndex;
	int fVirtualTableOffset;
} VirtualMethodCallInfo;

typedef struct FunctionCallInfo {
	struct CommonCallbackInfo fInfo;
	void* fForwardedSymbol;
} FunctionCallInfo;

typedef struct NativeToJavaCallbackCallInfo {
	struct CommonCallbackInfo fInfo;
	void* fJNICallFunction;
	jobject fCallbackInstance;
	jmethodID fMethod;
} NativeToJavaCallbackCallInfo;

typedef struct JavaToNativeCallbackCallInfo {
	struct CommonCallbackInfo fInfo;
} JavaToNativeCallbackCallInfo;

char __cdecl JavaToFunctionCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata);
char __cdecl JavaToVirtualMethodCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata);
char __cdecl NativeToJavaCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata);

void* getNativeObjectPointer(JNIEnv *env, jobject instance, jclass targetClass);
void* getPointerPeer(JNIEnv *env, jobject pointer);
jobject createPointer(JNIEnv *env, void* ptr, jclass targetType);

void callDefaultConstructor(void* constructor, void* thisPtr, int callMode);

void throwException(JNIEnv* env, const char* message);
jboolean assertThrow(JNIEnv* env, jboolean value, const char* message);

#endif

