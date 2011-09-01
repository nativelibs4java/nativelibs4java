#ifndef _JNI_H
#define _JNI_H

#define FIND_GLOBAL_CLASS(name) (*env)->NewGlobalRef(env, (*env)->FindClass(env, name))

void initMethods(JNIEnv* env);

#endif // _JNI_H
