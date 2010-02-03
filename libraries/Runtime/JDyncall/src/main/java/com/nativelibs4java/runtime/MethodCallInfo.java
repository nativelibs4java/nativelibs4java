/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.runtime;
import java.io.FileNotFoundException;
import java.lang.annotation.Annotation;
import static com.nativelibs4java.runtime.JDyncallLibrary.*;
import static com.nativelibs4java.runtime.DyncallSignatures.*;
import com.nativelibs4java.runtime.*;
import com.nativelibs4java.runtime.ann.*;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
/**
 *
 * @author Olivier
 */
public class MethodCallInfo {

    /*public static class GenericMethodInfo {
        Type returnType, paramsTypes[];
    }
    GenericMethodInfo genericInfo = new GenericMethodInfo();*/
    int returnValueType, paramsValueTypes[];
    Options methodOptions, paramsOptions[];
	Method method;
	long forwardedPointer;
    String dcSignature;
	String javaSignature;
    int dcCallingConvention;

	boolean isVarArgs;
	boolean isStatic;
	boolean isCPlusPlus;
	
	boolean direct;

    boolean isCallableAsRaw;

	public MethodCallInfo(Method method, long libraryHandle) throws FileNotFoundException {
        isVarArgs = false;
        isCPlusPlus = false;
        dcCallingConvention = 0;
        this.method = method;
        forwardedPointer = DynCall.getSymbolAddress(libraryHandle, method);

        Class<?>[] paramsTypes = method.getParameterTypes();
        Annotation[][] paramsAnnotations = method.getParameterAnnotations();
        /*genericInfo.returnType = method.getGenericReturnType();
        genericInfo.paramsTypes = method.getGenericParameterTypes();*/
        
        int modifiers = method.getModifiers();
        isStatic = Modifier.isStatic(modifiers);
        isVarArgs = method.isVarArgs();

        int nParams = paramsTypes.length;
        paramsValueTypes = new int[nParams];
        paramsOptions = new Options[nParams];

        this.direct = nParams <= JNI.getMaxDirectMappingArgCount();
        
        isCallableAsRaw = true; // TODO on native side : test number of parameters (on 64 bits win : must be <= 4)
        isCPlusPlus = CPPObject.class.isAssignableFrom(method.getDeclaringClass());

        GetOptions(methodOptions, method);

        StringBuilder javaSig = new StringBuilder(64), dcSig = new StringBuilder(16);
        javaSig.append('(');
        dcSig.append(DC_SIGCHAR_POINTER).append(DC_SIGCHAR_POINTER); // JNIEnv*, jobject: always present in native-bound functions

        for (int iParam = 0; iParam < nParams; iParam++) {
            Options paramOptions = paramsOptions[iParam] = new Options();
            Class<?> param = paramsTypes[iParam];

            ValueType paramValueType = getValueType(param, null, paramsAnnotations[iParam]);
            paramsValueTypes[iParam] = paramValueType.ordinal();
            GetOptions(paramOptions, method, paramsAnnotations[iParam]);

            appendToSignature(paramValueType, javaSig, dcSig);
        }
        javaSig.append(')');
        dcSig.append(')');

        ValueType retType = getValueType(method.getReturnType(), method);
        appendToSignature(retType, javaSig, dcSig);
        returnValueType = retType.ordinal();

        javaSignature = javaSig.toString();
        dcSignature = dcSig.toString();
    }
	
	public String getDcSignature() {
		return dcSignature;
	}
	public String getJavaSignature() {
		return javaSignature;
	}
    boolean getBoolAnnotation(Class<? extends Annotation> ac, AnnotatedElement element, Annotation... directAnnotations) {
        Annotation ann = DynCall.getAnnotation(ac, element, directAnnotations);
        return ann != null;
    }
    private void GetOptions(Options out, Method method, Annotation... directAnnotations) {
        out.bIsWideChar = getBoolAnnotation(Wide.class, method, directAnnotations);
        out.bIsByValue = getBoolAnnotation(ByValue.class, method, directAnnotations);
        out.bIsConst = getBoolAnnotation(Const.class, method, directAnnotations);
        out.bIsSizeT = getBoolAnnotation(PointerSized.class, method, directAnnotations);
        out.bIsCLong = getBoolAnnotation(CLong.class, method, directAnnotations);

        Virtual virtual = DynCall.getAnnotation(Virtual.class, method, directAnnotations);
        out.virtualIndex = virtual == null ? -1 : virtual.value();
    }

