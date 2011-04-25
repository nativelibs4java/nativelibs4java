#include "org_bridj_JNI.h"

#include "dyncallback/dyncall_callback.h"
#include "dynload/dynload.h"
#include "RawNativeForwardCallback.h"

#include "bridj.hpp"
#include <string.h>
#include <math.h>
#include "Exceptions.h"

#pragma warning(disable: 4152)
#pragma warning(disable: 4189) // local variable initialized but unreferenced // TODO remove this !

#define JNI_SIZEOF(type, escType) \
jint JNICALL Java_org_bridj_JNI_sizeOf_1 ## escType(JNIEnv *env, jclass clazz) { return sizeof(type); }

#define JNI_SIZEOF_t(type) JNI_SIZEOF(type ## _t, type ## _1t)

JNI_SIZEOF_t(size)
JNI_SIZEOF_t(wchar)
JNI_SIZEOF_t(ptrdiff)
JNI_SIZEOF(long, long)

//jclass gStructFieldsIOClass = NULL;
jclass gObjectClass = NULL;
jclass gPointerClass = NULL;
jclass gFlagSetClass = NULL;
jclass gValuedEnumClass = NULL;
jclass gBridJClass = NULL;
jclass gCallIOClass = NULL;
jmethodID gAddressMethod = NULL;
jmethodID gGetPeerMethod = NULL;
jmethodID gCreatePeerMethod = NULL;
jmethodID gGetValuedEnumValueMethod = NULL;
jmethodID gGetJavaObjectFromNativePeerMethod = NULL;
jmethodID gNewFlagSetMethod = NULL;
jmethodID gGetCallIOsMethod = NULL;
jmethodID gNewCallIOInstance = NULL;

jclass 		gMethodCallInfoClass 		 = NULL;
jfieldID 	gFieldId_javaSignature 		 = NULL;
jfieldID 	gFieldId_dcSignature 		 = NULL;    
jfieldID 	gFieldId_paramsValueTypes 	 = NULL;
jfieldID 	gFieldId_returnValueType 	 = NULL;    
jfieldID 	gFieldId_forwardedPointer 	 = NULL;
jfieldID 	gFieldId_virtualIndex 		 = NULL;
jfieldID 	gFieldId_virtualTableOffset	 = NULL;
jfieldID 	gFieldId_javaCallback 		 = NULL;
jfieldID 	gFieldId_isGenericCallback    = NULL;
jfieldID 	gFieldId_direct		 		 = NULL;
jfieldID 	gFieldId_startsWithThis 	     = NULL;
jfieldID 	gFieldId_isCPlusPlus 	     = NULL;
jfieldID 	gFieldId_isStatic    	     = NULL;
jfieldID 	gFieldId_bNeedsThisPointer 	 = NULL;
jfieldID 	gFieldId_dcCallingConvention = NULL;
jfieldID 	gFieldId_symbolName			 = NULL;
jfieldID 	gFieldId_nativeClass			 = NULL;
jfieldID 	gFieldId_methodName			 = NULL;
jfieldID 	gFieldId_declaringClass		 = NULL;

/*jclass gCLongClass = NULL;
jclass gSizeTClass = NULL;
jmethodID gCLongValueMethod = NULL;
jmethodID gSizeTValueMethod = NULL;
jlong UnboxCLong(JNIEnv* env, jobject v) {
	return (*env)->CallLongMethod(env, v, gCLongValueMethod);
}
jlong UnboxSizeT(JNIEnv* env, jobject v) { \
	return (*env)->CallLongMethod(env, v, gSizeTValueMethod);
}*/

#define BOX_METHOD_IMPL(prim, shortName, methShort, type, letter) \
jclass g ## shortName ## Class = NULL; \
jmethodID g ## shortName ## ValueOfMethod = NULL; \
jmethodID g ## shortName ## ValueMethod = NULL; \
jobject Box ## shortName(JNIEnv* env, j ## type v) { \
	return (*env)->CallStaticObjectMethod(env, g ## shortName ## Class, g ## shortName ## ValueOfMethod, v); \
} \
j ## type Unbox ## shortName(JNIEnv* env, jobject v) { \
	return (*env)->Call ## methShort ## Method(env, v, g ## shortName ## ValueMethod); \
}
			
BOX_METHOD_IMPL("org/bridj/SizeT", SizeT, Long, long, "J");
BOX_METHOD_IMPL("org/bridj/CLong", CLong, Long, long, "J");
BOX_METHOD_IMPL("java/lang/Integer", Int, Int, int, "I");
BOX_METHOD_IMPL("java/lang/Long", Long, Long, long, "J");
BOX_METHOD_IMPL("java/lang/Short", Short, Short, short, "S");
BOX_METHOD_IMPL("java/lang/Byte", Byte, Byte, byte, "B");
BOX_METHOD_IMPL("java/lang/Boolean", Boolean, Boolean, boolean, "Z");
BOX_METHOD_IMPL("java/lang/Character", Char, Char, char, "C");
BOX_METHOD_IMPL("java/lang/Float", Float, Float, float, "F");
BOX_METHOD_IMPL("java/lang/Double", Double, Double, double, "D");    
		
int main() {}

void printStackTrace(JNIEnv* env, jthrowable ex) {
	jthrowable cause;
	jclass thClass = (*env)->FindClass(env, "java/lang/Throwable");
	jmethodID printMeth = (*env)->GetMethodID(env, thClass, "printStackTrace", "()V");
	jmethodID causeMeth = (*env)->GetMethodID(env, thClass, "getCause", "()Ljava/lang/Throwable;");
	if (!ex) {
		jclass exClass = (*env)->FindClass(env, "java/lang/RuntimeException");
		jmethodID initMeth = (*env)->GetMethodID(env, exClass, "<init>", "()V");
		ex = (jthrowable)(*env)->NewObject(env, exClass, initMeth);
	}
	(*env)->CallVoidMethod(env, (jobject)ex, printMeth);
	cause = (jthrowable)(*env)->CallObjectMethod(env, ex, causeMeth);
	if (cause)
		printStackTrace(env, cause);
}

