package com.bridj;
import java.io.FileNotFoundException;
import java.lang.annotation.Annotation;
import static com.bridj.Dyncall.*;
import static com.bridj.Dyncall.CallingConvention.*;

import com.bridj.ann.Constructor;
import com.bridj.ann.Convention;
import static com.bridj.Dyncall.SignatureChars.*;
import com.bridj.*;
import com.bridj.ann.*;
import com.bridj.cpp.CPPObject;

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
	private Class<?> declaringClass;
    int returnValueType, paramsValueTypes[];
	private Method method;
	String methodName, symbolName;
	private long forwardedPointer;
    String dcSignature;
	String javaSignature;
	Callback javaCallback;
	private int virtualIndex = -1;
	int virtualTableOffset = 0;
    private int dcCallingConvention = DC_CALL_C_DEFAULT;

	boolean isVarArgs;
	boolean isStatic;
    boolean isCPlusPlus;
	boolean direct;
	boolean startsWithThis;

	public MethodCallInfo(Method method) throws FileNotFoundException {
        isVarArgs = false;
        this.setMethod(method);
		this.setDeclaringClass(method.getDeclaringClass());
		this.methodName = method.getName();
        
        Class<?>[] paramsTypes = method.getParameterTypes();
        Annotation[][] paramsAnnotations = method.getParameterAnnotations();
        /*genericInfo.returnType = method.getGenericReturnType();
        genericInfo.paramsTypes = method.getGenericParameterTypes();*/
        
        int modifiers = method.getModifiers();
        isStatic = Modifier.isStatic(modifiers);
        isVarArgs = method.isVarArgs();

        int nParams = paramsTypes.length;
        paramsValueTypes = new int[nParams];

        direct = true; // TODO on native side : test number of parameters (on 64 bits win : must be <= 4)
        //isCPlusPlus = CPPObject.class.isAssignableFrom(method.getDeclaringClass());

        //GetOptions(methodOptions, method);

        StringBuilder javaSig = new StringBuilder(64), dcSig = new StringBuilder(16);
        javaSig.append('(');
        dcSig.append(DC_SIGCHAR_POINTER).append(DC_SIGCHAR_POINTER); // JNIEnv*, jobject: always present in native-bound functions

        for (int iParam = 0; iParam < nParams; iParam++) {
//            Options paramOptions = paramsOptions[iParam] = new Options();
            Class<?> param = paramsTypes[iParam];

            ValueType paramValueType = getValueType(iParam, param, null, paramsAnnotations[iParam]);
            paramsValueTypes[iParam] = paramValueType.ordinal();
            //GetOptions(paramOptions, method, paramsAnnotations[iParam]);

            appendToSignature(paramValueType, javaSig, dcSig);
        }
        javaSig.append(')');
        dcSig.append(')');

        ValueType retType = getValueType(-1, method.getReturnType(), method);
        appendToSignature(retType, javaSig, dcSig);
        returnValueType = retType.ordinal();

        javaSignature = javaSig.toString();
        dcSignature = dcSig.toString();
        
        
        Virtual virtual = BridJ.getAnnotation(Virtual.class, false, method);
        isCPlusPlus = isCPlusPlus || virtual != null;
        
        if (isCPlusPlus) {
        	if (JNI.isWindows()) {
        		if (!JNI.is64Bits())
        			setDcCallingConvention(DC_CALL_C_X86_WIN32_THIS_MS);
        	} else {
        		if (!JNI.is64Bits())
        			setDcCallingConvention(DC_CALL_C_X86_WIN32_THIS_GNU);
        	}
        }
        Convention cc = BridJ.getAnnotation(Convention.class, false, method);
        if (cc != null) {
            switch (cc.value()) {
            //case Auto:
            //	break;
            case FastCall:
                setDcCallingConvention(JNI.isWindows() ? DC_CALL_C_X86_WIN32_FAST_MS : DC_CALL_C_DEFAULT); // TODO allow GCC-compiled C++ libs on windows
                break;
            case Pascal:
            case StdCall:
                setDcCallingConvention(DC_CALL_C_X86_WIN32_STD);
                break;
            case ThisCall:
                setDcCallingConvention(JNI.isWindows() ? DC_CALL_C_X86_WIN32_THIS_GNU : DC_CALL_C_DEFAULT);
            }
        }

        if (nParams <= JNI.getMaxDirectMappingArgCount())
            this.direct = false;

        symbolName = methodName;
        //this.direct = false; // TODO remove me !
    }
	

	public String getDcSignature() {
		return dcSignature;
	}
	public String getJavaSignature() {
		return javaSignature;
	}
    boolean getBoolAnnotation(Class<? extends Annotation> ac, boolean inherit, AnnotatedElement element, Annotation... directAnnotations) {
        Annotation ann = BridJ.getAnnotation(ac, inherit, element, directAnnotations);
        return ann != null;
    }
    public ValueType getValueType(int iParam, Class<?> c, AnnotatedElement element, Annotation... directAnnotations) {
    	Ptr sz = BridJ.getAnnotation(Ptr.class, true, element, directAnnotations);
    	Constructor cons = this.method.getAnnotation(Constructor.class);
    	//This th = BridJ.getAnnotation(This.class, true, element, directAnnotations);
    	CLong cl = BridJ.getAnnotation(CLong.class, true, element, directAnnotations);
        
    	if (sz != null || cons != null || cl != null) {
    		if (!(c == Long.class || c == Long.TYPE))
    			throw new RuntimeException("Annotation should only be used on a long parameter, not on a " + c.getName());
    		
    		if (sz != null) {
                if (!JNI.is64Bits())
                    direct = false;
            } else if (cl != null) {
                if (JNI.CLONG_SIZE != 8)
                    direct = false;
            } else if (cons != null) {
            	isCPlusPlus = true;
				startsWithThis = true;
				if (iParam != 0)
					throw new RuntimeException("Annotation " + Constructor.class.getName() + " cannot have more than one (long) argument");
            }
    	    return ValueType.eSizeTValue;
    	}
    	if (c == null || c.equals(Void.TYPE))
            return ValueType.eVoidValue;
        if (c == Integer.class || c == Integer.TYPE)
            return ValueType.eIntValue;
        if (c == Long.class || c == Long.TYPE) {
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
        if (c == Pointer.class) {
            direct = false;
        	return ValueType.ePointerValue;
        }

        throw new NoSuchElementException("No " + ValueType.class.getSimpleName() + " for class " + c.getName());
    }

    public void appendToSignature(ValueType type, StringBuilder javaSig, StringBuilder dcSig) {
        char dcChar;
        String javaChar;
        switch (type) {
            case eVoidValue:
                dcChar = DC_SIGCHAR_VOID;
                javaChar = "V";
                break;
            case eIntValue:
                dcChar = DC_SIGCHAR_INT;
                javaChar = "I";
                break;
            case eLongValue:
                dcChar = DC_SIGCHAR_LONGLONG;
                javaChar = "J";
                break;
            case eSizeTValue:
                javaChar = "J";
				if (JNI.SIZE_T_SIZE == 8) {
                    dcChar = DC_SIGCHAR_LONGLONG;
                } else {
                    dcChar = DC_SIGCHAR_INT;
                    direct = false;
                }
                break;
            case eShortValue:
                dcChar = DC_SIGCHAR_SHORT;
                javaChar = "S";
                break;
            case eDoubleValue:
                dcChar = DC_SIGCHAR_DOUBLE;
                javaChar = "D";
                break;
            case eFloatValue:
                dcChar = DC_SIGCHAR_FLOAT;
                javaChar = "F";
                break;
            case eByteValue:
                dcChar = DC_SIGCHAR_CHAR;
                javaChar = "B";
                break;
            case eWCharValue:
                switch (JNI.WCHAR_T_SIZE) {
                case 1:
                    dcChar = DC_SIGCHAR_CHAR;
                    direct = false;
                    break;
                case 2:
                    dcChar = DC_SIGCHAR_SHORT;
                    break;
                case 4:
                    dcChar = DC_SIGCHAR_INT;
                    direct = false;
                    break;
                default:
                    throw new RuntimeException("Unhandled sizeof(wchar_t) in GetJavaTypeSignature: " + JNI.WCHAR_T_SIZE);
                }
                javaChar = "C";
                break;
            case ePointerValue:
            	dcChar = DC_SIGCHAR_POINTER;
                javaChar = "Lcom/bridj/Pointer;";
                direct = false;
            	break;
            default:
                direct = false;
                throw new RuntimeException("Unhandled " + ValueType.class.getSimpleName() + ": " + type);
        }
        if (javaSig != null)
            javaSig.append(javaChar);
        if (dcSig != null)
            dcSig.append(dcChar);
    }


	public void setMethod(Method method) {
		this.method = method;
	}


	public Method getMethod() {
		return method;
	}


	public void setDeclaringClass(Class<?> declaringClass) {
		this.declaringClass = declaringClass;
	}


	public Class<?> getDeclaringClass() {
		return declaringClass;
	}


	public void setForwardedPointer(long forwardedPointer) {
		this.forwardedPointer = forwardedPointer;
	}


	public long getForwardedPointer() {
		return forwardedPointer;
	}


	public void setVirtualIndex(int virtualIndex) {
		this.virtualIndex = virtualIndex;
	}


	public int getVirtualIndex() {
		return virtualIndex;
	}

	public String getSymbolName() {
		return symbolName;
	}
	public void setSymbolName(String symbolName) {
		this.symbolName = symbolName;
	}
	public void setDcCallingConvention(int dcCallingConvention) {
		this.dcCallingConvention = dcCallingConvention;
	}


	public int getDcCallingConvention() {
		return dcCallingConvention;
	}

    public Callback getJavaCallback() {
        return javaCallback;
    }

    public void setJavaCallback(Callback javaCallback) {
        this.javaCallback = javaCallback;
    }
}
