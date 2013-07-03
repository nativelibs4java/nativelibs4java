#include "Common.h"
#include "API.h"
#include "Proxy.h"

JavaVM* gJVM = NULL;
#define JNI_VERSION JNI_VERSION_1_4

JNIEnv* GetEnv() {
  JNIEnv* env = NULL;
  if ((*gJVM)->GetEnv(gJVM, (void*)&env, JNI_VERSION) != JNI_OK) {
    if ((*gJVM)->AttachCurrentThreadAsDaemon(gJVM, (void*)&env, NULL) != JNI_OK) {
	  printf("BridJ: Cannot attach current JVM thread !\n");
      return NULL;
    }
  }
  return env;
}

struct _cl_icd_dispatch dispatch;

void initializeLibrary() {
  // Create JVM, based on env. OPENCL_PROXY_JAR
  // ...
  
  // Bind classes and methods.
  bindJavaAPI(&gJavaCLProxyDispatch);
}

void cleanupLibrary() {
  // Unbind classes and methods
  unbindJavaAPI();
  
  // Stop JVM
  // ...
}

