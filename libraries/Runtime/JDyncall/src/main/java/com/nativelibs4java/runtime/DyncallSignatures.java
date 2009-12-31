/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.runtime;
import static com.nativelibs4java.runtime.JDyncallLibrary.*;
/**
 *
 * @author Olivier
 */
public class DyncallSignatures {
    
    public static ValueType GetJavaTypeSignature(Class<?> type, StringBuilder javasig, StringBuilder dcsig, Options options, OptimizationHints optimizationHints) {
        if (type == null || type.equals(Void.TYPE)) {
            if (dcsig != null)
                dcsig.append(DC_SIGCHAR_VOID);
            if (javasig != null)
                javasig.append('V');
            return ValueType.eVoidValue;
        }
        if (type.isArray())
        {
            optimizationHints.isAdaptableAsRaw = false;
            if (dcsig != null)
                dcsig.append(DC_SIGCHAR_POINTER);
            if (javasig != null)
                javasig.append('[');

            ValueType componentType = GetJavaTypeSignature(type.getComponentType(), javasig, null, options, optimizationHints);
            switch (componentType) {
                case eIntValue:
                    return ValueType.eIntArrayValue;
                case eLongValue:
                    return ValueType.eLongArrayValue;
                case eShortValue:
                    return ValueType.eShortArrayValue;
                case eByteValue:
                    return ValueType.eByteArrayValue;
                case eDoubleValue:
                    return ValueType.eDoubleArrayValue;
                //case eCharValue:
                //	return ValueType.eCharArrayValue;
                case eFloatValue:
                    return ValueType.eFloatArrayValue;
                case eAddressableValue:
                case eCallbackValue:
                    return ValueType.eArrayValue;
                case eSizeTValue:
                    //TODO
                case eCLongValue:
                    //TODO
                case eWCharValue:
                    //TODO
                case eLongPtrValue:
                    //TODO
                case eBufferValue:
                    //TODO
                case eCharArrayValue:
                case eBooleanArrayValue:
                case eFloatArrayValue:
                case eDoubleArrayValue:
                case eShortArrayValue:
                case eIntArrayValue:
                case eByteArrayValue:
                case eLongArrayValue:
                    //TODO ?
                case eArrayValue:
                case eVoidValue:
                default:
                    throw new RuntimeException("Unhandled array component type in GetJavaTypeSignature: " + componentType);
            }

        }
        ValueType typeOut;

        if (type.isPrimitive()) {
            if (type.equals(Integer.TYPE)) {
                if (dcsig != null)
                    dcsig.append(DC_SIGCHAR_INT);
                if (javasig != null)
                    javasig.append('I');
                typeOut = ValueType.eIntValue;
            } else if (type.equals(Long.TYPE)) {
                if (javasig != null)
                    javasig.append('J');
                if (options.bIsPointer) {
                    if (JNI.POINTER_SIZE != 8)
                        optimizationHints.isAdaptableAsRaw = false;
                    if (dcsig != null)
                        dcsig.append(DC_SIGCHAR_POINTER);
                    typeOut = ValueType.eLongPtrValue;
                } else if (options.bIsCLong) {
                    if (JNI.POINTER_SIZE != 8)
                        optimizationHints.isAdaptableAsRaw = false;
                    if (dcsig != null)
                        dcsig.append(DC_SIGCHAR_LONG);
                    typeOut = ValueType.eCLongValue;
                } else if (options.bIsSizeT) {
                    if (JNI.SIZE_T_SIZE != 8)
                        optimizationHints.isAdaptableAsRaw = false;
                    if (dcsig != null)
                        dcsig.append(JNI.SIZE_T_SIZE == 4 ? DC_SIGCHAR_INT : DC_SIGCHAR_LONGLONG);
                    typeOut = ValueType.eSizeTValue;
                } else {
                    if (dcsig != null)
                        dcsig.append(DC_SIGCHAR_LONGLONG);
                    typeOut = ValueType.eLongValue;
                }
            } else if (type.equals(Short.TYPE)) {
                if (dcsig != null)
                        dcsig.append(DC_SIGCHAR_SHORT);
                if (javasig != null)
                    javasig.append('S');
                typeOut = ValueType.eShortValue;
            } else if (type.equals(Double.TYPE)) {
                if (dcsig != null)
                    dcsig.append(DC_SIGCHAR_DOUBLE);
                if (javasig != null)
                    javasig.append('D');
                typeOut = ValueType.eDoubleValue;
            } else if (type.equals(Float.TYPE)) {
                if (dcsig != null)
                    dcsig.append(DC_SIGCHAR_FLOAT);
                if (javasig != null)
                    javasig.append('F');
                typeOut = ValueType.eFloatValue;
            } else if (type.equals(Boolean.TYPE)) {
                if (dcsig != null)
                    dcsig.append(DC_SIGCHAR_CHAR);
                if (javasig != null)
                    javasig.append('Z');
                typeOut = ValueType.eByteValue;
            } else if (type.equals(Byte.TYPE)) {
                if (dcsig != null)
                    dcsig.append(DC_SIGCHAR_CHAR);
                if (javasig != null)
                    javasig.append('B');
                typeOut = ValueType.eByteValue;
            } else if (type.equals(Character.TYPE)) {
                optimizationHints.isAdaptableAsRaw = false;
                if (options.bIsWideChar) {
                    switch (JNI.WCHAR_T_SIZE) {
                        case 1:
                            if (dcsig != null)
                                dcsig.append(DC_SIGCHAR_CHAR);
                            break;
                        case 2:
                            if (dcsig != null)
                                dcsig.append(DC_SIGCHAR_SHORT);
                            break;
                        case 4:
                            if (dcsig != null)
                                dcsig.append(DC_SIGCHAR_INT);
                            break;
                        default:
                            throw new RuntimeException("Unhandled sizeof(wchar_t) in GetJavaTypeSignature: " + JNI.WCHAR_T_SIZE);
                    }
                    typeOut = ValueType.eWCharValue;
                } else {
                    if (dcsig != null)
                        dcsig.append(DC_SIGCHAR_CHAR);
                    typeOut = ValueType.eByteValue;
                }
                if (javasig != null)
                    javasig.append('C');
            } else {
                optimizationHints.isAdaptableAsRaw = false;
                String name = type.getName();
                typeOut = ValueType.eVoidValue;
                throw new RuntimeException("Unhandled primitive type in GetJavaTypeSignature : " + name);
            }
        } else {
            optimizationHints.isAdaptableAsRaw = false;
            String name = type.getName();
            if (dcsig != null)
                dcsig.append(DC_SIGCHAR_POINTER);
            if (javasig != null)
                javasig.append('L');
            for (int i = 0, len = name.length(); i < len; i++) {
                char c = name.charAt(i);
                if (c == '.')
                    c = '/';
                if (javasig != null)
                    javasig.append(c);
            }
            if (javasig != null)
                javasig.append(';');
            typeOut = ValueType.eAddressableValue;
        }
        return typeOut;
    }
}
