#include "JNIUtils.hpp"
#include <string>

/*string GetString(JNIEnv *env, jobject o) {
	jstring js = (jstring)o;
	const char* str = env->GetStringUTFChars(js, NULL);
	string s(str);
	env->ReleaseStringUTFChars(js, str);
	return s;
}*/

JString::JString(JNIEnv *env_, jstring s_) : s(s_), env(env_) {
	chars = env->GetStringUTFChars(s, NULL);
}
JString::~JString() {
	env->ReleaseStringUTFChars(s, chars);
}
size_t JString::length() const {
	return strlen(chars);
}