#define DEFINE_COMMON_CLASS_AND_METHODS 1

#include "jdyncall.hpp"

using namespace std;

#define DEFINE_CLASS(pack, className) if (!(className ## _class = (jclass)env->NewGlobalRef(env->FindClass(pack "/" #className)))) cerr << "Class " << pack "/" #className << " not found !\n";
#define DEFINE_METHOD(className, methodName, sig) className ## _ ## methodName = env->GetMethodID(className ## _class, #methodName, sig);
#define DEFINE_STATIC_METHOD(className, methodName, sig) className ## _ ## methodName = env->GetStaticMethodID(className ## _class, #methodName, sig);

#define DEFINE_TYPE(className) \
	DEFINE_CLASS("java/lang", className); \
	className ## _TYPE = (jclass)env->NewGlobalRef(env->GetStaticObjectField(className ## _class, env->GetStaticFieldID(className ## _class, "TYPE", "Ljava/lang/Class;")));

#define DEFINE_ANN(className) DEFINE_CLASS("com/nativelibs4java/runtime/ann", className);

void DefineCommonClassesAndMethods(JNIEnv *env)
{	
	DEFINE_CLASS("java/lang", String);
	DEFINE_METHOD(String, toString, "()Ljava/lang/String;");

	DEFINE_CLASS("java/lang/reflect", Member);
	DEFINE_METHOD(Member, getName, "()Ljava/lang/String;");
	DEFINE_METHOD(Member, getModifiers, "()I");

	DEFINE_CLASS("java/lang/reflect", Method);
	DEFINE_METHOD(Method, getParameterTypes, "()[Ljava/lang/Class;");
	DEFINE_METHOD(Method, getParameterAnnotations, "()[[Ljava/lang/annotation/Annotation;");
	DEFINE_METHOD(Method, getReturnType, "()Ljava/lang/Class;");
	DEFINE_METHOD(Method, getGenericReturnType, "()Ljava/lang/reflect/Type;");
	DEFINE_METHOD(Method, isVarArgs, "()Z");

	DEFINE_CLASS("java/lang", Class);
	DEFINE_METHOD(Class, cast, "(Ljava/lang/Object;)Ljava/lang/Object;");
	DEFINE_METHOD(Class, getMethods, "()[Ljava/lang/reflect/Method;");
	DEFINE_METHOD(Class, getDeclaredMethods, "()[Ljava/lang/reflect/Method;");
	DEFINE_METHOD(Class, getComponentType, "()Ljava/lang/Class;");
	DEFINE_METHOD(Class, getName, "()Ljava/lang/String;");
	DEFINE_METHOD(Class, isArray, "()Z");
	DEFINE_METHOD(Class, isPrimitive, "()Z");

	DEFINE_TYPE(Void);
	DEFINE_TYPE(Integer);
	DEFINE_METHOD(Integer, intValue, "()I");
	DEFINE_TYPE(Long);
	DEFINE_METHOD(Long, longValue, "()J");
	DEFINE_TYPE(Short);
	DEFINE_METHOD(Short, shortValue, "()S");
	DEFINE_TYPE(Byte);
	DEFINE_METHOD(Byte, byteValue, "()B");
	DEFINE_TYPE(Float);
	DEFINE_METHOD(Float, floatValue, "()F");
	DEFINE_TYPE(Double);
	DEFINE_METHOD(Double, doubleValue, "()D");
	DEFINE_TYPE(Character);
	DEFINE_METHOD(Character, charValue, "()C");
	DEFINE_TYPE(Boolean);
	DEFINE_METHOD(Boolean, booleanValue, "()Z");
	
	DEFINE_CLASS("java/nio", IntBuffer);
	DEFINE_METHOD(IntBuffer, array, "()[I");
	DEFINE_CLASS("java/nio", LongBuffer);
	DEFINE_METHOD(LongBuffer, array, "()[J");
	DEFINE_CLASS("java/nio", ShortBuffer);
	DEFINE_METHOD(ShortBuffer, array, "()[S");
	DEFINE_CLASS("java/nio", ByteBuffer);
	DEFINE_METHOD(ByteBuffer, array, "()[B");
	DEFINE_CLASS("java/nio", FloatBuffer);
	DEFINE_METHOD(FloatBuffer, array, "()[F");
	DEFINE_CLASS("java/nio", DoubleBuffer);
	DEFINE_METHOD(DoubleBuffer, array, "()[D");
	DEFINE_CLASS("java/nio", CharBuffer);
	DEFINE_METHOD(CharBuffer, array, "()[C");
	
	DEFINE_CLASS("java/lang", RuntimeException);

	DEFINE_CLASS("com/nativelibs4java/runtime", Addressable);
	DEFINE_METHOD(Addressable, getAddress, "()J");
	DEFINE_METHOD(Addressable, setAddress, "(J)V");

	DEFINE_CLASS("com/nativelibs4java/runtime", Callback);
	DEFINE_CLASS("com/nativelibs4java/runtime", UnmappableTypeException);

	DEFINE_CLASS("java/lang/reflect", AnnotatedElement);


	DEFINE_CLASS("com/nativelibs4java/runtime", AddressableFactory);
	DEFINE_METHOD(AddressableFactory, newInstance, "(J)Lcom/nativelibs4java/runtime/Addressable;");
	
	DEFINE_CLASS("com/nativelibs4java/runtime", DynCall);
	DEFINE_STATIC_METHOD(DynCall, newAddressableFactory, "(Ljava/lang/Class;Ljava/lang/reflect/Type;)Lcom/nativelibs4java/runtime/AddressableFactory;");
	DEFINE_STATIC_METHOD(DynCall, getSymbolAddress, "(Ljava/lang/reflect/AnnotatedElement;)J");
	DEFINE_STATIC_METHOD(DynCall, getAnnotation, "(Ljava/lang/Class;Ljava/lang/reflect/AnnotatedElement;[Ljava/lang/annotation/Annotation;)Ljava/lang/annotation/Annotation;");

	DEFINE_CLASS("com/nativelibs4java/runtime", CPPObject);

	DEFINE_ANN(Wide);
	DEFINE_ANN(Index);
	DEFINE_METHOD(Index, value, "()I");
	DEFINE_ANN(Const);
	DEFINE_ANN(NativeSize);
	DEFINE_ANN(CLong);
	DEFINE_ANN(Virtual);
	DEFINE_ANN(ByValue);
	DEFINE_ANN(Array);
	DEFINE_METHOD(Array, value, "()I");
	DEFINE_ANN(CallingConvention);
	DEFINE_ANN(Mangling);
	DEFINE_ANN(Library);
}