package com.jdyncall;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.WeakHashMap;
import java.nio.*;

/**
 *
 * @author Olivier
 */
public class PointerIO<T> {
    final Type targetType;
    final Class<T> targetClass;
    final int targetSize;

    public PointerIO(Type targetType, int targetSize) {
        this.targetType = targetType;
        this.targetSize = targetSize;
        if (targetType instanceof Class<?>)
            targetClass = (Class<T>)targetType;
        else if (targetType instanceof ParameterizedType)
            targetClass = (Class<T>)((ParameterizedType)targetType).getRawType();
        else
            targetClass = null;//throw new UnsupportedOperationException("Can't extract class from type " + targetType.toString());
    }

    public Type getTargetType() {
        return targetType;
    }

    public Class<T> getTargetClass() {
        return targetClass;
    }
	
	public static PointerIO change(PointerIO io, Type newTargetType) {
        if (io != null && newTargetType != null && newTargetType.equals(io.targetType))
			return io;
        return getInstanceByType(newTargetType);
    }

    public int getTargetSize() {
        return targetSize;
    }
    public T get(Pointer<T> pointer, int index) {
        try {
            #foreach ($prim in $primitivesNoBool)
            if (targetType == ${prim.WrapperName}.TYPE || targetType == ${prim.WrapperName}.class)
                return (T)(${prim.WrapperName})pointer.get${prim.CapName}(index * ${prim.Size});
            #end
            Class<T> c = getTargetClass();
            if (Pointer.class.isAssignableFrom(c))
                return (T)c.getConstructor(Long.TYPE).newInstance(pointer.getSizeT(JNI.POINTER_SIZE * index));
            if (c == String.class || c == CharSequence.class)
                return (T)pointer.getString(index * JNI.POINTER_SIZE);

            throw new UnsupportedOperationException("Cannot get value of type " + targetType);
        } catch (Exception ex) {
            throw new RuntimeException("Unexpectedly failed to get value of type " + targetType, ex);
        }
    }
    
    public void set(Pointer<T> pointer, int index, T value) {
        Class<T> c = getTargetClass();
        #foreach ($prim in $primitivesNoBool)
        #if ($velocityCount > 1) else #end
        if (c == ${prim.WrapperName}.TYPE || c == ${prim.WrapperName}.class)
            pointer.set${prim.CapName}(index * ${prim.Size}, (${prim.WrapperName})value);
        #end
        else if (Pointer.class.isAssignableFrom(c))
            pointer.setSizeT(JNI.POINTER_SIZE * index, (Long)value);
        else if (CharSequence.class.isAssignableFrom(c))
            pointer.setString(index * JNI.POINTER_SIZE, value.toString());
        else
            throw new UnsupportedOperationException("Cannot get value of type " + c.getName());
    }

    static Map<Type, PointerIO> ios = new WeakHashMap<Type, PointerIO>();
    public synchronized static <P> PointerIO<P> getInstance(Class<P> type) {
        return getInstanceByType(type);
    }
    public synchronized static <P> PointerIO<P> getInstanceByType(Type type) {
        PointerIO io = ios.get(type);
        if (io == null) {
            #foreach ($prim in $primitivesNoBool)
            #if ($velocityCount > 1) else #end
            if (type == ${prim.WrapperName}.TYPE || type == ${prim.WrapperName}.class)
                io = new PointerIO<${prim.WrapperName}>(type, ${prim.Size}) {
                    @Override
                    public ${prim.WrapperName} get(Pointer<${prim.WrapperName}> pointer, int index) {
                        return pointer.get${prim.CapName}(index * ${prim.Size});
                    }
                    @Override
                    public void set(Pointer<${prim.WrapperName}> pointer, int index, ${prim.WrapperName} value) {
                        pointer.set${prim.CapName}(index * ${prim.Size}, value);
                    }
                };
            #end
            else
                io = new PointerIO(type, -1);

            ios.put(type, io);
        }
        return io;
    }

    #foreach ($prim in $primitivesNoBool)
    static PointerIO<${prim.WrapperName}> ${prim.Name}Instance;
    #end

    #foreach ($prim in $primitivesNoBool)
    public static PointerIO<${prim.WrapperName}> get${prim.CapName}Instance() {
        if (${prim.Name}Instance == null)
            ${prim.Name}Instance = getInstance(${prim.WrapperName}.class);
        return ${prim.Name}Instance;
	}
    #end

    public static <P> PointerIO<P> getBufferPrimitiveInstance(Buffer buffer) {
        #foreach ($prim in $primitivesNoBool)
		if (buffer instanceof ${prim.BufferName})
            return (PointerIO<P>)get${prim.CapName}Instance();
		#end
        throw new UnsupportedOperationException();
    }

    static PointerIO stringInstance;
    public synchronized static PointerIO<String> getStringInstance() {
        if (stringInstance == null)
            stringInstance = getInstance(String.class);
        return stringInstance;
    }

}
