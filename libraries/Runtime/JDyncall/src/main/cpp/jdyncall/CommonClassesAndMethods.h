#ifdef DEFINE_COMMON_CLASS_AND_METHODS
	#define EXTERN_CLASS_AND_METHODS
#else
	#define EXTERN_CLASS_AND_METHODS extern
#endif

void DefineCommonClassesAndMethods(JNIEnv *env);

#define DECLARE_CLASS(className) EXTERN_CLASS_AND_METHODS jclass className ## _class;
#define DECLARE_METHOD(className, methodName) EXTERN_CLASS_AND_METHODS jmethodID className ## _ ## methodName;
#define DECLARE_TYPE(className) EXTERN_CLASS_AND_METHODS jclass className ## _class;  EXTERN_CLASS_AND_METHODS jclass className ## _TYPE;
DECLARE_CLASS(Object);
DECLARE_METHOD(Object, toString);

DECLARE_CLASS(String);
DECLARE_METHOD(String, toString);

DECLARE_CLASS(Member);
DECLARE_METHOD(Member, getName);
DECLARE_METHOD(Member, getModifiers);

DECLARE_CLASS(Method);
DECLARE_METHOD(Method, getParameterTypes);
DECLARE_METHOD(Method, getParameterAnnotations);
DECLARE_METHOD(Method, getReturnType);
DECLARE_METHOD(Method, getGenericReturnType);
DECLARE_METHOD(Method, isVarArgs);

DECLARE_CLASS(Class);
DECLARE_METHOD(Class, cast);
DECLARE_METHOD(Class, getMethods);
DECLARE_METHOD(Class, getDeclaredMethods);
DECLARE_METHOD(Class, getComponentType);
DECLARE_METHOD(Class, getName);
DECLARE_METHOD(Class, isArray);
DECLARE_METHOD(Class, isPrimitive);

DECLARE_TYPE(Void);
DECLARE_TYPE(Integer);
DECLARE_METHOD(Integer, intValue);
DECLARE_TYPE(Long);
DECLARE_METHOD(Long, longValue);
DECLARE_TYPE(Short);
DECLARE_METHOD(Short, shortValue);
DECLARE_TYPE(Byte);
DECLARE_METHOD(Byte, byteValue);
DECLARE_TYPE(Float);
DECLARE_METHOD(Float, floatValue);
DECLARE_TYPE(Double);
DECLARE_METHOD(Double, doubleValue);
DECLARE_TYPE(Character);
DECLARE_METHOD(Character, charValue);
DECLARE_TYPE(Boolean);
DECLARE_METHOD(Boolean, booleanValue);

DECLARE_CLASS(IntBuffer);
DECLARE_METHOD(IntBuffer, array);
DECLARE_CLASS(LongBuffer);
DECLARE_METHOD(LongBuffer, array);
DECLARE_CLASS(ShortBuffer);
DECLARE_METHOD(ShortBuffer, array);
DECLARE_CLASS(ByteBuffer);
DECLARE_METHOD(ByteBuffer, array);
DECLARE_CLASS(FloatBuffer);
DECLARE_METHOD(FloatBuffer, array);
DECLARE_CLASS(DoubleBuffer);
DECLARE_METHOD(DoubleBuffer, array);
DECLARE_CLASS(CharBuffer);
DECLARE_METHOD(CharBuffer, array);

DECLARE_CLASS(RuntimeException);

DECLARE_CLASS(Pointable);
DECLARE_METHOD(Pointable, getPointer);

DECLARE_CLASS(PointerRefreshable);
DECLARE_METHOD(PointerRefreshable, setPointer);

DECLARE_CLASS(Callback);

DECLARE_CLASS(UnmappableTypeException);

DECLARE_CLASS(AnnotatedElement);

DECLARE_CLASS(AddressableFactory);
DECLARE_METHOD(AddressableFactory, newInstance);
	
DECLARE_CLASS(DynCall);
DECLARE_METHOD(DynCall, newAddressableFactory);
DECLARE_METHOD(DynCall, getSymbolAddress);
DECLARE_METHOD(DynCall, getAnnotation);

DECLARE_CLASS(CPPObject);

DECLARE_CLASS(Wide);
DECLARE_CLASS(Field);
DECLARE_METHOD(Field, value);
DECLARE_CLASS(Const);
DECLARE_CLASS(NativeSize);
DECLARE_CLASS(CLong);
DECLARE_CLASS(Virtual);
DECLARE_CLASS(ByValue);
DECLARE_CLASS(Length);
DECLARE_METHOD(Length, value);
DECLARE_CLASS(CallingConvention);
DECLARE_CLASS(Mangling);
DECLARE_CLASS(Library);