#include "jdyncall.hpp"

jobject GetAnnotation(JNIEnv *env, jclass annotationClass, jobject annotatedElement, jobjectArray extraAnnotations = NULL);

bool GetAnnotationInt(JNIEnv *env, jclass annotationClass, jmethodID valueMethod, jint& out, jobject annotatedElement, jobjectArray extraAnnotations = NULL);

bool HasAnnotation(JNIEnv *env, jclass annotationClass, jobject annotatedElement, jobjectArray extraAnnotations = NULL);

void GetOptions(JNIEnv *env, Options &out, jobject annotatedElement, jobjectArray extraAnnotations = NULL);

void GetFieldOptions(JNIEnv *env, FieldOptions &out, jobject annotatedElement, jobjectArray extraAnnotations = NULL);