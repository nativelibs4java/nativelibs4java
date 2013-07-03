#include "API.h"

jclass gOpenCLProxyInterface = NULL;
jmethodID gclGetPlatformIDsMethod = NULL;
jobject gOpenCLProxyImplementation = NULL;

void bindJavaAPI(void *instanceData) {
  jclass proxyClass = NULL;
  jmethodID getProxyInstanceMethod = NULL;
  JNIEnv *env = GetEnv();
  gOpenCLProxyInterface = GLOBAL_REF((*env)->FindClass(env, PROXY_INTERFACE_SIG));
  gclGetPlatformIDsMethod = (*env)->GetMethodID(env, proxyClass, "clGetPlatformIDs", "(ILL)I");
  
  proxyClass = (*env)->FindClass(env, "com/nativelibs4java/opencl/library/Proxy");
  getProxyInstanceMethod = (*env)->GetStaticMethodID(env, proxyClass, "getInstance", "(L)L" PROXY_INTERFACE_SIG ";");
  gOpenCLProxyImplementation = GLOBAL_REF((*env)->CallStaticObjectMethod(env, proxyClass, getProxyInstanceMethod, (jlong)(size_t)instanceData)); 
}

void unbindJavaAPI() {
  JNIEnv *env = GetEnv();
  DEL_GLOBAL_REF(gOpenCLProxyImplementation);
  DEL_GLOBAL_REF(gOpenCLProxyInterface);
}

