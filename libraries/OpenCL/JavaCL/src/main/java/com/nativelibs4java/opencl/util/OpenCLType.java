/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.util;

public enum OpenCLType {
    Int(Integer.class), Char(Character.class), Long(Long.class), Short(Short.class), Byte(Byte.class), Double(Double.class), Float(Float.class), Half(null);
        
	OpenCLType(Class<?> type) {
			this.type = type;
	}
	public final Class<?> type;
	
    public String toCType() {
        if (this == Byte)
            return "char";
        if (this == Char)
            return "short";
        return name().toLowerCase();
    }
    public static OpenCLType fromClass(Class<? extends Number> valueType) {
        if (valueType == Integer.TYPE || valueType == Integer.class)
            return Int;
        if (valueType == java.lang.Long.TYPE || valueType == Long.class)
            return Long;
        if (valueType == java.lang.Short.TYPE || valueType == Short.class)
            return Short;
        if (valueType == java.lang.Double.TYPE || valueType == Double.class)
            return Double;
        if (valueType == java.lang.Float.TYPE || valueType == Float.class)
            return Float;
        if (valueType == java.lang.Byte.TYPE || valueType == Byte.class)
            return Byte;

        if (!valueType.isPrimitive())
            throw new IllegalArgumentException("Value type is not a primitive: '" + valueType.getName() + "' !");

        throw new IllegalArgumentException("Primitive type not handled: '" + valueType.getName() + "' !");
    }
}