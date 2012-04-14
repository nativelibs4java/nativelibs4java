/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.util;

public enum Primitive {
    Float(Float.class, 1, OpenCLType.Float),
    Double(Double.class, 1, OpenCLType.Double),
    Long(Long.class, 1, OpenCLType.Long),
    Int(Integer.class, 1, OpenCLType.Int),
    Short(Short.class, 1, OpenCLType.Short),
    Byte(Byte.class, 1, OpenCLType.Byte),

    Float2(Float.class, 2, OpenCLType.Float),
    Double2(Double.class, 2, OpenCLType.Double),
    Long2(Long.class, 2, OpenCLType.Long),
    Int2(Integer.class, 2, OpenCLType.Int),
    Short2(Short.class, 2, OpenCLType.Short),
    Byte2(Byte.class, 2, OpenCLType.Byte),

    Float3(Float.class, 3, OpenCLType.Float),
    Double3(Double.class, 3, OpenCLType.Double),
    Long3(Long.class, 3, OpenCLType.Long),
    Int3(Integer.class, 3, OpenCLType.Int),
    Short3(Short.class, 3, OpenCLType.Short),
    Byte3(Byte.class, 3, OpenCLType.Byte),

    Float4(Float.class, 4, OpenCLType.Float),
    Double4(Double.class, 4, OpenCLType.Double),
    Long4(Long.class, 4, OpenCLType.Long),
    Int4(Integer.class, 4, OpenCLType.Int),
    Short4(Short.class, 4, OpenCLType.Short),
    Byte4(Byte.class, 4, OpenCLType.Byte),

    Float8(Float.class, 8, OpenCLType.Float),
    Double8(Double.class, 8, OpenCLType.Double),
    Long8(Long.class, 8, OpenCLType.Long),
    Int8(Integer.class, 8, OpenCLType.Int),
    Short8(Short.class, 8, OpenCLType.Short),
    Byte8(Byte.class, 8, OpenCLType.Byte),

    Float16(Float.class, 16, OpenCLType.Float),
    Double16(Double.class, 16, OpenCLType.Double),
    Long16(Long.class, 16, OpenCLType.Long),
    Int16(Integer.class, 16, OpenCLType.Int),
    Short16(Short.class, 16, OpenCLType.Short),
    Byte16(Byte.class, 16, OpenCLType.Byte);

    Primitive(Class<?> primitiveType, int primitiveCount, OpenCLType oclType) {
        this.primitiveCount = primitiveCount;
        this.primitiveType = primitiveType;
        this.oclType = oclType;
    }
    public final OpenCLType oclType;
    public final int primitiveCount;
    public final Class<?> primitiveType;
    public String clTypeName() {
        return name().toLowerCase();
    }
    public String getRequiredPragmas() {
    	if (primitiveType == Double.class)
    		return "#pragma OPENCL EXTENSION cl_khr_fp64 : enable\n";
    	return "";
    }
}