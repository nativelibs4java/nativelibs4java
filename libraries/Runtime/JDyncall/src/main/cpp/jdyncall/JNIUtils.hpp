#include "jni.h"
//#include <string>

class JString {
	JNIEnv *env;
	jstring s;
	const char* chars;
public:
	JString(JNIEnv *env_, jstring s_);
	~JString();
	
	operator const char*() const {
		return chars;
	}
	size_t length() const;
};

//std::string GetString(JNIEnv *env, jobject o);