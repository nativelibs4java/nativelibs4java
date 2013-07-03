#include "Common.h"
#include "API.h"
#include "Proxy.h"

JavaVM* gJVM = NULL;
#define JNI_VERSION JNI_VERSION_1_4

JNIEnv* GetEnv() {
  JNIEnv* env = NULL;
  if ((*gJVM)->GetEnv(gJVM, (void*)&env, JNI_VERSION) != JNI_OK) {
    if ((*gJVM)->AttachCurrentThreadAsDaemon(gJVM, (void*)&env, NULL) != JNI_OK) {
	  printf("JavaCL Proxy: Cannot attach current JVM thread !\n");
      return NULL;
    }
  }
  return env;
}

struct _cl_icd_dispatch dispatch;

void createJVM() {
  JNIEnv *env;
  JavaVMInitArgs vm_args;
  JavaVMOption options;
  options.optionString = "-Djava.class.path=javacl-proxy.jar"; // TODO: change this
  vm_args.version = JNI_VERSION_1_6;
  vm_args.nOptions = 1;
  vm_args.options = &options;
  vm_args.ignoreUnrecognized = 0;
  JNI_CreateJavaVM(&gJVM, (void**)&env, &vm_args);
  // TODO: handle errors.
}
void initializeLibrary() {
  // Create JVM, based on env. OPENCL_PROXY_JAR
  createJVM();
  
  // Bind classes and methods.
  bindJavaAPI(&gJavaCLProxyDispatch);
}

void cleanupLibrary() {
  // Unbind classes and methods
  unbindJavaAPI();
  
  // Stop JVM
  (*gJVM)->DestroyJavaVM(gJVM);
}

