#include "com_bridj_JNI.h"

#include "dyncallback/dyncall_callback.h"
#include "dynload/dynload.h"
#include "RawNativeForwardCallback.h"

#include "bridj.hpp"
#include <string.h>
//#include <stdlib.h>
#include "Exceptions.h"

#if defined(DC_UNIX)
#include <dlfcn.h>
#endif

#if defined(DC__OS_Win64) || defined(DC__OS_Win32)
//#include <Dbghelp.h>
#endif

#pragma warning(disable: 4152)

#pragma warning(disable: 4189) // local variable initialized but unreferenced // TODO remove this !

#define JNI_SIZEOF(type, escType) \
jint JNICALL Java_com_bridj_JNI_sizeOf_1 ## escType(JNIEnv *env, jclass clazz) { return sizeof(type); }

#define JNI_SIZEOF_t(type) JNI_SIZEOF(type ## _t, type ## _1t)

JNI_SIZEOF_t(size)
JNI_SIZEOF_t(wchar)
JNI_SIZEOF_t(ptrdiff)
JNI_SIZEOF(long, long)

jclass gPointerClass = NULL;
jmethodID gAddressMethod = NULL;
jmethodID gGetPeerMethod = NULL;
jmethodID gCreatePeerMethod = NULL;

int main() {}

void initMethods(JNIEnv* env) {
	if (!gAddressMethod)
	{
		gPointerClass = (*env)->FindClass(env, "com/bridj/Pointer");
		gAddressMethod = (*env)->GetStaticMethodID(env, gPointerClass, "getAddress", "(Lcom/bridj/NativeObject;Ljava/lang/Class;)J");
		gGetPeerMethod = (*env)->GetMethodID(env, gPointerClass, "getPeer", "()J");
		gCreatePeerMethod = (*env)->GetStaticMethodID(env, gPointerClass, "pointerToAddress", "(JLjava/lang/Class;)Lcom/bridj/Pointer;");
		
	}
}

//void main() {}

jobject createPointer(JNIEnv *env, void* ptr, jclass targetType) {
	initMethods(env);
	return (*env)->CallStaticObjectMethod(env, gPointerClass, gCreatePeerMethod, (jlong)(size_t)ptr, targetType);
}

void* getPointerPeer(JNIEnv *env, jobject pointer) {
	initMethods(env);
	return (void*)(size_t)(*env)->CallLongMethod(env, gPointerClass, gGetPeerMethod, pointer);
}

void* getNativeObjectPointer(JNIEnv *env, jobject instance, jclass targetClass) {
	initMethods(env);
	return (void*)(size_t)(*env)->CallStaticLongMethod(env, gPointerClass, gAddressMethod, instance, targetClass);
}
//void _DllMainCRTStartup();

