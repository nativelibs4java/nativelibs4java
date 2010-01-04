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
    private final int size;
    private final String name, capName, wrapperName, bufferName;

    public String getCapName() {
        return capName;
    }

    public Class<?> getBufferType() {
        return bufferType;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
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

    public Primitive(Class<?> type) {
        this.type = type;
        if (type == Integer.TYPE) {
            wrapper = Integer.class;
            bufferType = IntBuffer.class;
            size = 4;
        } else if (type == Long.TYPE) {
            wrapper = Long.class;
            bufferType = LongBuffer.class;
            size = 8;
        } else if (type == Short.TYPE) {
            wrapper = Short.class;
            bufferType = ShortBuffer.class;
            size = 2;
        } else if (type == Byte.TYPE) {
            wrapper = Byte.class;
            bufferType = ByteBuffer.class;
            size = 1;
        } else if (type == Character.TYPE) {
            wrapper = Character.class;
            bufferType = CharBuffer.class;
            size = 2;
        } else if (type == Float.TYPE) {
            wrapper = Float.class;
            bufferType = FloatBuffer.class;
            size = 4;
        } else if (type == Double.TYPE) {
            wrapper = Double.class;
            bufferType = DoubleBuffer.class;
            size = 8;
        } else if (type == Boolean.TYPE) {
            wrapper = Boolean.class;
            bufferType = ByteBuffer.class;
            size = 1;
        } else
            throw new IllegalArgumentException();

        name = type.getName();
        capName = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        wrapperName = wrapper.getSimpleName();
        bufferName = bufferType.getSimpleName();
    }

    static List<Primitive> primitives;
    public static synchronized List<Primitive> getPrimitives() {
        if (primitives == null) {
            primitives = new ArrayList<Primitive>();
            primitives.add(new Primitive(Integer.TYPE));
            primitives.add(new Primitive(Long.TYPE));
            primitives.add(new Primitive(Short.TYPE));
            primitives.add(new Primitive(Byte.TYPE));
            primitives.add(new Primitive(Character.TYPE));
            primitives.add(new Primitive(Float.TYPE));
            primitives.add(new Primitive(Double.TYPE));
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
