package com.nativelibs4java.velocity;

import java.nio.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Olivier
 */
public class Primitive {
    private final Class<?> type, wrapper, bufferType;
    private final String size;
    private final String name, capName, wrapperName, bufferName;
    private final String v1, v2, v3;

    public String getCapName() {
        return capName;
    }

    public Class<?> getBufferType() {
        return bufferType;
    }

    public String getName() {
        return name;
    }

    public String getV1() {
        return       v1;
    }

    public String getV2() {
        return       v2;
    }

    public String getV3() {
        return       v3;
    }

    public String getSize() {
        return size;
    }

    public Class<?> getType() {
        return type;
    }

    public Class<?> getWrapper() {
        return wrapper;
    }

    public String getWrapperName() {
        return wrapperName;
    }

    public String getBufferName() {
        return bufferName;
    }

    public Primitive(String integralClassName) {
        this.type = null;
		wrapper = null;
		bufferType = null;
		size = integralClassName + ".SIZE";
		v1 = "new " + integralClassName + "(1)";
		v2 = "new " + integralClassName + "(2)";
		v3 = "new " + integralClassName + "(3)";
		
		name = integralClassName;
        capName = integralClassName;
        wrapperName = integralClassName;
        bufferName = null;
	}
    
    public Primitive(Class<?> type) {
        this.type = type;
        if (type == Integer.TYPE) {
            wrapper = Integer.class;
            bufferType = IntBuffer.class;
            size = "4";
            v1 = "1";
            v2 = "2";
            v3 = "3";
        } else if (type == Long.TYPE) {
            wrapper = Long.class;
            bufferType = LongBuffer.class;
            size = "8";
            v1 = "1L";
            v2 = "2L";
            v3 = "3L";
        } else if (type == Short.TYPE) {
            wrapper = Short.class;
            bufferType = ShortBuffer.class;
            size = "2";
            v1 = "(short)1";
            v2 = "(short)2";
            v3 = "(short)3";
        } else if (type == Byte.TYPE) {
            wrapper = Byte.class;
            bufferType = ByteBuffer.class;
            size = "1";
            v1 = "(byte)1";
            v2 = "(byte)2";
            v3 = "(byte)3";
        } else if (type == Character.TYPE) {
            wrapper = Character.class;
            bufferType = CharBuffer.class;
            size = "2";
            v1 = "'a'";
            v2 = "'b'";
            v3 = "'c'";
        } else if (type == Float.TYPE) {
            wrapper = Float.class;
            bufferType = FloatBuffer.class;
            size = "4";
            v1 = "1f";
            v2 = "2f";
            v3 = "3f";
        } else if (type == Double.TYPE) {
            wrapper = Double.class;
            bufferType = DoubleBuffer.class;
            size = "8";
            v1 = "1.0";
            v2 = "2.0";
            v3 = "3.0";
        } else if (type == Boolean.TYPE) {
            wrapper = Boolean.class;
            bufferType = ByteBuffer.class;
            size = "1";
            v1 = "true";
            v2 = "false";
            v3 = "true";
        } else
            throw new IllegalArgumentException();

        name = type.getName();
        capName = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        wrapperName = wrapper.getSimpleName();
        bufferName = bufferType.getSimpleName();
    }

    static List<Primitive> bridJPrimitives;
    public static synchronized List<Primitive> getBridJPrimitives() {
        if (bridJPrimitives == null) {
            bridJPrimitives = new ArrayList<Primitive>();
            bridJPrimitives.addAll(getPrimitives());
            bridJPrimitives.add(new Primitive("CLong"));
            bridJPrimitives.add(new Primitive("SizeT"));
            bridJPrimitives = Collections.unmodifiableList(bridJPrimitives);
        }
        return bridJPrimitives;
    }

    static List<Primitive> primitives;
    public static synchronized List<Primitive> getPrimitives() {
        if (primitives == null) {
            primitives = new ArrayList<Primitive>();
            primitives.addAll(getPrimitivesNoBool());
            primitives.add(new Primitive(Boolean.TYPE));
            primitives = Collections.unmodifiableList(primitives);
        }
        return primitives;
    }

    static List<Primitive> primitivesNoBool;
    public static synchronized List<Primitive> getPrimitivesNoBool() {
        if (primitivesNoBool == null) {
            primitivesNoBool = new ArrayList<Primitive>();
            primitivesNoBool.add(new Primitive(Integer.TYPE));
            primitivesNoBool.add(new Primitive(Long.TYPE));
            primitivesNoBool.add(new Primitive(Short.TYPE));
            primitivesNoBool.add(new Primitive(Byte.TYPE));
            primitivesNoBool.add(new Primitive(Character.TYPE));
            primitivesNoBool.add(new Primitive(Float.TYPE));
            primitivesNoBool.add(new Primitive(Double.TYPE));
            primitivesNoBool = Collections.unmodifiableList(primitivesNoBool);
        }
        return primitivesNoBool;
    }
}
