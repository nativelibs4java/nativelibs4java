#include "com_nativelibs4java_runtime_JNI.h"

#include "dyncallback/dyncall_callback.h"
#include "dynload/dynload.h"

#include "jdyncall.hpp"
#include <iostream>

using namespace std;

#define JNI_SIZEOF(type, escType) \
jint JNICALL Java_com_nativelibs4java_runtime_JNI_sizeOf_1 ## escType(JNIEnv *, jclass) { return sizeof(type); }

#define JNI_SIZEOF_t(type) JNI_SIZEOF(type ## _t, type ## _1t)

JNI_SIZEOF_t(size)
JNI_SIZEOF_t(wchar)
JNI_SIZEOF_t(ptrdiff)

void JNICALL Java_com_nativelibs4java_runtime_JNI_init(JNIEnv *env, jclass)
{
	DefineCommonClassesAndMethods(env);
}

class MethodCallInfos : public vector<MethodCallInfo*> {
public:
	~MethodCallInfos() {
		for (size_t i = size(); i--;) {
			delete (*this)[i];
		}
	}
} gMethodCallInfos;

void JNICALL Java_com_nativelibs4java_runtime_JNI_registerMethod(JNIEnv *env, jclass, jclass declaringClass, jobject method, jlong symbol)
{
	if (!symbol) {
		cerr << "No symbol !\n";
		return;
	}
	MethodCallInfo *info = new MethodCallInfo(env, declaringClass, method, (void*)(ptrdiff_t)symbol);

	gMethodCallInfos.push_back(info);

	JString name(env, (jstring)env->CallObjectMethod(method, Member_getName));
	
	JNINativeMethod meth;
	meth.fnPtr = info->GetCallback();
	meth.name = (char*)(const char*)name;
	meth.signature = (char*)info->GetJavaSignature().c_str();
	env->RegisterNatives(declaringClass, &meth, 1);
}
/*

void JNICALL Java_com_nativelibs4java_runtime_JNI_registerClass(JNIEnv *env, jclass, jclass declaringClass)
{
	jarray methods = (jarray)env->CallObjectMethod((jobject)declaringClass, Class_getDeclaredMethods);
	vector<jobject> methodsToBind;
	for (jsize iMethod = 0, nMethods = env->GetArrayLength(methods); iMethod < nMethods; iMethod++) {
		jobject method = env->GetObjectArrayElement((jobjectArray)methods, iMethod);
		jint modifiers = env->CallIntMethod(method, Member_getModifiers);
		//if (modifiers & 
		methodsToBind.push_back(method);
	}
	
	//DynCall_getSymbolAddress = env->GetMethodID(DynCall_class, "getSymbolAddress", "(Ljava/lang/reflect/AnnotatedElement;)J");
	
	for (size_t iMethod = 0, nMethods = methodsToBind.size(); iMethod < nMethods; iMethod++) {
		jobject method = methodsToBind[iMethod];
		method = env->CallObjectMethod((jobject)AnnotatedElement_class, Class_cast, method);
		void* symbol = (void*)env->CallStaticLongMethod(DynCall_class, DynCall_getSymbolAddress, method);
		registerMethod(env, declaringClass, method, symbol);
	}
}
*/


//char (DCCallbackHandler)(DCCallback* pcb, DCArgs* args, DCValue* result, void* userdata);

jlong JNICALL Java_com_nativelibs4java_runtime_JNI_getObjectPointer(JNIEnv *, jclass, jobject object)
{
	return (jlong)object;
}
 
jlong JNICALL Java_com_nativelibs4java_runtime_JNI_loadLibrary(JNIEnv *env, jclass, jstring pathStr)
{
	const char* path = env->GetStringUTFChars(pathStr, NULL);
	jlong ret = (jlong)dlLoadLibrary(path);
	env->ReleaseStringUTFChars(pathStr, path);
	return ret;
}

void JNICALL Java_com_nativelibs4java_runtime_JNI_freeLibrary(JNIEnv *, jclass, jlong libHandle)
{
	dlFreeLibrary((DLLib*)libHandle);
}

jlong JNICALL Java_com_nativelibs4java_runtime_JNI_findSymbolInLibrary(JNIEnv *env, jclass, jlong libHandle, jstring nameStr)
{
	const char* name = env->GetStringUTFChars(nameStr, NULL);
	jlong ret = (jlong)dlFindSymbol((DLLib*)libHandle, name);
	env->ReleaseStringUTFChars(nameStr, name);
	return ret;
}