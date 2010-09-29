/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.util;

public enum Primitive {
    Float(Float.class, 1),
    Double(Double.class, 1),
    Long(Long.class, 1),
    Int(Integer.class, 1),
    Short(Short.class, 1),
    Byte(Byte.class, 1),

    Float2(Float.class, 2),
    Double2(Double.class, 2),
    Long2(Long.class, 2),
    Int2(Integer.class, 2),
    Short2(Short.class, 2),
    Byte2(Byte.class, 2),

    Float3(Float.class, 3),
    Double3(Double.class, 3),
    Long3(Long.class, 3),
    Int3(Integer.class, 3),
    Short3(Short.class, 3),
    Byte3(Byte.class, 3),

    Float4(Float.class, 4),
    Double4(Double.class, 4),
    Long4(Long.class, 4),
    Int4(Integer.class, 4),
    Short4(Short.class, 4),
    Byte4(Byte.class, 4),

    Float8(Float.class, 8),
    Double8(Double.class, 8),
    Long8(Long.class, 8),
    Int8(Integer.class, 8),
    Short8(Short.class, 8),
    Byte8(Byte.class, 8),

    Float16(Float.class, 16),
    Double16(Double.class, 16),
    Long16(Long.class, 16),
    Int16(Integer.class, 16),
    Short16(Short.class, 16),
    Byte16(Byte.class, 16);

    Primitive(Class<?> primitiveType, int primitiveCount) {
        this.primitiveCount = primitiveCount;
        this.primitiveType = primitiveType;
    }
    public final int primitiveCount;
    public final Class<?> primitiveType;
    public String clTypeName() {
        return name().toLowerCase();
    }
}