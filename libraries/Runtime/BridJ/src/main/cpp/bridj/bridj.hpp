#ifndef BRIDJ_H
#define BRIDJ_H

#ifndef BRIDJ_API
#define BRIDJ_API
#endif

#pragma warning(error: 4061)
#pragma warning(disable: 4127)
#pragma warning(disable: 4100) // unreferenced formal parameter
#pragma warning(disable: 4706) // assignment inside a conditional expression
#pragma warning(disable: 4054) // casting a function pointer to a data pointer

#ifndef _WIN32
#define __cdecl
#endif

//#ifdef _WIN64
#define NO_DIRECT_CALLS // TODO REMOVE ME !!! (issues with stack alignment on COM calls ?)
//#endif

#include "dyncallback/dyncall_callback.h"
#include <jni.h>


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
	eBooleanValue,
	eLongValue,
	eDoubleValue,
	eFloatValue,
	ePointerValue,
	eEllipsis,
	eFlagSet,
	eNativeObjectValue
} ValueType;

typedef struct CommonCallbackInfo {
	int nParams;
	char fDCReturnType;
	enum ValueType fReturnType;
	enum ValueType* fParamTypes;
	int fDCMode;
	void* fDCCallback;
	JNIEnv* fEnv;
#ifdef _DEBUG
	char* fSymbolName;
#endif
} CommonCallbackInfo;

typedef struct CPPMethodCallInfo {
	struct CommonCallbackInfo fInfo;
	jclass fClass;
	void* fForwardedSymbol;
} CPPMethodCallInfo;

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

#if defined (DC__OS_Darwin)
#include <objc/objc.h>

typedef struct JavaToObjCCallInfo {
	struct CommonCallbackInfo fInfo;
	SEL fSelector;
} JavaToObjCCallInfo;

char __cdecl JavaToObjCCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata);
#endif

typedef struct StructFieldInfo {
	struct CommonCallbackInfo fInfo;
	jmethodID fMethod;
	void* fJNICallFunction;
	jint fFieldIndex;
} StructFieldInfo;


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
char __cdecl JavaToCPPMethodCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata);
char __cdecl JavaToVirtualMethodCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata);
char __cdecl JavaToNativeCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata);
char __cdecl NativeToJavaCallHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata);
char __cdecl StructHandler(DCCallback* callback, DCArgs* args, DCValue* result, void* userdata);


void* getNativeObjectPointer(JNIEnv *env, jobject instance, jclass targetClass);
void* getPointerPeer(JNIEnv *env, jobject pointer);
jobject createPointer(JNIEnv *env, void* ptr, jclass targetType);

void callDefaultConstructor(void* constructor, void* thisPtr, int callMode);

void throwException(JNIEnv* env, const char* message);
jboolean assertThrow(JNIEnv* env, jboolean value, const char* message);

#endif

