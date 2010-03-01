#include "com_bridj_JNI.h"

#include "dyncallback/dyncall_callback.h"
#include "dynload/dynload.h"
#include "RawNativeForwardCallback.h"

#include "bridj.hpp"
#include <string.h>
#include "Exceptions.h"

#pragma warning(disable: 4152)
#pragma warning(disable: 4189) // local variable initialized but unreferenced // TODO remove this !

#define JNI_SIZEOF(type, escType) \
jint JNICALL Java_com_bridj_JNI_sizeOf_1 ## escType(JNIEnv *env, jclass clazz) { return sizeof(type); }

#define JNI_SIZEOF_t(type) JNI_SIZEOF(type ## _t, type ## _1t)

JNI_SIZEOF_t(size)
JNI_SIZEOF_t(wchar)
JNI_SIZEOF_t(ptrdiff)
JNI_SIZEOF(long, long)

jclass gStructFieldsIOClass = NULL;
jclass gPointerClass = NULL;
jmethodID gAddressMethod = NULL;
jmethodID gGetPeerMethod = NULL;
jmethodID gCreatePeerMethod = NULL;

jclass 		gMethodCallInfoClass 		 = NULL;
jfieldID 	gFieldId_javaSignature 		 = NULL;
jfieldID 	gFieldId_dcSignature 		 = NULL;    
jfieldID 	gFieldId_paramsValueTypes 	 = NULL;
jfieldID 	gFieldId_returnValueType 	 = NULL;    
jfieldID 	gFieldId_forwardedPointer 	 = NULL;
jfieldID 	gFieldId_virtualIndex 		 = NULL;
jfieldID 	gFieldId_virtualTableOffset	 = NULL;
jfieldID 	gFieldId_javaCallback 		 = NULL;
jfieldID 	gFieldId_direct		 		 = NULL;
jfieldID 	gFieldId_startsWithThis 	 = NULL;
jfieldID 	gFieldId_dcCallingConvention = NULL;
jfieldID 	gFieldId_symbolName			 = NULL;
jfieldID 	gFieldId_methodName			 = NULL;
jfieldID 	gFieldId_declaringClass		 = NULL;

int main() {}

void initMethods(JNIEnv* env) {
	if (!gAddressMethod)
	{
		gStructFieldsIOClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "com/bridj/StructFieldsIO"));
		gPointerClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "com/bridj/Pointer"));
		gAddressMethod = (*env)->GetStaticMethodID(env, gPointerClass, "getAddress", "(Lcom/bridj/NativeObject;Ljava/lang/Class;)J");
		gGetPeerMethod = (*env)->GetMethodID(env, gPointerClass, "getPeer", "()J");
		gCreatePeerMethod = (*env)->GetStaticMethodID(env, gPointerClass, "pointerToAddress", "(JLjava/lang/Class;)Lcom/bridj/Pointer;");
		
		gMethodCallInfoClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "com/bridj/MethodCallInfo"));
		
