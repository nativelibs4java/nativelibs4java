#include "bridj.hpp"
#include <jni.h>
#include "Exceptions.h"

jboolean followArgs(JNIEnv* env, DCArgs* args, CallTempStruct* call, int nTypes, ValueType* pTypes);

jboolean followCall(JNIEnv* env, ValueType returnType, CallTempStruct* call, DCValue* result, void* callback);

jobject initCallHandler(DCArgs* args, CallTempStruct** callOut, JNIEnv** envOut);
void cleanupCallHandler(JNIEnv* env, CallTempStruct* call);