JavaVM* gJVM = NULL;
#define JNI_VERSION JNI_VERSION_1_4

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* jvm, void* x) {
  gJVM = jvm;
  return JNI_VERSION;
}

JNIEnv* GetEnv() {
  JNIEnv* env = NULL;
  if ((*gJVM)->GetEnv(gJVM, (void*)&env, JNI_VERSION) != JNI_OK) {
    if ((*gJVM)->AttachCurrentThread(gJVM, (void*)&env, NULL) != JNI_OK) {
	  printf("BridJ: Cannot attach current JVM thread !\n");
      return NULL;
    }
  }
  return env;
}

void initMethods(JNIEnv* env) {
	if (!gAddressMethod)
	{
		#define FIND_GLOBAL_CLASS(name) (*env)->NewGlobalRef(env, (*env)->FindClass(env, name))
		
		gObjectClass = FIND_GLOBAL_CLASS("java/lang/Object");
		
		#define INIT_PRIM(prim, shortName, methShort, type, letter) \
			g ## shortName ## Class = FIND_GLOBAL_CLASS(prim); \
			g ## shortName ## ValueMethod = (*env)->GetMethodID(env, g ## shortName ## Class, #type "Value", "()" letter); \
			g ## shortName ## ValueOfMethod = (*env)->GetStaticMethodID(env, g ## shortName ## Class, "valueOf", "(" letter ")L" prim ";");
			
		INIT_PRIM("org/bridj/SizeT", SizeT, Long, long, "J");
		INIT_PRIM("org/bridj/CLong", CLong, Long, long, "J");
		INIT_PRIM("java/lang/Integer", Int, Int, int, "I");
		INIT_PRIM("java/lang/Long", Long, Long, long, "J");
		INIT_PRIM("java/lang/Short", Short, Short, short, "S");
		INIT_PRIM("java/lang/Byte", Byte, Byte, byte, "B");
		INIT_PRIM("java/lang/Boolean", Boolean, Boolean, boolean, "Z");
		INIT_PRIM("java/lang/Character", Char, Char, char, "C");
		INIT_PRIM("java/lang/Float", Float, Float, float, "F");
		INIT_PRIM("java/lang/Double", Double, Double, double, "D");    
		
		gBridJClass = FIND_GLOBAL_CLASS("org/bridj/BridJ");
		gFlagSetClass = FIND_GLOBAL_CLASS("org/bridj/FlagSet");
		gValuedEnumClass = FIND_GLOBAL_CLASS("org/bridj/ValuedEnum");
		//gStructFieldsIOClass = FIND_GLOBAL_CLASS("org/bridj/StructFieldsIO");
		gPointerClass = FIND_GLOBAL_CLASS("org/bridj/Pointer");
		gMethodCallInfoClass = FIND_GLOBAL_CLASS("org/bridj/MethodCallInfo");
		gCallIOClass = FIND_GLOBAL_CLASS("org/bridj/CallIO");
		
		//gGetTempCallStruct = (*env)->GetStaticMethodID(env, gBridJClass, "getTempCallStruct", "()J"); 
		//gReleaseTempCallStruct = (*env)->GetStaticMethodID(env, gBridJClass, "releaseTempCallStruct", "(J)V"); 
		gGetValuedEnumValueMethod = (*env)->GetMethodID(env, gValuedEnumClass, "value", "()J");
		gGetJavaObjectFromNativePeerMethod = (*env)->GetStaticMethodID(env, gBridJClass, "getJavaObjectFromNativePeer", "(J)Ljava/lang/Object;");
		gNewFlagSetMethod = (*env)->GetStaticMethodID(env, gFlagSetClass, "fromValue", "(JLjava/lang/Class;)Lorg/bridj/FlagSet;"); 
		gAddressMethod = (*env)->GetStaticMethodID(env, gPointerClass, "getAddress", "(Lorg/bridj/NativeObject;Ljava/lang/Class;)J");
		gGetPeerMethod = (*env)->GetMethodID(env, gPointerClass, "getPeer", "()J");
		gCreatePeerMethod = (*env)->GetStaticMethodID(env, gPointerClass, "pointerToAddress", "(JLjava/lang/Class;)Lorg/bridj/Pointer;");
		gGetCallIOsMethod = (*env)->GetMethodID(env, gMethodCallInfoClass, "getCallIOs", "()[Lorg/bridj/CallIO;");
		gNewCallIOInstance = (*env)->GetMethodID(env, gCallIOClass, "newInstance", "(J)Ljava/lang/Object;");
		
#define GETFIELD_ID(out, name, sig) \
		if (!(gFieldId_ ## out = (*env)->GetFieldID(env, gMethodCallInfoClass, name, sig))) \
			throwException(env, "Failed to get the field " #name " in MethodCallInfo !");
		
	
		GETFIELD_ID(javaSignature 		,	"javaSignature"		,	"Ljava/lang/String;"		);
		GETFIELD_ID(dcSignature 			,	"dcSignature" 		,	"Ljava/lang/String;"		);
		GETFIELD_ID(symbolName 			,	"symbolName" 		,	"Ljava/lang/String;"		);
		GETFIELD_ID(nativeClass 			,	"nativeClass" 		,	"J"						);
		GETFIELD_ID(methodName 			,	"methodName" 		,	"Ljava/lang/String;"		);
		GETFIELD_ID(declaringClass		,	"declaringClass" 	,	"Ljava/lang/Class;"		);
		GETFIELD_ID(paramsValueTypes 		,	"paramsValueTypes"	,	"[I"						);
		GETFIELD_ID(returnValueType 		,	"returnValueType" 	,	"I"						);
		GETFIELD_ID(forwardedPointer 		,	"forwardedPointer" 	,	"J"						);
		GETFIELD_ID(virtualIndex 		,	"virtualIndex"		,	"I"						);
		GETFIELD_ID(virtualTableOffset	,	"virtualTableOffset"	,	"I"						);
		GETFIELD_ID(javaCallback 		,	"javaCallback" 		,	"Lorg/bridj/Callback;"	);
		GETFIELD_ID(isGenericCallback 	,	"isGenericCallback"	,	"Z"						);
		GETFIELD_ID(direct		 		,	"direct"	 			,	"Z"						);
		GETFIELD_ID(isCPlusPlus	 		,	"isCPlusPlus"		,	"Z"						);
		GETFIELD_ID(isStatic		 		,	"isStatic"			,	"Z"						);
		GETFIELD_ID(startsWithThis		,	"startsWithThis"		,	"Z"						);
		GETFIELD_ID(bNeedsThisPointer	,	"bNeedsThisPointer"		,	"Z"						);
		GETFIELD_ID(dcCallingConvention,	"dcCallingConvention"	,	"I"						);
		
	}
}

jlong getFlagValue(JNIEnv *env, jobject valuedEnum)
{
	return valuedEnum ? (*env)->CallLongMethod(env, valuedEnum, gGetValuedEnumValueMethod) : 0;	
}

jobject newFlagSet(JNIEnv *env, jlong value)
{
	return (*env)->CallStaticObjectMethod(env, gFlagSetClass, gNewFlagSetMethod, value);	
}

//void main() {}
jmethodID GetMethodIDOrFail(JNIEnv* env, jclass declaringClass, const char* methName, const char* javaSig)
{
	jmethodID id = (*env)->GetStaticMethodID(env, declaringClass, methName, javaSig);
	if (!id) {
		(*env)->ExceptionClear(env);
		id = (*env)->GetMethodID(env, declaringClass, methName, javaSig);
	}
	if (!id)
		throwException(env, "Couldn't find this method !");
	
	return id;
}


jobject createPointerFromIO(JNIEnv *env, void* ptr, jobject callIO) {
	jobject instance;
	jlong addr;
	if (!ptr || !callIO)
		return NULL;
	initMethods(env);
	addr = PTR_TO_JLONG(ptr);
	instance = (*env)->CallObjectMethod(env, callIO, gNewCallIOInstance, addr);
	return instance;
}

void* getPointerPeer(JNIEnv *env, jobject pointer) {
	initMethods(env);
	return pointer ? JLONG_TO_PTR((*env)->CallLongMethod(env, pointer, gGetPeerMethod, pointer)) : NULL;
}

void* getNativeObjectPointer(JNIEnv *env, jobject instance, jclass targetClass) {
	initMethods(env);
	return JLONG_TO_PTR((*env)->CallStaticLongMethod(env, gPointerClass, gAddressMethod, instance, targetClass));
}


jobject getJavaObjectForNativePointer(JNIEnv *env, void* nativeObject) {
	initMethods(env);
	return (*env)->CallStaticObjectMethod(env, gBridJClass, gGetJavaObjectFromNativePeerMethod, PTR_TO_JLONG(nativeObject));
}

void JNICALL Java_org_bridj_JNI_init(JNIEnv *env, jclass clazz)
{
	initThreadLocal(env);
}


jlong JNICALL Java_org_bridj_JNI_getEnv(JNIEnv *env, jclass clazz)
{
	return PTR_TO_JLONG(env);
}

jlong JNICALL Java_org_bridj_JNI_newGlobalRef(JNIEnv *env, jclass clazz, jobject obj)
{
	return obj ? PTR_TO_JLONG((*env)->NewGlobalRef(env, obj)) : 0;
}

void JNICALL Java_org_bridj_JNI_deleteGlobalRef(JNIEnv *env, jclass clazz, jlong ref)
{
	if (ref)
		(*env)->DeleteGlobalRef(env, (jobject)JLONG_TO_PTR(ref));
}
jlong JNICALL Java_org_bridj_JNI_newWeakGlobalRef(JNIEnv *env, jclass clazz, jobject obj)
{
	return obj ? PTR_TO_JLONG((*env)->NewWeakGlobalRef(env, obj)) : 0;
}

void JNICALL Java_org_bridj_JNI_deleteWeakGlobalRef(JNIEnv *env, jclass clazz, jlong ref)
{
	if (ref)
		(*env)->DeleteWeakGlobalRef(env, (jobject)JLONG_TO_PTR(ref));
}
void JNICALL Java_org_bridj_JNI_callSinglePointerArgVoidFunction(JNIEnv *env, jclass clazz, jlong constructor, jlong thisPtr, jint callMode)
{
	callSinglePointerArgVoidFunction(env, JLONG_TO_PTR(constructor), JLONG_TO_PTR(thisPtr), callMode);
}

jlong JNICALL Java_org_bridj_JNI_getDirectBufferAddress(JNIEnv *env, jobject jthis, jobject buffer) {
	BEGIN_TRY();
	return !buffer ? 0 : PTR_TO_JLONG((*env)->GetDirectBufferAddress(env, buffer));
	END_TRY_RET(env, 0);
}
jlong JNICALL Java_org_bridj_JNI_getDirectBufferCapacity(JNIEnv *env, jobject jthis, jobject buffer) {
	BEGIN_TRY();
	return !buffer ? 0 : (*env)->GetDirectBufferCapacity(env, buffer);
	END_TRY_RET(env, 0);
}

jlong JNICALL Java_org_bridj_JNI_getObjectPointer(JNIEnv *env, jclass clazz, jobject object)
{
	return PTR_TO_JLONG(object);
}
 
#if defined(DC_UNIX)
char* dlerror();
#endif

jlong JNICALL Java_org_bridj_JNI_loadLibrary(JNIEnv *env, jclass clazz, jstring pathStr)
{
	const char* path = (*env)->GetStringUTFChars(env, pathStr, NULL);
	jlong ret = PTR_TO_JLONG(dlLoadLibrary(path));
#if defined(DC_UNIX)
	if (!ret) {
		printf("# BridJ: dlopen error = %s\n", dlerror());
	}
#endif
	(*env)->ReleaseStringUTFChars(env, pathStr, path);
	return ret;
}

void JNICALL Java_org_bridj_JNI_freeLibrary(JNIEnv *env, jclass clazz, jlong libHandle)
{
	dlFreeLibrary((DLLib*)JLONG_TO_PTR(libHandle));
}

jlong JNICALL Java_org_bridj_JNI_loadLibrarySymbols(JNIEnv *env, jclass clazz, jstring libPath)
{
	DLSyms* pSyms;
	const char* libPathStr;
	libPathStr = (*env)->GetStringUTFChars(env, libPath, NULL);
	pSyms = dlSymsInit(libPathStr);
	(*env)->ReleaseStringUTFChars(env, libPath, libPathStr);
	
	return PTR_TO_JLONG(pSyms);
}
void JNICALL Java_org_bridj_JNI_freeLibrarySymbols(JNIEnv *env, jclass clazz, jlong symbolsHandle)
{
	DLSyms* pSyms = (DLSyms*)symbolsHandle;
	dlSymsCleanup(pSyms);
	free(pSyms);
}

jarray JNICALL Java_org_bridj_JNI_getLibrarySymbols(JNIEnv *env, jclass clazz, jlong libHandle, jlong symbolsHandle)
{
    jclass stringClass;
    jarray ret;
    DLSyms* pSyms = (DLSyms*)symbolsHandle;
	int count, i;
	if (!pSyms)
		return NULL;

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


jstring JNICALL Java_org_bridj_JNI_findSymbolName(JNIEnv *env, jclass clazz, jlong libHandle, jlong symbolsHandle, jlong address)
{
	const char* name = dlSymsNameFromValue((DLSyms*)JLONG_TO_PTR(symbolsHandle), JLONG_TO_PTR(address));
	return name ? (*env)->NewStringUTF(env, name) : NULL;
}

jlong JNICALL Java_org_bridj_JNI_findSymbolInLibrary(JNIEnv *env, jclass clazz, jlong libHandle, jstring nameStr)
{
	const char* name;
	void* ptr;
	if (!nameStr)
		return 0;
	
	name = (*env)->GetStringUTFChars(env, nameStr, NULL);
	
	ptr = dlFindSymbol((DLLib*)JLONG_TO_PTR(libHandle), name);
	(*env)->ReleaseStringUTFChars(env, nameStr, name);
	return PTR_TO_JLONG(ptr);
}

jobject JNICALL Java_org_bridj_JNI_newDirectByteBuffer(JNIEnv *env, jobject jthis, jlong peer, jlong length) {
	BEGIN_TRY();
	return (*env)->NewDirectByteBuffer(env, (void*)peer, length);
	END_TRY_RET(env, NULL);
}

JNIEXPORT jlong JNICALL Java_org_bridj_JNI_createCallTempStruct(JNIEnv* env, jclass clazz) {
	CallTempStruct* s = MALLOC_STRUCT(CallTempStruct);
	s->vm = dcNewCallVM(1024);
	return PTR_TO_JLONG(s);	
}
JNIEXPORT void JNICALL Java_org_bridj_JNI_deleteCallTempStruct(JNIEnv* env, jclass clazz, jlong handle) {
	CallTempStruct* s = (CallTempStruct*)JLONG_TO_PTR(handle);
	dcFree(s->vm);
	free(s);	
}

JNIEXPORT jint JNICALL Java_org_bridj_JNI_getMaxDirectMappingArgCount(JNIEnv *env, jclass clazz) {
#if defined(_WIN64)
	return 4;
#elif defined(DC__OS_Darwin) && defined(DC__Arch_AMD64)
	return 4;
#elif defined(DC__OS_Linux) && defined(DC__Arch_AMD64)
	return 4;
#elif defined(_WIN32)
	return 8;
#else
	return -1;
#endif
}

char getDCReturnType(JNIEnv* env, ValueType returnType) 
{
	switch (returnType) {
#define RET_TYPE_CASE(valueType, hiCase) \
		case valueType: \
			return DC_SIGCHAR_ ## hiCase;
		case eIntFlagSet:
		RET_TYPE_CASE(eIntValue, INT)
		RET_TYPE_CASE(eLongValue, LONGLONG)
		RET_TYPE_CASE(eShortValue, SHORT)
		RET_TYPE_CASE(eFloatValue, FLOAT)
		RET_TYPE_CASE(eDoubleValue, DOUBLE)
		case eBooleanValue:
		RET_TYPE_CASE(eByteValue, CHAR)
		case eCLongValue:
			return DC_SIGCHAR_LONGLONG;
		case eSizeTValue:
			return DC_SIGCHAR_LONGLONG;
		case eVoidValue:
			return DC_SIGCHAR_VOID;
		case ePointerValue:
			return DC_SIGCHAR_POINTER;
		case eWCharValue:
			switch (sizeof(wchar_t)) {
			case 1:
				return DC_SIGCHAR_CHAR;
			case 2:
				return DC_SIGCHAR_SHORT;
			case 4:
				return DC_SIGCHAR_INT;
			default:
				throwException(env, "wchar_t size not supported yet !");
				return DC_SIGCHAR_VOID;
			}
			// TODO
		case eNativeObjectValue:
			return DC_SIGCHAR_POINTER;
		default:
			throwException(env, "Return ValueType not supported yet !");
			return DC_SIGCHAR_VOID;
	}
}

void initCommonCallInfo(
	struct CommonCallbackInfo* info,
	JNIEnv *env,
	jint callMode,
	jint nParams,
	jint returnValueType, 
	jintArray paramsValueTypes,
	jobjectArray callIOs
) {
	info->fEnv = env;
	info->fDCMode = callMode;
	info->fReturnType = (ValueType)returnValueType;
	info->nParams = nParams;
	if (nParams) {
		info->fParamTypes = (ValueType*)malloc(nParams * sizeof(jint));	
		(*env)->GetIntArrayRegion(env, paramsValueTypes, 0, nParams, (jint*)info->fParamTypes);
	}
	info->fDCReturnType = getDCReturnType(env, info->fReturnType);
	
	if (callIOs) 
	{
		jsize n = (*env)->GetArrayLength(env, callIOs), i;
		if (n)
		{
			info->fCallIOs = (jobject*)malloc((n + 1) * sizeof(jobject));
			for (i = 0; i < n; i++) {
				jobject obj = (*env)->GetObjectArrayElement(env, callIOs, i);
				if (obj)
					obj = (*env)->NewGlobalRef(env, obj);
				info->fCallIOs[i] = obj;
			}
			info->fCallIOs[n] = NULL;
		}
	}
	
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
	case eBooleanValue:
		return (*env)->CallBooleanMethod;
	case eByteValue:
		return (*env)->CallByteMethod;
	case eShortValue:
		return (*env)->CallShortMethod;
	case eWCharValue:
		return (*env)->CallCharMethod;
	case eVoidValue:
		return (*env)->CallVoidMethod;
	case eNativeObjectValue:
	case ePointerValue:
		return (*env)->CallObjectMethod;
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
	case eBooleanValue:
		return (*env)->CallStaticBooleanMethod;
	case eByteValue:
		return (*env)->CallStaticByteMethod;
	case eShortValue:
		return (*env)->CallStaticShortMethod;
	case eWCharValue:
		return (*env)->CallStaticCharMethod;
	case eVoidValue:
		return (*env)->CallStaticVoidMethod;
	case eNativeObjectValue:
	case ePointerValue:
		return (*env)->CallStaticObjectMethod;
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

	
void registerJavaFunction(JNIEnv* env, jclass declaringClass, jstring methodName, jstring methodSignature, 
#ifdef _DEBUG
	CommonCallbackInfo* info, 
#endif
	void (*callback)())
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
	
#ifdef _DEBUG
#pragma warning(push)
#pragma warning(disable: 4996)
	info->fSymbolName = (char*)malloc(strlen(meth.name) + 1);
	strcpy(info->fSymbolName, meth.name);
#pragma warning(pop)
#endif

	(*env)->ReleaseStringUTFChars(env, methodName, meth.name);
	(*env)->ReleaseStringUTFChars(env, methodSignature, meth.signature);
}

void freeCommon(JNIEnv* env, CommonCallbackInfo* info)
{
	if (info->nParams)
		free(info->fParamTypes);
	
	if (info->fCallIOs)
	{
		jobject* ptr = info->fCallIOs;
		while (*ptr) {
			(*env)->DeleteGlobalRef(env, *ptr);
			ptr++;
		}
		free(info->fCallIOs);
	}
	
#ifdef _DEBUG
	if (info->fSymbolName)
		free(info->fSymbolName);
#endif

	dcbFreeCallback((DCCallback*)info->fDCCallback);
}
	      
#define GetField_javaSignature()         jstring          javaSignature        = (*env)->GetObjectField(   env, methodCallInfo, gFieldId_javaSignature       )
#define GetField_dcSignature()           jstring          dcSignature          = (*env)->GetObjectField(   env, methodCallInfo, gFieldId_dcSignature         )
#define GetField_symbolName()            jstring          symbolName           = (*env)->GetObjectField(   env, methodCallInfo, gFieldId_symbolName          )
#define GetField_nativeClass()           jlong            nativeClass          = (*env)->GetLongField(     env, methodCallInfo, gFieldId_nativeClass         )
#define GetField_methodName()            jstring          methodName           = (*env)->GetObjectField(   env, methodCallInfo, gFieldId_methodName          )
#define GetField_paramsValueTypes()      jintArray        paramsValueTypes     = (*env)->GetObjectField(   env, methodCallInfo, gFieldId_paramsValueTypes    )
#define GetField_javaCallback()          jobject          javaCallback         = (*env)->GetObjectField(   env, methodCallInfo, gFieldId_javaCallback        )
#define GetField_isGenericCallback()     jboolean         isGenericCallback    = (*env)->GetBooleanField(  env, methodCallInfo, gFieldId_isGenericCallback   )
#define GetField_forwardedPointer()      jlong            forwardedPointer     = (*env)->GetLongField(     env, methodCallInfo, gFieldId_forwardedPointer    )
#define GetField_returnValueType()       jint             returnValueType      = (*env)->GetIntField(      env, methodCallInfo, gFieldId_returnValueType     )
#define GetField_virtualIndex()          jint             virtualIndex         = (*env)->GetIntField(      env, methodCallInfo, gFieldId_virtualIndex        )
#define GetField_virtualTableOffset()    jint             virtualTableOffset   = (*env)->GetIntField(      env, methodCallInfo, gFieldId_virtualTableOffset  )
#define GetField_dcCallingConvention()   jint             dcCallingConvention  = (*env)->GetIntField(      env, methodCallInfo, gFieldId_dcCallingConvention )
#define GetField_direct()                jboolean         direct               = (*env)->GetBooleanField(  env, methodCallInfo, gFieldId_direct              )
#define GetField_isCPlusPlus()           jboolean         isCPlusPlus          = (*env)->GetBooleanField(  env, methodCallInfo, gFieldId_isCPlusPlus         )
#define GetField_isStatic()              jboolean         isStatic             = (*env)->GetBooleanField(  env, methodCallInfo, gFieldId_isStatic            )
#define GetField_startsWithThis()        jboolean         startsWithThis       = (*env)->GetBooleanField(  env, methodCallInfo, gFieldId_startsWithThis      )
#define GetField_bNeedsThisPointer()     jboolean         bNeedsThisPointer    = (*env)->GetBooleanField(  env, methodCallInfo, gFieldId_bNeedsThisPointer   )
#define GetField_declaringClass()        jstring          declaringClass       = (jclass)(*env)->GetObjectField(env, methodCallInfo, gFieldId_declaringClass )
#define GetField_nParams()               jsize            nParams              = (*env)->GetArrayLength(   env, paramsValueTypes                             )
#define GetField_callIOs()               jobjectArray     callIOs              = (*env)->CallObjectMethod( env, methodCallInfo, gGetCallIOsMethod            )


#define BEGIN_INFOS_LOOP(type)                                                                                           \
	jsize i, n = (*env)->GetArrayLength(env, methodCallInfos);															 \
	NEW_STRUCTS(n, type, infos);																						 \
	initMethods(env);                                                                                        	 		 \
	for (i = 0; i < n; i++)                                                                                          	 \
	{                  																								 	 \
		type* info = &infos[i];																						 	 \
		jobject methodCallInfo = (*env)->GetObjectArrayElement(env, methodCallInfos, i);
		
#define END_INFOS_LOOP() }

JNIEXPORT jlong JNICALL Java_org_bridj_JNI_createCToJavaCallback(
	JNIEnv *env, 
	jclass clazz,
	jobject methodCallInfo
) {
	struct NativeToJavaCallbackCallInfo* info = NULL;
	{
		const char* dcSig, *javaSig, *methName;
		
		GetField_javaSignature()        ;
		GetField_dcSignature()          ;
		GetField_symbolName()           ;
		GetField_nativeClass()          ;
		GetField_methodName()           ;
		GetField_paramsValueTypes()     ;
		GetField_javaCallback()         ;
		GetField_isGenericCallback()    ;
		//GetField_forwardedPointer()     ;
		GetField_returnValueType()      ;
		//GetField_virtualIndex()         ;
		//GetField_virtualTableOffset()   ;
		GetField_dcCallingConvention()  ;
		//GetField_direct()               ;
		//GetField_startsWithThis()       ;
		//GetField_bNeedsThisPointer()    ;
		GetField_isCPlusPlus()          ;
		GetField_declaringClass()       ;
		GetField_nParams()              ;
		GetField_callIOs()              ;
		
		{
			info = MALLOC_STRUCT(NativeToJavaCallbackCallInfo);
			memset(info, 0, sizeof(struct NativeToJavaCallbackCallInfo));
			
			// TODO DIRECT C++ virtual thunk
			javaSig = (char*)(*env)->GetStringUTFChars(env, javaSignature, NULL);
			dcSig = (*env)->GetStringUTFChars(env, dcSignature, NULL);
			methName = (char*)(*env)->GetStringUTFChars(env, methodName, NULL);

			info->fInfo.fDCCallback = dcbNewCallback(dcSig, isCPlusPlus ? CPPToJavaCallHandler : CToJavaCallHandler, info);
			info->fCallbackInstance = (*env)->NewGlobalRef(env, javaCallback);
			info->fMethod = GetMethodIDOrFail(env, declaringClass, methName, javaSig);
			info->fIsGenericCallback = isGenericCallback;
			
			info->fJNICallFunction = getJNICallFunction(env, (ValueType)returnValueType);

			(*env)->ReleaseStringUTFChars(env, javaSignature, javaSig);
			(*env)->ReleaseStringUTFChars(env, methodName, methName);
			(*env)->ReleaseStringUTFChars(env, dcSignature, dcSig);
			
			initCommonCallInfo(&info->fInfo, env, dcCallingConvention, nParams, returnValueType, paramsValueTypes, callIOs);
		}
	}
	return PTR_TO_JLONG(info);
}
JNIEXPORT jlong JNICALL Java_org_bridj_JNI_getActualCToJavaCallback(
	JNIEnv *env, 
	jclass clazz,
	jlong handle
) {
	struct NativeToJavaCallbackCallInfo* info = (struct NativeToJavaCallbackCallInfo*)JLONG_TO_PTR(handle);
	return PTR_TO_JLONG(info->fInfo.fDCCallback);
}
JNIEXPORT void JNICALL Java_org_bridj_JNI_freeCToJavaCallback(
	JNIEnv *env, 
	jclass clazz,
	jlong handle
) {
	struct NativeToJavaCallbackCallInfo* info = (struct NativeToJavaCallbackCallInfo*)JLONG_TO_PTR(handle);
	(*env)->DeleteGlobalRef(env, info->fCallbackInstance);
	freeCommon(env, &info->fInfo);
	free(info);
}


JNIEXPORT jlong JNICALL Java_org_bridj_JNI_bindJavaToCCallbacks(
	JNIEnv *env, 
	jclass clazz,
	jobjectArray methodCallInfos
) {
	BEGIN_INFOS_LOOP(JavaToNativeCallbackCallInfo)
	
	GetField_javaSignature()        ;
	GetField_dcSignature()          ;
	//GetField_symbolName()           ;
	//GetField_nativeClass()          ;
	GetField_methodName()           ;
	GetField_paramsValueTypes()     ;
	//GetField_javaCallback()         ;
	//GetField_forwardedPointer()     ;
	GetField_returnValueType()      ;
	//GetField_virtualIndex()         ;
	//GetField_virtualTableOffset()   ;
	GetField_dcCallingConvention()  ;
	//GetField_direct()               ;
	//GetField_startsWithThis()       ;
	//GetField_bNeedsThisPointer()    ;
	GetField_declaringClass()       ;
	GetField_nParams()              ;
	GetField_callIOs()              ;
	
	{
		//void* callback;
		const char* dcSig;
		
		// TODO DIRECT C++ virtual thunk
		dcSig = (*env)->GetStringUTFChars(env, dcSignature, NULL);
		info->fInfo.fDCCallback = dcbNewCallback(dcSig, JavaToCCallHandler/* NativeToJavaCallHandler*/, info);
		(*env)->ReleaseStringUTFChars(env, dcSignature, dcSig);
			
		initCommonCallInfo(&info->fInfo, env, dcCallingConvention, nParams, returnValueType, paramsValueTypes, callIOs);
		registerJavaFunction(env, declaringClass, methodName, javaSignature, 
#ifdef _DEBUG
			&info->fInfo,
#endif
			info->fInfo.fDCCallback);
	}
	END_INFOS_LOOP()
	return PTR_TO_JLONG(infos);
}
JNIEXPORT void JNICALL Java_org_bridj_JNI_freeJavaToCCallbacks(
	JNIEnv *env, 
	jclass clazz,
	jlong handle,
	jint size
) {
	JavaToNativeCallbackCallInfo* infos = (JavaToNativeCallbackCallInfo*)JLONG_TO_PTR(handle);
	jint i;
	if (!infos)
		return;
	for (i = 0; i < size; i++) {
		freeCommon(env, &infos[i].fInfo);
	}
	free(infos);
}

JNIEXPORT jlong JNICALL Java_org_bridj_JNI_bindJavaMethodsToCFunctions(
	JNIEnv *env, 
	jclass clazz,
	jobjectArray methodCallInfos
) {
	BEGIN_INFOS_LOOP(FunctionCallInfo)
	
	GetField_javaSignature()        ;
	GetField_dcSignature()          ;
	GetField_symbolName()           ;
	GetField_methodName()           ;
	GetField_paramsValueTypes()     ;
	GetField_forwardedPointer()     ;
	GetField_returnValueType()      ;
	GetField_dcCallingConvention()  ;
	GetField_direct()               ;
	GetField_isCPlusPlus()          ;
	GetField_isStatic()             ;
	GetField_startsWithThis()       ;
	GetField_declaringClass()       ;
	GetField_nParams()              ;
	GetField_callIOs()              ;
	
	{
		info->fForwardedSymbol = JLONG_TO_PTR(forwardedPointer);
		if (isCPlusPlus && !isStatic && declaringClass)
			info->fClass = (*env)->NewGlobalRef(env, declaringClass);
		
#ifndef NO_DIRECT_CALLS
		if (direct && forwardedPointer)
			info->fInfo.fDCCallback = (DCCallback*)dcRawCallAdapterSkipTwoArgs((void (*)())forwardedPointer, dcCallingConvention);
#endif
		if (!info->fInfo.fDCCallback) {
			const char* ds = (*env)->GetStringUTFChars(env, dcSignature, NULL);
			//info->fInfo.fDCCallback = dcbNewCallback(ds, JavaToFunctionCallHandler, info);
			info->fInfo.fDCCallback = dcbNewCallback(ds, isCPlusPlus && !isStatic ? JavaToCPPMethodCallHandler : JavaToFunctionCallHandler, info);
			(*env)->ReleaseStringUTFChars(env, dcSignature, ds);
		}
		initCommonCallInfo(&info->fInfo, env, dcCallingConvention, nParams, returnValueType, paramsValueTypes, callIOs);
		registerJavaFunction(env, declaringClass, methodName, javaSignature, 
#ifdef _DEBUG
			&info->fInfo,
#endif
			info->fInfo.fDCCallback);
	}
	END_INFOS_LOOP()
	return PTR_TO_JLONG(infos);
}
JNIEXPORT void JNICALL Java_org_bridj_JNI_freeCFunctionBindings(
	JNIEnv *env, 
	jclass clazz,
	jlong handle,
	jint size
) {
	FunctionCallInfo* infos = (FunctionCallInfo*)JLONG_TO_PTR(handle);
	jint i;
	if (!infos)
		return;
	for (i = 0; i < size; i++) {
		if (infos[i].fClass)
			(*env)->DeleteGlobalRef(env, infos[i].fClass);
		freeCommon(env, &infos[i].fInfo);
	}
	free(infos);
}
JNIEXPORT jlong JNICALL Java_org_bridj_JNI_bindJavaMethodsToObjCMethods(
	JNIEnv *env, 
	jclass clazz,
	jobjectArray methodCallInfos
) {
#if defined (DC__OS_Darwin)
	BEGIN_INFOS_LOOP(JavaToObjCCallInfo)
	
	GetField_javaSignature()        ;
	GetField_dcSignature()          ;
	GetField_symbolName()           ;
	GetField_nativeClass()          ;
	GetField_methodName()           ;
	GetField_paramsValueTypes()     ;
	//GetField_javaCallback()         ;
	//GetField_forwardedPointer()     ;
	GetField_returnValueType()      ;
	//GetField_virtualIndex()         ;
	//GetField_virtualTableOffset()   ;
	GetField_dcCallingConvention()  ;
	//GetField_direct()               ;
	//GetField_startsWithThis()       ;
	//GetField_bNeedsThisPointer()    ;
	GetField_declaringClass()       ;
	GetField_nParams()              ;
	GetField_callIOs()              ;
	
	{
		const char* ds, *methName;
	
		// TODO DIRECT ObjC thunk
		methName = (char*)(*env)->GetStringUTFChars(env, symbolName, NULL);
		ds = (*env)->GetStringUTFChars(env, dcSignature, NULL);
		
		info->fInfo.fDCCallback = dcbNewCallback(ds, JavaToObjCCallHandler, info);
		info->fSelector = sel_registerName(methName);
		info->fNativeClass = nativeClass;
		
		(*env)->ReleaseStringUTFChars(env, dcSignature, ds);
		(*env)->ReleaseStringUTFChars(env, symbolName, methName);
		
		
		initCommonCallInfo(&info->fInfo, env, dcCallingConvention, nParams, returnValueType, paramsValueTypes, callIOs);
		registerJavaFunction(env, declaringClass, methodName, javaSignature, info->fInfo.fDCCallback);
	}
	END_INFOS_LOOP()
	return PTR_TO_JLONG(infos);
#else
	return 0;
#endif
}

JNIEXPORT void JNICALL Java_org_bridj_JNI_freeObjCMethodBindings(
	JNIEnv *env, 
	jclass clazz,
	jlong handle,
	jint size
) {
#if defined (DC__OS_Darwin)
	JavaToObjCCallInfo* infos = (JavaToObjCCallInfo*)JLONG_TO_PTR(handle);
	jint i;
	if (!infos)
		return;
	for (i = 0; i < size; i++) {
		freeCommon(env, &infos[i].fInfo);
	}
	free(infos);
#endif
}


JNIEXPORT jlong JNICALL Java_org_bridj_JNI_bindJavaMethodsToVirtualMethods(
	JNIEnv *env, 
	jclass clazz,
	jobjectArray methodCallInfos
) {
	BEGIN_INFOS_LOOP(VirtualMethodCallInfo)
	
	GetField_javaSignature()        ;
	GetField_dcSignature()          ;
	GetField_symbolName()           ;
	GetField_methodName()           ;
	GetField_paramsValueTypes()     ;
	GetField_returnValueType()      ;
	GetField_virtualIndex()         ;
	GetField_virtualTableOffset()   ;
	GetField_dcCallingConvention()  ;
	GetField_startsWithThis()       ;
	//GetField_bNeedsThisPointer()    ;
	GetField_declaringClass()       ;
	GetField_nParams()              ;
	GetField_callIOs()              ;
	
	{
		const char* ds;
	
		info->fClass = (*env)->NewGlobalRef(env, declaringClass);
		info->fHasThisPtrArg = startsWithThis;
		info->fVirtualIndex = virtualIndex;
		info->fVirtualTableOffset = virtualTableOffset;
		
		// TODO DIRECT C++ virtual thunk
		ds = (*env)->GetStringUTFChars(env, dcSignature, NULL);
		info->fInfo.fDCCallback = dcbNewCallback(ds, JavaToVirtualMethodCallHandler, info);
		(*env)->ReleaseStringUTFChars(env, dcSignature, ds);
		
		
		initCommonCallInfo(&info->fInfo, env, dcCallingConvention, nParams, returnValueType, paramsValueTypes, callIOs);
		registerJavaFunction(env, declaringClass, methodName, javaSignature, 
#ifdef _DEBUG
			&info->fInfo,
#endif
			info->fInfo.fDCCallback);
	}
	END_INFOS_LOOP()
	return PTR_TO_JLONG(infos);
}
JNIEXPORT void JNICALL Java_org_bridj_JNI_freeVirtualMethodBindings(
	JNIEnv *env, 
	jclass clazz,
	jlong handle,
	jint size
) {
	VirtualMethodCallInfo* infos = (VirtualMethodCallInfo*)JLONG_TO_PTR(handle);
	jint i;
	if (!infos)
		return;
	for (i = 0; i < size; i++) {
		(*env)->DeleteGlobalRef(env, infos[i].fClass);
		freeCommon(env, &infos[i].fInfo);
	}
	free(infos);
}

#define FUNC_VOID_3(name, t1, t2, t3, nt1, nt2, nt3) \
void JNICALL Java_org_bridj_JNI_ ## name(JNIEnv *env, jclass clazz, t1 a1, t2 a2, t3 a3) \
{ \
	BEGIN_TRY(); \
	name((nt1)a1, (nt2)a2, (nt3)a3); \
	END_TRY(env); \
}

#define FUNC_3(ret, name, t1, t2, t3, nt1, nt2, nt3) \
ret JNICALL Java_org_bridj_JNI_ ## name(JNIEnv *env, jclass clazz, t1 a1, t2 a2, t3 a3) \
{ \
	BEGIN_TRY(); \
	return (ret)name((nt1)a1, (nt2)a2, (nt3)a3); \
	END_TRY_RET(env, (ret)0); \
}

#define FUNC_VOID_1(name, t1, nt1) \
void JNICALL Java_org_bridj_JNI_ ## name(JNIEnv *env, jclass clazz, t1 a1) \
{ \
	BEGIN_TRY(); \
	name((nt1)a1); \
	END_TRY(env); \
}

#define FUNC_1(ret, name, t1, nt1) \
ret JNICALL Java_org_bridj_JNI_ ## name(JNIEnv *env, jclass clazz, t1 a1) \
{ \
	BEGIN_TRY(); \
	return (ret)name((nt1)a1); \
	END_TRY_RET(env, (ret)0); \
}


jlong JNICALL Java_org_bridj_JNI_mallocNulled(JNIEnv *env, jclass clazz, jlong size) 
{
	size_t len = (size_t)size;
	void* p = malloc(len);
	if (p)
		memset(p, 0, len);
	return PTR_TO_JLONG(p);
}

FUNC_1(jlong, malloc, jlong, size_t)

FUNC_VOID_1(free, jlong, void*)

FUNC_1(jlong, strlen, jlong, char*)
FUNC_1(jlong, wcslen, jlong, wchar_t*)

FUNC_VOID_3(memcpy, jlong, jlong, jlong, void*, void*, size_t)
FUNC_VOID_3(memmove, jlong, jlong, jlong, void*, void*, size_t)

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
