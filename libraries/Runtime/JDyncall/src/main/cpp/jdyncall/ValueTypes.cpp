#include "jdyncall.hpp"

using namespace std;

/*
ValueType GetValueType(JNIEnv *env, jobject obj, ValueType *typeConstraint)
{
	if (!obj)
		return typeConstraint ? *typeConstraint : eAddressableValue;

	cerr << "Implement me : GetType\n";
	jclass c = env->GetObjectClass(obj);
	return GetValueType(env, c);
}
ValueType GetValueType(JNIEnv *env, jclass c)
{
	#define GETVALUETYPE_CASE(cl, type) \
	if (env->IsAssignableFrom(c, cl ## _class)) \
		return type;

	GETVALUETYPE_CASE(Addressable, eAddressableValue);
	GETVALUETYPE_CASE(Callback, eCallbackValue);
	GETVALUETYPE_CASE(Integer, eIntValue);
	GETVALUETYPE_CASE(Long, eLongValue);
	GETVALUETYPE_CASE(Short, eShortValue);
	GETVALUETYPE_CASE(Byte, eByteValue);
	GETVALUETYPE_CASE(Float, eFloatValue);
	GETVALUETYPE_CASE(Double, eDoubleValue);

	env->ThrowNew(UnmappableTypeException_class, "");
	return (ValueType)-1;
}
*/