void JNICALL Java_com_bridj_JNI_init(JNIEnv *env, jclass clazz)
{
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

jboolean GetInfoClassAndFields(
	JNIEnv* env						, 
	jclass* 	clOut				, 
	jfieldID* 	javaSignature 		,
	jfieldID* 	dcSignature 		,
	jfieldID* 	paramsValueTypes 	,
	jfieldID* 	returnValueType 	,
	jfieldID* 	forwardedPointer 	,
	jfieldID* 	virtualIndex 		,
	jfieldID* 	virtualTableOffset	,
	jfieldID* 	javaCallback 		,
	jfieldID* 	direct		 		,
	jfieldID* 	startsWithThis 		,
	jfieldID* 	methodName			,
	jfieldID* 	declaringClass		,
	jfieldID* 	dcCallingConvention	
) {
	jclass cl = (*env)->FindClass(env, "com/bridj/MethodCallInfo");
	if (clOut)
		*clOut = cl;    

#define GETFIELD(out, name, sig) \
	if (out) { \
		if (!(*out = (*env)->GetFieldID(env, cl, name, sig))) { \
			throwException(env, "Failed to get field " #name " in MethodCallInfo !"); \
			return JNI_FALSE; \
		} \
	}

	GETFIELD(javaSignature 		,	"javaSignature"			,	"Ljava/lang/String;"	);
	GETFIELD(dcSignature 		,	"dcSignature" 			,	"Ljava/lang/String;"	);
	GETFIELD(methodName 		,	"methodName" 			,	"Ljava/lang/String;"	);
	GETFIELD(declaringClass		,	"declaringClass" 		,	"Ljava/lang/Class;"		);
	GETFIELD(paramsValueTypes 	,	"paramsValueTypes"		,	"[I"					);
	GETFIELD(returnValueType 	,	"returnValueType" 		,	"I"						);
	GETFIELD(forwardedPointer 	,	"forwardedPointer" 		,	"J"						);
	GETFIELD(virtualIndex 		,	"virtualIndex" 			,	"I"						);
	GETFIELD(virtualTableOffset	,	"virtualTableOffset"	,	"I"						);
	GETFIELD(javaCallback 		,	"javaCallback" 			,	"Lcom/bridj/Callback;"	);
	GETFIELD(direct		 		,	"direct"	 			,	"Z"						);
	GETFIELD(startsWithThis		,	"startsWithThis"		,	"Z"						);
	GETFIELD(dcCallingConvention,	"dcCallingConvention"	,	"I"						);
	
	return JNI_TRUE;
}

#define BEGIN_INFOS_LOOP(type)                                                                                           \
	jsize i, n = (*env)->GetArrayLength(env, methodCallInfos);															 \
	jclass 		cl					;                                                                                    \
	jfieldID 	id_javaSignature 		;                                                                                \
	jfieldID 	id_dcSignature 		;                                                                                    \
	jfieldID 	id_paramsValueTypes 	;                                                                                \
	jfieldID 	id_returnValueType 	;                                                                                    \
	jfieldID 	id_forwardedPointer 	;                                                                                \
	jfieldID 	id_virtualIndex 		;                                                                                \
	jfieldID 	id_virtualTableOffset	;                                                                                \
	jfieldID 	id_javaCallback 		;                                                                                \
	jfieldID 	id_direct		 		;                                                                                \
	jfieldID 	id_startsWithThis 		;                                                                                \
	jfieldID 	id_dcCallingConvention	;                                                                                \
	jfieldID 	id_methodName			;                                                                                \
	jfieldID 	id_declaringClass		;                                                                                \
	NEW_STRUCTS(n, type, infos);																						 \
	                                                                                                                     \
	if (!GetInfoClassAndFields(env,                                                                                      \
		&cl					,                                                                                            \
		&id_javaSignature 		,                                                                                        \
		&id_dcSignature 		,                                                                                        \
		&id_paramsValueTypes 	,                                                                                        \
		&id_returnValueType 	,                                                                                        \
		&id_forwardedPointer 	,                                                                                        \
		&id_virtualIndex 		,                                                                                        \
		&id_virtualTableOffset	,                                                                                        \
		&id_javaCallback 		,                                                                                        \
		&id_direct		 		,                                                                                        \
		&id_startsWithThis 		,                                                                                        \
		&id_methodName			,                                                                                    	 \
		&id_declaringClass 		,                                                                                    	 \
		&id_dcCallingConvention                                                                                          \
	))																													 \
		return 0;                                                                                                        \
	                                                                                                                     \
	for (i = 0; i < n; i++)                                                                                              \
	{                  																									 \
		type* info = &infos[i];																							 \
		jobject methodCallInfo = (*env)->GetObjectArrayElement(env, methodCallInfos, i);                                 \
		                                                                                                                 \
		jstring 	javaSignature 		= (*env)->GetObjectField(	env, methodCallInfo, id_javaSignature 			);   \
		jstring 	dcSignature 		= (*env)->GetObjectField(	env, methodCallInfo, id_dcSignature 		    );   \
		jstring 	methodName	 		= (*env)->GetObjectField(	env, methodCallInfo, id_methodName	 		    );   \
		jstring 	declaringClass 		= (jclass)(*env)->GetObjectField(	env, methodCallInfo, id_declaringClass	);   \
		jintArray 	paramsValueTypes 	= (*env)->GetObjectField(	env, methodCallInfo, id_paramsValueTypes 	    );   \
		jobject 	javaCallback 		= (*env)->GetObjectField(	env, methodCallInfo, id_javaCallback 		    );   \
		jlong 		forwardedPointer 	= (*env)->GetLongField(		env, methodCallInfo, id_forwardedPointer 	    );   \
		jint	 	returnValueType 	= (*env)->GetIntField(		env, methodCallInfo, id_returnValueType 	    );   \
		jint	 	virtualIndex 		= (*env)->GetIntField(		env, methodCallInfo, id_virtualIndex 		    );   \
		jint	 	virtualTableOffset	= (*env)->GetIntField(		env, methodCallInfo, id_virtualTableOffset		);   \
		jint	 	dcCallingConvention	= (*env)->GetIntField(		env, methodCallInfo, id_dcCallingConvention		);   \
		jboolean 	direct		 		= (*env)->GetBooleanField(	env, methodCallInfo, id_direct		 			);   \
		jboolean 	startsWithThis		= (*env)->GetBooleanField(	env, methodCallInfo, id_startsWithThis 			);   \
		jsize		nParams				= (*env)->GetArrayLength(	env, paramsValueTypes);
		
#define END_INFOS_LOOP() }

JNIEXPORT jlong JNICALL Java_com_bridj_JNI_bindJavaToCCallbacks(
	JNIEnv *env, 
	jclass clazz,
	jobjectArray methodCallInfos
) {
	BEGIN_INFOS_LOOP(JavaToNativeCallbackCallInfo)
	{
		void* callback;
		const char* dcSig;
		
		// TODO DIRECT C++ virtual thunk
		dcSig = (*env)->GetStringUTFChars(env, dcSignature, NULL);
		callback = dcbNewCallback(dcSig, NativeToJavaCallHandler, info);
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
JNIEXPORT jlong JNICALL Java_com_bridj_JNI_bindJavaMethodsToCFunctions(
	JNIEnv *env, 
	jclass clazz,
	jobjectArray methodCallInfos
) {
	BEGIN_INFOS_LOOP(FunctionCallInfo)
	{
		info->fForwardedSymbol = (void*)(size_t)forwardedPointer;
		if (direct && forwardedPointer)
			info->fInfo.fDCCallback = (DCCallback*)dcRawCallAdapterSkipTwoArgs((void (*)())forwardedPointer, dcCallingConvention);
		
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

/*
JNIEXPORT jlong JNICALL Java_com_bridj_JNI_createCallback(
	JNIEnv *env, 
	jclass clazz,
	jint callbackType,
	jclass declaringClass,
	jobject javaCallbackInstance,
	jobject method,
	jboolean startsWithThis,
	jstring methodName,
	jint callMode,
	jlong forwardedPointer, 
	jint virtualTableOffset,
	jint virtualIndex,
	jboolean direct, 
	jstring javaSignature, 
	jstring dcSignature,
	jboolean isJavaToCCallback,
	jint nParams,
	jint returnValueType, 
	jintArray paramsValueTypes
) {
	
	struct CommonCallbackInfo *pCommonInfo = NULL;
	void *pInfo = NULL;
	void *callbackToRegister = NULL;
	
	switch ((CallbackType)callbackType)
	{
	case eJavaCallbackToNativeFunction:
		{
			const char *dcSig, *javaSig, *methName;
			NEW_STRUCT(JavaToNativeCallbackCallInfo, info, pInfo, pCommonInfo);
			
			javaSig = (char*)(*env)->GetStringUTFChars(env, javaSignature, NULL);
			methName = (char*)(*env)->GetStringUTFChars(env, methodName, NULL);
			info->fMethod = (*env)->GetMethodID(env, declaringClass, methName, javaSig);
			(*env)->ReleaseStringUTFChars(env, javaSignature, javaSig);
			(*env)->ReleaseStringUTFChars(env, methodName, methName);
			
			// TODO DIRECT C++ virtual thunk
			dcSig = (*env)->GetStringUTFChars(env, dcSignature, NULL);
			info->fInfo.fDCCallback = dcbNewCallback(dcSig, NativeToJavaCallHandler, info);
			(*env)->ReleaseStringUTFChars(env, dcSignature, dcSig);
		} 
		break;
	case eNativeToJavaCallback:
		{
			const char *dcSig, *javaSig, *methName;
			NEW_STRUCT(NativeToJavaCallbackCallInfo, info, pInfo, pCommonInfo);
			
			javaSig = (char*)(*env)->GetStringUTFChars(env, javaSignature, NULL);
			methName = (char*)(*env)->GetStringUTFChars(env, methodName, NULL);
			info->fCallbackInstance = javaCallbackInstance;
			info->fMethod = (*env)->GetMethodID(env, declaringClass, methName, javaSig);
			info->fJNICallFunction = getJNICallFunction(env, (ValueType)returnValueType);
			
			(*env)->ReleaseStringUTFChars(env, javaSignature, javaSig);
			(*env)->ReleaseStringUTFChars(env, methodName, methName);
			
			// TODO DIRECT C++ virtual thunk
			dcSig = (*env)->GetStringUTFChars(env, dcSignature, NULL);
			info->fInfo.fDCCallback = dcbNewCallback(dcSig, NativeToJavaCallHandler, info);
			(*env)->ReleaseStringUTFChars(env, dcSignature, dcSig);
		}
		break;
	case eJavaToVirtualMethod:
		{
			const char* ds;
			NEW_STRUCT(VirtualMethodCallInfo, info, pInfo, pCommonInfo);
			
			info->fClass = (*env)->NewGlobalRef(env, declaringClass);
			info->fHasThisPtrArg = startsWithThis;
			info->fVirtualIndex = virtualIndex;
			info->fVirtualTableOffset = virtualTableOffset;
			//info->fClass = NULL;//TODO declaringClass;
			
			// TODO DIRECT C++ virtual thunk
			ds = (*env)->GetStringUTFChars(env, dcSignature, NULL);
			info->fInfo.fDCCallback = dcbNewCallback(ds, JavaToVirtualMethodCallHandler, info);
			(*env)->ReleaseStringUTFChars(env, dcSignature, ds);
			
			callbackToRegister = info->fInfo.fDCCallback;
		}
		break;
	case eJavaToNativeFunction:
		{
			NEW_STRUCT(FunctionCallInfo, info, pInfo, pCommonInfo);
			
			info->fForwardedSymbol = (void*)(size_t)forwardedPointer;
			if (direct && forwardedPointer)
				info->fInfo.fDCCallback = (DCCallback*)dcRawCallAdapterSkipTwoArgs((void (*)())forwardedPointer, callMode);
			
			if (!info->fInfo.fDCCallback) {
				const char* ds = (*env)->GetStringUTFChars(env, dcSignature, NULL);
				info->fInfo.fDCCallback = dcbNewCallback(ds, JavaToFunctionCallHandler, info);
				(*env)->ReleaseStringUTFChars(env, dcSignature, ds);
			}
			callbackToRegister = info->fInfo.fDCCallback;
		}
		break;
	default:
		throwException(env, "Unknown callbackType !");
		return 0;
	}
	initCommonCallInfo(pCommonInfo, env, callMode, nParams, returnValueType, paramsValueTypes);
	
	if (callbackToRegister) {
		JNINativeMethod meth;
		meth.fnPtr = callbackToRegister;
		meth.name = (char*)(*env)->GetStringUTFChars(env, methodName, NULL);
		meth.signature = (char*)(*env)->GetStringUTFChars(env, javaSignature, NULL);
		(*env)->RegisterNatives(env, declaringClass, &meth, 1);
		
		(*env)->ReleaseStringUTFChars(env, methodName, meth.name);
		(*env)->ReleaseStringUTFChars(env, javaSignature, meth.signature);
	}
	return (jlong)(size_t)pInfo;
}

JNIEXPORT void JNICALL Java_com_bridj_JNI_freeCallback(
	JNIEnv *env, 
	jclass clazz,
	jint callbackType,
	jlong nativeCallback
) {
	CallInfo* info = (CallInfo*)nativeCallback;
	if (info->nParams)
		free(info->fParamTypes);
	
	dcbFreeCallback((DCCallback*)info->fDCCallback);
	free(info);
}*/


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
