#include "bridj.hpp"
#include <jni.h>
#include "Exceptions.h"

jboolean followArgs(CallTempStruct* call, DCArgs* args, int nTypes, ValueType* pTypes);

jboolean followCall(CallTempStruct* call, ValueType returnType, DCValue* result, void* callback);

jobject initCallHandler(DCArgs* args, CallTempStruct** callOut, JNIEnv* env);
void cleanupCallHandler(CallTempStruct* call);
