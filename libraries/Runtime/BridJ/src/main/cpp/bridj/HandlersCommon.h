#include "bridj.hpp"
#include <jni.h>
#include "Exceptions.h"

jboolean followArgs(JNIEnv* env, DCArgs* args, DCCallVM* vm, int nTypes, ValueType* pTypes);

jboolean followCall(JNIEnv* env, ValueType returnType, DCCallVM* vm, DCValue* result, void* callback);

void initVM(DCCallVM** vm);

jobject initCallHandler(DCArgs* args, DCCallVM** vmOut, JNIEnv** envOut);