    public static class UnmappableMethod extends RuntimeException {
        public UnmappableMethod(Method method) {
            super("Method " + method + " cannot be used as a native mapping");
        }
    }

    public ValueType getValueType(Class<?> c, AnnotatedElement element, Annotation... directAnnotations) {
        if (c == null || c.equals(Void.TYPE))
            return ValueType.eVoidValue;
        if (c == Integer.class || c == Integer.TYPE)
            return ValueType.eIntValue;
        if (c == Long.class || c == Long.TYPE) {
            PointerSized sz = DynCall.getAnnotation(PointerSized.class, element, directAnnotations);
            if (sz != null) {
                isCallableAsRaw = false;
                return ValueType.eSizeTValue;
            }
            return ValueType.eLongValue;
        }
        if (c == Short.class || c == Short.TYPE)
            return ValueType.eShortValue;
        if (c == Byte.class || c == Byte.TYPE)
            return ValueType.eByteValue;
        if (c == Float.class || c == Float.TYPE)
            return ValueType.eFloatValue;
        if (c == Double.class || c == Double.TYPE)
            return ValueType.eDoubleValue;
        if (c == Boolean.class || c == Boolean.TYPE)
            return ValueType.eByteValue;

        throw new NoSuchElementException("No " + ValueType.class.getSimpleName() + " for class " + c.getName());
    }

    public void appendToSignature(ValueType type, StringBuilder javaSig, StringBuilder dcSig) {
        char dcChar, javaChar;
        switch (type) {
            case eVoidValue:
                dcChar = DC_SIGCHAR_VOID;
                javaChar = 'V';
                break;
            case eIntValue:
                dcChar = DC_SIGCHAR_INT;
                javaChar = 'I';
                break;
            case eSizeTValue:
                if (JNI.SIZE_T_SIZE == 8) {
                    dcChar = DC_SIGCHAR_LONGLONG;
                    javaChar = 'J';
                } else {
                    dcChar = DC_SIGCHAR_INT;
                    javaChar = 'I';
                    isCallableAsRaw = false;
                }
                break;
            case eShortValue:
                dcChar = DC_SIGCHAR_SHORT;
                javaChar = 'S';
                break;
            case eDoubleValue:
                dcChar = DC_SIGCHAR_DOUBLE;
                javaChar = 'D';
                break;
            case eFloatValue:
                dcChar = DC_SIGCHAR_FLOAT;
                javaChar = 'F';
                break;
            case eBoolValue:
                dcChar = DC_SIGCHAR_CHAR;
                javaChar = 'Z';
                break;
            case eByteValue:
                dcChar = DC_SIGCHAR_CHAR;
                javaChar = 'B';
                break;
            case eWCharTValue:
                switch (JNI.WCHAR_T_SIZE) {
                case 1:
                    dcChar = DC_SIGCHAR_CHAR;
                    isCallableAsRaw = false;
                    break;
                case 2:
                    dcChar = DC_SIGCHAR_SHORT;
                    break;
                case 4:
                    dcChar = DC_SIGCHAR_INT;
                    isCallableAsRaw = false;
                    break;
                default:
                    throw new RuntimeException("Unhandled sizeof(wchar_t) in GetJavaTypeSignature: " + JNI.WCHAR_T_SIZE);
                }
                javaChar = 'C';
                break;
            default:
                throw new RuntimeException("Unhandled " + ValueType.class.getSimpleName() + ": " + type);
        }
        if (javaSig != null)
            javaSig.append(javaChar);
        if (dcSig != null)
            dcSig.append(dcChar);
    }

}
