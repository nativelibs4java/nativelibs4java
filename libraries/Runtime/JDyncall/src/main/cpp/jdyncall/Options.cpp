#include "jdyncall.hpp"

jobject GetAnnotation(JNIEnv *env, jclass annotationClass, jobject annotatedElement, jobjectArray extraAnnotations = NULL) {
	return env->CallStaticObjectMethod(DynCall_class, DynCall_getAnnotation, annotationClass, annotatedElement, extraAnnotations);
}

bool GetAnnotationInt(JNIEnv *env, jclass annotationClass, jmethodID valueMethod, jint& out, jobject annotatedElement, jobjectArray extraAnnotations = NULL) {
	jobject index = GetAnnotation(env, annotationClass, annotatedElement, extraAnnotations);
	if (!index)
		return false;
	out = env->CallIntMethod(index, valueMethod);
	return true;
}

bool HasAnnotation(JNIEnv *env, jclass annotationClass, jobject annotatedElement, jobjectArray extraAnnotations = NULL) {
	return GetAnnotation(env, annotationClass, annotatedElement, extraAnnotations) != NULL;
}


void GetOptions(JNIEnv *env, Options &out, jobject annotatedElement, jobjectArray extraAnnotations = NULL) {
#define SET_BOOL_ANNOTATION_OPTION(annClass, optField) \
	if (HasAnnotation(env, annClass ## _class, annotatedElement, extraAnnotations)) \
		out.optField = true;
#define SET_INT_ANNOTATION_OPTION(annClass, valueMeth, optField) \
	{ \
		jint value; \
		if (GetAnnotationInt(env, annClass ## _class, valueMeth, value, annotatedElement, extraAnnotations)) \
			out.optField = value; \
	}

	SET_BOOL_ANNOTATION_OPTION(Wide, bIsWideChar);
	SET_BOOL_ANNOTATION_OPTION(ByValue, bIsByValue);
	SET_BOOL_ANNOTATION_OPTION(Virtual, bIsVirtual);
	SET_BOOL_ANNOTATION_OPTION(Const, bIsConst);
	SET_BOOL_ANNOTATION_OPTION(NativeSize, bIsSizeT);
	SET_BOOL_ANNOTATION_OPTION(CLong, bIsCLong);
	
	SET_INT_ANNOTATION_OPTION(Field, Field_value, index);
}

void GetFieldOptions(JNIEnv *env, FieldOptions &out, jobject annotatedElement, jobjectArray extraAnnotations = NULL) {
	GetOptions(env, out, annotatedElement, extraAnnotations);
	SET_INT_ANNOTATION_OPTION(Length, Length_value, arraySize);
}