#define GETFIELD_ID(out, name, sig) \
		if (!(gFieldId_ ## out = (*env)->GetFieldID(env, gMethodCallInfoClass, name, sig))) \
			throwException(env, "Failed to get field " #name " in MethodCallInfo !");
		
	
		GETFIELD_ID(javaSignature 		,	"javaSignature"			,	"Ljava/lang/String;"	);
		GETFIELD_ID(dcSignature 		,	"dcSignature" 			,	"Ljava/lang/String;"	);
		GETFIELD_ID(symbolName 		,	"symbolName" 			,	"Ljava/lang/String;"	);
		GETFIELD_ID(methodName 		,	"methodName" 			,	"Ljava/lang/String;"	);
		GETFIELD_ID(declaringClass		,	"declaringClass" 		,	"Ljava/lang/Class;"		);
		GETFIELD_ID(paramsValueTypes 	,	"paramsValueTypes"		,	"[I"					);
		GETFIELD_ID(returnValueType 	,	"returnValueType" 		,	"I"						);
		GETFIELD_ID(forwardedPointer 	,	"forwardedPointer" 		,	"J"						);
		GETFIELD_ID(virtualIndex 		,	"virtualIndex" 			,	"I"						);
		GETFIELD_ID(virtualTableOffset	,	"virtualTableOffset"	,	"I"						);
		GETFIELD_ID(javaCallback 		,	"javaCallback" 			,	"Lcom/bridj/Callback;"	);
		GETFIELD_ID(direct		 		,	"direct"	 			,	"Z"						);
		GETFIELD_ID(startsWithThis		,	"startsWithThis"		,	"Z"						);
		GETFIELD_ID(dcCallingConvention,	"dcCallingConvention"	,	"I"						);
		
	}
}

//void main() {}

jobject createPointer(JNIEnv *env, void* ptr, jclass targetType) {
	initMethods(env);
	return (*env)->CallStaticObjectMethod(env, gPointerClass, gCreatePeerMethod, (jlong)(size_t)ptr, targetType);
}

void* getPointerPeer(JNIEnv *env, jobject pointer) {
	initMethods(env);
	return (void*)(size_t)(*env)->CallLongMethod(env, pointer, gGetPeerMethod, pointer);
}

void* getNativeObjectPointer(JNIEnv *env, jobject instance, jclass targetClass) {
	initMethods(env);
	return (void*)(size_t)(*env)->CallStaticLongMethod(env, gPointerClass, gAddressMethod, instance, targetClass);
}
//void _DllMainCRTStartup();

/*
#include <dlfcn.h>

void TESTOBJC() {
	dlopen("/System/Library/Frameworks/CoreFoundation.framework/CoreFoundation", RTLD_LAZY);
	dlopen("/System/Library/Frameworks/Foundation.framework/Foundation", RTLD_LAZY);
	dlopen("/System/Library/Frameworks/Cocoa.framework/Cocoa", RTLD_LAZY);
	{
		id clsPool = objc_getClass("NSAutoreleasePool");
		if (!clsPool) {
			printf("#\n# INIT NO clsPool : %ld\n#\n", (long int)clsPool);
		} else {
			//clsPool = class_createInstance((Class)clsPool, 0);
			id poolInst = objc_msgSend(clsPool, sel_registerName("new"));
			printf("#\n# INIT poolInst : %ld\n#\n", (long int)poolInst);
		}
	}
	
	
}*/

void JNICALL Java_com_bridj_JNI_init(JNIEnv *env, jclass clazz)
{
	//TESTOBJC();
}

void JNICALL Java_com_bridj_JNI_callDefaultCPPConstructor(JNIEnv *env, jclass clazz, jlong constructor, jlong thisPtr, jint callMode)
{
	callDefaultConstructor((void*)(size_t)constructor, (void*)(size_t)thisPtr, callMode);
}

jlong JNICALL Java_com_bridj_JNI_getDirectBufferAddress(JNIEnv *env, jobject jthis, jobject buffer) {
	BEGIN_TRY();
	return !buffer ? 0 : (jlong)(size_t)(*env)->GetDirectBufferAddress(env, buffer);
	END_TRY_RET(env, 0);
}
jlong JNICALL Java_com_bridj_JNI_getDirectBufferCapacity(JNIEnv *env, jobject jthis, jobject buffer) {
	BEGIN_TRY();
	return !buffer ? 0 : (*env)->GetDirectBufferCapacity(env, buffer);
	END_TRY_RET(env, 0);
}

jlong JNICALL Java_com_bridj_JNI_getObjectPointer(JNIEnv *env, jclass clazz, jobject object)
{
	return (jlong)(size_t)object;
}
 
jlong JNICALL Java_com_bridj_JNI_loadLibrary(JNIEnv *env, jclass clazz, jstring pathStr)
{
	const char* path = (*env)->GetStringUTFChars(env, pathStr, NULL);
	jlong ret = (jlong)(size_t)dlLoadLibrary(path);
	(*env)->ReleaseStringUTFChars(env, pathStr, path);
	return ret;
}

void JNICALL Java_com_bridj_JNI_freeLibrary(JNIEnv *env, jclass clazz, jlong libHandle)
{
	dlFreeLibrary((DLLib*)(size_t)libHandle);
}

jlong JNICALL Java_com_bridj_JNI_loadLibrarySymbols(JNIEnv *env, jclass clazz, jlong libHandle)
{
	return (jlong)(size_t)dlSymsInit((DLLib*)libHandle);
}
void JNICALL Java_com_bridj_JNI_freeLibrarySymbols(JNIEnv *env, jclass clazz, jlong symbolsHandle)
{
	DLSyms* pSyms = (DLSyms*)symbolsHandle;
	dlSymsCleanup(pSyms);
	free(pSyms);
}

jarray JNICALL Java_com_bridj_JNI_getLibrarySymbols(JNIEnv *env, jclass clazz, jlong libHandle, jlong symbolsHandle)
{
	jclass stringClass;
    jarray ret;
    DLSyms* pSyms = (DLSyms*)symbolsHandle;
	int count, i;
	count = dlSymsCount(pSyms);
	
	stringClass = (*env)->FindClass(env, "java/lang/String");
	ret = (*env)->NewObjectArray(env, count, stringClass, 0);
    for (i = 0; i < count; i++) {
		const char* name = dlSymsName(pSyms, i);
		if (!name)
			continue;
		(*env)->SetObjectArrayElement(env, ret, i, (*env)->NewStringUTF(env, name));
    }
    return ret;
}


jstring JNICALL Java_com_bridj_JNI_findSymbolName(JNIEnv *env, jclass clazz, jlong libHandle, jlong symbolsHandle, jlong address)
{
	const char* name = dlSymsNameFromValue((DLSyms*)(size_t)symbolsHandle, (void*)(size_t)address);
	return name ? (*env)->NewStringUTF(env, name) : NULL;
}

jlong JNICALL Java_com_bridj_JNI_findSymbolInLibrary(JNIEnv *env, jclass clazz, jlong libHandle, jstring nameStr)
{
	const char* name;
	jlong ret;
	if (!nameStr)
		return 0;
	
	name = (*env)->GetStringUTFChars(env, nameStr, NULL);
	ret = (jlong)(size_t)dlFindSymbol((DLLib*)(size_t)libHandle, name);
	(*env)->ReleaseStringUTFChars(env, nameStr, name);
	return ret;
}

jobject JNICALL Java_com_bridj_JNI_newDirectByteBuffer(JNIEnv *env, jobject jthis, jlong peer, jlong length) {
	BEGIN_TRY();
	return (*env)->NewDirectByteBuffer(env, (void*)peer, length);
	END_TRY_RET(env, NULL);
}

JNIEXPORT jint JNICALL Java_com_bridj_JNI_getMaxDirectMappingArgCount(JNIEnv *env, jclass clazz) {
#if defined(_WIN64)
	return 4;
#elif defined(DC__OS_Darwin) && defined(DC__Arch_AMD64)
	return 4;
#elif defined(_WIN32)
	return 65000;
#else
	return -1;
#endif
}

/*char __cdecl callInt(JNIEnv *env, jclass clazz, long args, long methodCallInfo) {
{
	DCArgs* args;
	, DCValue* result, MethodCallInfo *info
	JavaToNativeCallHandler(
}*/

char getDCReturnType(ValueType returnType) 
{
	switch (returnType) {
#define CALL_CASE(valueType, capCase, hiCase, uni) \
		case valueType: \
			return DC_SIGCHAR_ ## hiCase;
		CALL_CASE(eIntValue, Int, INT, i)
		CALL_CASE(eLongValue, Long, LONGLONG, l)
		CALL_CASE(eShortValue, Short, SHORT, s)
		CALL_CASE(eFloatValue, Float, FLOAT, f)
		CALL_CASE(eDoubleValue, Double, DOUBLE, d)
		CALL_CASE(eByteValue, Char, CHAR, c)
		case eCLongValue:
			return DC_SIGCHAR_LONG;
		case eSizeTValue:
			return DC_SIGCHAR_LONG;
		case eVoidValue:
			return DC_SIGCHAR_VOID;
		case eWCharValue:
			// TODO
		default:
			//cerr << "Return ValueType not supported yet: " << (int)info->fReturnType << " !\n";
			return DC_SIGCHAR_VOID;
	}
}

void initCommonCallInfo(
	struct CommonCallbackInfo* info,
	JNIEnv *env,
	jint callMode,
	jint nParams,
	jint returnValueType, 
	jintArray paramsValueTypes
) {
	info->fEnv = env;
	info->fDCMode = callMode;
	info->fReturnType = (ValueType)returnValueType;
	info->nParams = nParams;
	if (nParams) {
		info->fParamTypes = (ValueType*)malloc(nParams * sizeof(jint));	
		(*env)->GetIntArrayRegion(env, paramsValueTypes, 0, nParams, (jint*)info->fParamTypes);
	}
	info->fDCReturnType = getDCReturnType(info->fReturnType);
}

void* getJNICallFunction(JNIEnv* env, ValueType valueType) {
	switch (valueType) {
	case eIntValue:
		return (*env)->CallIntMethod;
	case eSizeTValue:
	case eCLongValue:
	case eLongValue:
		return (*env)->CallLongMethod;
	case eFloatValue:
		return (*env)->CallFloatMethod;
	case eDoubleValue:
		return (*env)->CallDoubleMethod;
	case eByteValue:
		return (*env)->CallByteMethod;
	case eShortValue:
		return (*env)->CallShortMethod;
	case eWCharValue:
		return (*env)->CallCharMethod;
	case eVoidValue:
		return (*env)->CallVoidMethod;
	default:
		throwException(env, "Unhandled type in getJNICallFunction !");
		return NULL;
	}
}


void* getJNICallStaticFunction(JNIEnv* env, ValueType valueType) {
	switch (valueType) {
	case eIntValue:
		return (*env)->CallStaticIntMethod;
	case eSizeTValue:
	case eCLongValue:
	case eLongValue:
		return (*env)->CallStaticLongMethod;
	case eFloatValue:
		return (*env)->CallStaticFloatMethod;
	case eDoubleValue:
		return (*env)->CallStaticDoubleMethod;
	case eByteValue:
		return (*env)->CallStaticByteMethod;
	case eShortValue:
		return (*env)->CallStaticShortMethod;
	case eWCharValue:
		return (*env)->CallStaticCharMethod;
	case eVoidValue:
		return (*env)->CallStaticVoidMethod;
	default:
		throwException(env, "Unhandled type in getJNICallStaticFunction !");
		return NULL;
	}
}

#define NEW_STRUCTS(n, type, name) \
	struct type *name = NULL; \
	size_t sizeof ## name = n * sizeof(struct type); \
	name = (struct type*)malloc(sizeof ## name); \
	memset(name, 0, sizeof ## name);

	
void registerJavaFunction(JNIEnv* env, jclass declaringClass, jstring methodName, jstring methodSignature, void (*callback)())
{
	JNINativeMethod meth;
	if (!callback) {
			throwException(env, "No callback !");
			return;
		}
	if (!methodName) {
			throwException(env, "No methodName !");
			return;
		}
	if (!methodSignature) {
			throwException(env, "No methodSignature !");
			return;
		}
	if (!declaringClass) {
			throwException(env, "No declaringClass !");
			return;
		}

	meth.fnPtr = callback;
	meth.name = (char*)(*env)->GetStringUTFChars(env, methodName, NULL);
	meth.signature = (char*)(*env)->GetStringUTFChars(env, methodSignature, NULL);
	(*env)->RegisterNatives(env, declaringClass, &meth, 1);
	
	(*env)->ReleaseStringUTFChars(env, methodName, meth.name);
	(*env)->ReleaseStringUTFChars(env, methodSignature, meth.signature);
}

void freeCommon(CommonCallbackInfo* info)
{
	if (info->nParams)
		free(info->fParamTypes);
	
	dcbFreeCallback((DCCallback*)info->fDCCallback);
}
	                                                                                                                     
#define GET_INFO_FIELDS()                                                                                               	 \
		jstring 	javaSignature 		= (*env)->GetObjectField(	env, methodCallInfo, gFieldId_javaSignature 		);   \
		jstring 	dcSignature 		= (*env)->GetObjectField(	env, methodCallInfo, gFieldId_dcSignature 		    );   \
		jstring 	symbolName	 		= (*env)->GetObjectField(	env, methodCallInfo, gFieldId_symbolName	 	    );   \
		jstring 	methodName	 		= (*env)->GetObjectField(	env, methodCallInfo, gFieldId_methodName	 	    );   \
		jstring    declaringClass= (jclass)(*env)->GetObjectField(	env, methodCallInfo, gFieldId_declaringClass		);   \
		jintArray 	paramsValueTypes 	= (*env)->GetObjectField(	env, methodCallInfo, gFieldId_paramsValueTypes 	    );   \
		jobject 	javaCallback 		= (*env)->GetObjectField(	env, methodCallInfo, gFieldId_javaCallback 		    );   \
		jlong 		forwardedPointer 	= (*env)->GetLongField(		env, methodCallInfo, gFieldId_forwardedPointer 	    );   \
		jint	 	returnValueType 	= (*env)->GetIntField(		env, methodCallInfo, gFieldId_returnValueType 	    );   \
		jint	 	virtualIndex 		= (*env)->GetIntField(		env, methodCallInfo, gFieldId_virtualIndex 		    );   \
		jint	 	virtualTableOffset	= (*env)->GetIntField(		env, methodCallInfo, gFieldId_virtualTableOffset	);   \
		jint	 	dcCallingConvention	= (*env)->GetIntField(		env, methodCallInfo, gFieldId_dcCallingConvention	);   \
		jboolean 	direct		 		= (*env)->GetBooleanField(	env, methodCallInfo, gFieldId_direct		 		);   \
		jboolean 	startsWithThis		= (*env)->GetBooleanField(	env, methodCallInfo, gFieldId_startsWithThis 		);   \
		jsize		nParams				= (*env)->GetArrayLength(	env, paramsValueTypes);
		
#define BEGIN_INFOS_LOOP(type)                                                                                           \
	jsize i, n = (*env)->GetArrayLength(env, methodCallInfos);															 \
	NEW_STRUCTS(n, type, infos);																						 \
	initMethods(env);                                                                                        	 		 \
	for (i = 0; i < n; i++)                                                                                          	 \
	{                  																								 	 \
		type* info = &infos[i];																						 	 \
			jobject methodCallInfo = (*env)->GetObjectArrayElement(env, methodCallInfos, i);                             \
			GET_INFO_FIELDS();

#define END_INFOS_LOOP() }

JNIEXPORT jlong JNICALL Java_com_bridj_JNI_createCToJavaCallback(
	JNIEnv *env, 
	jclass clazz,
	jobject methodCallInfo
) {
	struct NativeToJavaCallbackCallInfo* info = NULL;
	{
		const char* dcSig, *javaSig, *methName;
		GET_INFO_FIELDS();
		{
			info = (struct NativeToJavaCallbackCallInfo*)malloc(sizeof(struct NativeToJavaCallbackCallInfo));
			memset(info, 0, sizeof(struct NativeToJavaCallbackCallInfo));
			
			// TODO DIRECT C++ virtual thunk
			javaSig = (char*)(*env)->GetStringUTFChars(env, javaSignature, NULL);
			dcSig = (*env)->GetStringUTFChars(env, dcSignature, NULL);
			methName = (char*)(*env)->GetStringUTFChars(env, methodName, NULL);

			info->fInfo.fDCCallback = dcbNewCallback(dcSig, NativeToJavaCallHandler, info);
			info->fCallbackInstance = (*env)->NewGlobalRef(env, javaCallback);
			info->fMethod = (*env)->GetMethodID(env, declaringClass, methName, javaSig);
			info->fJNICallFunction = getJNICallFunction(env, (ValueType)returnValueType);

			//info->fInfo.fSymbolName = methName;

			(*env)->ReleaseStringUTFChars(env, javaSignature, javaSig);
			(*env)->ReleaseStringUTFChars(env, methodName, methName);
			(*env)->ReleaseStringUTFChars(env, dcSignature, dcSig);
			
			initCommonCallInfo(&info->fInfo, env, dcCallingConvention, nParams, returnValueType, paramsValueTypes);
		}
	}
	return (jlong)(size_t)info;
}
JNIEXPORT jlong JNICALL Java_com_bridj_JNI_getActualCToJavaCallback(
	JNIEnv *env, 
	jclass clazz,
	jlong handle
) {
	struct NativeToJavaCallbackCallInfo* info = (struct NativeToJavaCallbackCallInfo*)(size_t)handle;
	return (jlong)(size_t)info->fInfo.fDCCallback;
}
JNIEXPORT void JNICALL Java_com_bridj_JNI_freeCToJavaCallback(
	JNIEnv *env, 
	jclass clazz,
	jlong handle
) {
	struct NativeToJavaCallbackCallInfo* info = (struct NativeToJavaCallbackCallInfo*)(size_t)handle;
	(*env)->DeleteGlobalRef(env, info->fCallbackInstance);
	freeCommon(&info->fInfo);
	free(info);
}


JNIEXPORT jlong JNICALL Java_com_bridj_JNI_bindJavaToCCallbacks(
	JNIEnv *env, 
	jclass clazz,
	jobjectArray methodCallInfos
) {
	BEGIN_INFOS_LOOP(JavaToNativeCallbackCallInfo)
	{
		//void* callback;
		const char* dcSig;
		
		// TODO DIRECT C++ virtual thunk
		dcSig = (*env)->GetStringUTFChars(env, dcSignature, NULL);
		info->fInfo.fDCCallback = dcbNewCallback(dcSig, JavaToNativeCallHandler/* NativeToJavaCallHandler*/, info);
		(*env)->ReleaseStringUTFChars(env, dcSignature, dcSig);
			
		initCommonCallInfo(&info->fInfo, env, dcCallingConvention, nParams, returnValueType, paramsValueTypes);
		registerJavaFunction(env, declaringClass, methodName, javaSignature, info->fInfo.fDCCallback);
	}
	END_INFOS_LOOP()
	return (jlong)(size_t)infos;
}
JNIEXPORT void JNICALL Java_com_bridj_JNI_freeJavaToCCallbacks(
	JNIEnv *env, 
	jclass clazz,
	jlong handle,
	jint size
) {
	JavaToNativeCallbackCallInfo* infos = (JavaToNativeCallbackCallInfo*)(size_t)handle;
	jint i;
	if (!infos)
		return;
	for (i = 0; i < size; i++) {
		freeCommon(&infos[i].fInfo);
	}
	free(infos);
}




JNIEXPORT jlong JNICALL Java_com_bridj_JNI_bindGetters(
	JNIEnv *env, 
	jclass clazz,
	jobjectArray methodCallInfos
) {
	BEGIN_INFOS_LOOP(StructFieldInfo)
	{
		const char* ds = (*env)->GetStringUTFChars(env, dcSignature, NULL);
		info->fInfo.fDCCallback = dcbNewCallback(ds, JavaToFunctionCallHandler, info);
		(*env)->ReleaseStringUTFChars(env, dcSignature, ds);
		
		//TODO jmethodID fMethod;
		//TODO void* fJNICallFunction;
		//TODO jint fFieldIndex
	
		initCommonCallInfo(&info->fInfo, env, dcCallingConvention, nParams, returnValueType, paramsValueTypes);
		registerJavaFunction(env, declaringClass, methodName, javaSignature, info->fInfo.fDCCallback);
	}
	END_INFOS_LOOP()
	return (jlong)(size_t)infos;
}
JNIEXPORT void JNICALL Java_com_bridj_JNI_freeGetters(
	JNIEnv *env, 
	jclass clazz,
	jlong handle,
	jint size
) {
	StructFieldInfo* infos = (StructFieldInfo*)(size_t)handle;
	jint i;
	if (!infos)
		return;
	for (i = 0; i < size; i++) {
		freeCommon(&infos[i].fInfo);
	}
	free(infos);
}




JNIEXPORT jlong JNICALL Java_com_bridj_JNI_bindJavaMethodsToCFunctions(
	JNIEnv *env, 
	jclass clazz,
	jobjectArray methodCallInfos
) {
	BEGIN_INFOS_LOOP(FunctionCallInfo)
	{
		info->fForwardedSymbol = (void*)(size_t)forwardedPointer;
#ifndef NO_DIRECT_CALLS
		if (direct && forwardedPointer)
			info->fInfo.fDCCallback = (DCCallback*)dcRawCallAdapterSkipTwoArgs((void (*)())forwardedPointer, dcCallingConvention);
#endif
		
		if (!info->fInfo.fDCCallback) {
			const char* ds = (*env)->GetStringUTFChars(env, dcSignature, NULL);
			info->fInfo.fDCCallback = dcbNewCallback(ds, JavaToFunctionCallHandler, info);
			(*env)->ReleaseStringUTFChars(env, dcSignature, ds);
		}
		initCommonCallInfo(&info->fInfo, env, dcCallingConvention, nParams, returnValueType, paramsValueTypes);
		registerJavaFunction(env, declaringClass, methodName, javaSignature, info->fInfo.fDCCallback);
	}
	END_INFOS_LOOP()
	return (jlong)(size_t)infos;
}
JNIEXPORT void JNICALL Java_com_bridj_JNI_freeCFunctionBindings(
	JNIEnv *env, 
	jclass clazz,
	jlong handle,
	jint size
) {
	FunctionCallInfo* infos = (FunctionCallInfo*)(size_t)handle;
	jint i;
	if (!infos)
		return;
	for (i = 0; i < size; i++) {
		freeCommon(&infos[i].fInfo);
	}
	free(infos);
}

JNIEXPORT jlong JNICALL Java_com_bridj_JNI_bindJavaMethodsToObjCMethods(
	JNIEnv *env, 
	jclass clazz,
	jobjectArray methodCallInfos
) {
#if defined (DC__OS_Darwin)
	BEGIN_INFOS_LOOP(JavaToObjCCallInfo)
	{
		const char* ds, *methName;
	
		// TODO DIRECT ObjC thunk
		methName = (char*)(*env)->GetStringUTFChars(env, symbolName, NULL);
		ds = (*env)->GetStringUTFChars(env, dcSignature, NULL);
		
		info->fInfo.fDCCallback = dcbNewCallback(ds, JavaToObjCCallHandler, info);
		info->fSelector = sel_registerName(methName);
		
		(*env)->ReleaseStringUTFChars(env, dcSignature, ds);
		(*env)->ReleaseStringUTFChars(env, symbolName, methName);
		
		
		initCommonCallInfo(&info->fInfo, env, dcCallingConvention, nParams, returnValueType, paramsValueTypes);
		registerJavaFunction(env, declaringClass, methodName, javaSignature, info->fInfo.fDCCallback);
	}
	END_INFOS_LOOP()
	return (jlong)(size_t)infos;
#else
	return 0;
#endif
}

JNIEXPORT void JNICALL Java_com_bridj_JNI_freeObjCMethodBindings(
	JNIEnv *env, 
	jclass clazz,
	jlong handle,
	jint size
) {
#if defined (DC__OS_Darwin)
	JavaToObjCCallInfo* infos = (JavaToObjCCallInfo*)(size_t)handle;
	jint i;
	if (!infos)
		return;
	for (i = 0; i < size; i++) {
		freeCommon(&infos[i].fInfo);
	}
	free(infos);
#endif
}


JNIEXPORT jlong JNICALL Java_com_bridj_JNI_bindJavaMethodsToVirtualMethods(
	JNIEnv *env, 
	jclass clazz,
	jobjectArray methodCallInfos
) {
	BEGIN_INFOS_LOOP(VirtualMethodCallInfo)
	{
		const char* ds;
	
		info->fClass = (*env)->NewGlobalRef(env, declaringClass);
		info->fHasThisPtrArg = startsWithThis;
		info->fVirtualIndex = virtualIndex;
		info->fVirtualTableOffset = virtualTableOffset;
		//info->fClass = NULL;//TODO declaringClass;
		
		// TODO DIRECT C++ virtual thunk
		ds = (*env)->GetStringUTFChars(env, dcSignature, NULL);
		info->fInfo.fDCCallback = dcbNewCallback(ds, JavaToVirtualMethodCallHandler, info);
		(*env)->ReleaseStringUTFChars(env, dcSignature, ds);
		
		
		initCommonCallInfo(&info->fInfo, env, dcCallingConvention, nParams, returnValueType, paramsValueTypes);
		registerJavaFunction(env, declaringClass, methodName, javaSignature, info->fInfo.fDCCallback);
	}
	END_INFOS_LOOP()
	return (jlong)(size_t)infos;
}
JNIEXPORT void JNICALL Java_com_bridj_JNI_freeVirtualMethodBindings(
	JNIEnv *env, 
	jclass clazz,
	jlong handle,
	jint size
) {
	VirtualMethodCallInfo* infos = (VirtualMethodCallInfo*)(size_t)handle;
	jint i;
	if (!infos)
		return;
	for (i = 0; i < size; i++) {
		(*env)->DeleteGlobalRef(env, infos[i].fClass);
		freeCommon(&infos[i].fInfo);
	}
	free(infos);
}

#define FUNC_VOID_3(name, t1, t2, t3, nt1, nt2, nt3) \
void JNICALL Java_com_bridj_JNI_ ## name(JNIEnv *env, jclass clazz, t1 a1, t2 a2, t3 a3) \
{ \
	BEGIN_TRY(); \
	name((nt1)a1, (nt2)a2, (nt3)a3); \
	END_TRY(env); \
}

#define FUNC_3(ret, name, t1, t2, t3, nt1, nt2, nt3) \
ret JNICALL Java_com_bridj_JNI_ ## name(JNIEnv *env, jclass clazz, t1 a1, t2 a2, t3 a3) \
{ \
	BEGIN_TRY(); \
	return (ret)name((nt1)a1, (nt2)a2, (nt3)a3); \
	END_TRY_RET(env, (ret)0); \
}

#define FUNC_VOID_1(name, t1, nt1) \
void JNICALL Java_com_bridj_JNI_ ## name(JNIEnv *env, jclass clazz, t1 a1) \
{ \
	BEGIN_TRY(); \
	name((nt1)a1); \
	END_TRY(env); \
}

#define FUNC_1(ret, name, t1, nt1) \
ret JNICALL Java_com_bridj_JNI_ ## name(JNIEnv *env, jclass clazz, t1 a1) \
{ \
	BEGIN_TRY(); \
	return (ret)name((nt1)a1); \
	END_TRY_RET(env, (ret)0); \
}

FUNC_1(jlong, malloc, jlong, size_t)
FUNC_VOID_1(free, jlong, void*)

FUNC_1(jlong, strlen, jlong, char*)
FUNC_1(jlong, wcslen, jlong, wchar_t*)

FUNC_VOID_3(memcpy, jlong, jlong, jlong, void*, void*, size_t)
FUNC_VOID_3(memmove, jlong, jlong, jlong, void*, void*, size_t)

//FUNC_VOID_3(wmemcpy, jlong, jlong, jlong, wchar_t*, wchar_t*, size_t)
//FUNC_VOID_3(wmemmove, jlong, jlong, jlong, wchar_t*, wchar_t*, size_t)

FUNC_3(jlong, memchr, jlong, jbyte, jlong, void*, unsigned char, size_t)
FUNC_3(jint, memcmp, jlong, jlong, jlong, void*, void*, size_t)
FUNC_VOID_3(memset, jlong, jbyte, jlong, void*, unsigned char, size_t)

#include "PrimDefs_int.h"
#include "JNI_prim.h"

#include "PrimDefs_long.h"
#include "JNI_prim.h"

#include "PrimDefs_short.h"
#include "JNI_prim.h"

#include "PrimDefs_byte.h"
#include "JNI_prim.h"

#include "PrimDefs_char.h"
#include "JNI_prim.h"

#include "PrimDefs_boolean.h"
#include "JNI_prim.h"

#include "PrimDefs_float.h"
#include "JNI_prim.h"

#include "PrimDefs_double.h"
#include "JNI_prim.h"
