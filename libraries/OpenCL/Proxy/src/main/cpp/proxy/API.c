#include "API.h"

jclass gOpenCLProxyInterface = NULL;
jmethodID gclGetPlatformIDsMethod = NULL;
jobject gOpenCLProxyImplementation = NULL;

#define PROXIED_CLASS_SIG "com/nativelibs4java/opencl/library/ProxiedOpenCLLibrary"

void bindJavaAPI(void *instanceData) {
  jclass proxyClass = NULL;
  jmethodID getProxyInstanceMethod = NULL;
  jmethodID setIcdDispatchTableMethod = NULL;
  JNIEnv *env = GetEnv();
  gOpenCLProxyInterface = GLOBAL_REF((*env)->FindClass(env, PROXY_INTERFACE_SIG));
  gclGetPlatformIDsMethod = (*env)->GetMethodID(env, proxyClass, "clGetPlatformIDs", "(ILL)I");
  
  proxyClass = (*env)->FindClass(env, PROXIED_CLASS_SIG);
  getProxyInstanceMethod = (*env)->GetStaticMethodID(env, proxyClass, "getInstance", "()L" PROXY_INTERFACE_SIG ";");
  setIcdDispatchTableMethod = (*env)->GetStaticMethodID(env, proxyClass, "setIcdDispatchTable", "(L)V" PROXIED_CLASS_SIG ";");
  
  (*env)->CallStaticVoidMethod(env, proxyClass, setIcdDispatchTableMethod, (jlong)(size_t)instanceData);
  gOpenCLProxyImplementation = GLOBAL_REF((*env)->CallStaticObjectMethod(env, proxyClass, getProxyInstanceMethod)); 
}

void unbindJavaAPI() {
  JNIEnv *env = GetEnv();
  DEL_GLOBAL_REF(gOpenCLProxyImplementation);
  DEL_GLOBAL_REF(gOpenCLProxyInterface);
}

