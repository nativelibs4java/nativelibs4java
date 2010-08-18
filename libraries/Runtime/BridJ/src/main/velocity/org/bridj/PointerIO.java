package org.bridj;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.nio.*;

/**
 *
 * @author Olivier
 */
public abstract class PointerIO<T> {
	final Type targetType;
	final Class<?> typedPointerClass;
	final int targetSize, targetAlignment = -1;
	
	public PointerIO(Type targetType, int targetSize, Class<?> typedPointerClass) {
		this.targetType = targetType;
		this.targetSize = targetSize;
		this.typedPointerClass = typedPointerClass;
	}
	abstract T get(Pointer<T> pointer, long index);
	abstract void set(Pointer<T> pointer, long index, T value);
	
	public T castTarget(long peer) {
		throw new UnsupportedOperationException("Cannot cast pointer to " + targetType);
	}
	
	PointerIO<Pointer<T>> getReferenceIO() {
		return new CommonPointerIOs.PointerPointerIO<T>(this);
	}
	public long getTargetSize() {
		return targetSize;
	}
	public int getTargetAlignment() { 
		return targetAlignment < 0 ? (int)getTargetSize() : targetAlignment;
	}
	public boolean isTypedPointer() {
		return typedPointerClass != null;
	}
	public Class<?> getTypedPointerClass() {
		return typedPointerClass;
	}
	public Type getTargetType() {
		return targetType;
	}
	
	static Class<?> getClass(Type type) {
		if (type instanceof Class<?>)
			return (Class<?>)type;
		if (type instanceof ParameterizedType)
			return getClass(((ParameterizedType)type).getRawType());
		return null;
	}
	
	static PointerIO pointerInstance;
    public synchronized static PointerIO<Pointer> getPointerInstance() {
        if (pointerInstance == null)
            pointerInstance = getPointerInstance((PointerIO<?>)null);
        return pointerInstance;
    }
    
	public static <T> PointerIO<Pointer<T>> getPointerInstance(Type target) {
		return getPointerInstance((PointerIO<T>)getInstance(target));
	}
	public static <T> PointerIO<Pointer<T>> getPointerInstance(PointerIO<T> targetIO) {
		return new CommonPointerIOs.PointerPointerIO<T>(targetIO);
	}
	public static <T> PointerIO<Pointer<T>> getArrayInstance(PointerIO<T> targetIO, long[] dimensions, int iDimension) {
		return new CommonPointerIOs.PointerArrayIO<T>(targetIO, dimensions, iDimension);
	}
	
    public synchronized static <S extends StructObject> PointerIO<S> getInstance(StructIO s) {
        return new CommonPointerIOs.StructPointerIO(s);
    }
    static Map<Type, PointerIO<?>> ios = new HashMap<Type, PointerIO<?>>();
	public synchronized static <P> PointerIO<P> getInstance(Type type) {
		PointerIO io = ios.get(type);
        if (io == null) {
            final Class<?> cl = (type instanceof Class) ? (Class)type : (type instanceof ParameterizedType) ? (Class)((ParameterizedType)type).getRawType() : null;
    	
            #foreach ($prim in $primitives)
            #if ($velocityCount > 1) else #end
            if (type == ${prim.WrapperName}.TYPE || type == ${prim.WrapperName}.class)
                io = CommonPointerIOs.${prim.Name}IO;
            #end
			else if (cl != null && Pointer.class.equals(cl))
                io = getPointerInstance();
            else if (cl != null && TypedPointer.class.isAssignableFrom(cl))
            	io = new CommonPointerIOs.TypedPointerPointerIO((Class<? extends TypedPointer>)cl);
            else if (cl != null && SizeT.class.isAssignableFrom(cl))
                io = CommonPointerIOs.sizeTIO;
			else if (cl != null && CLong.class.isAssignableFrom(cl))
                io = CommonPointerIOs.clongIO;
			else if (cl != null && StructObject.class.isAssignableFrom(cl))
				io = getInstance(StructIO.getInstance((Class)cl, type));
            else if (cl != null && Callback.class.isAssignableFrom(cl))
				io = new CommonPointerIOs.CallbackPointerIO(cl);
            //else
            //throw new UnsupportedOperationException("Cannot create pointer io to type " + type + ((type instanceof Class) && ((Class)type).getSuperclass() != null ? " (parent type : " + ((Class)type).getSuperclass().getName() + ")" : ""));
            	//return null; // TODO throw here ?

            //if (io == null)
            //	throw new RuntimeException("Failed to create pointer io to type " + type);
            ios.put(type, io);
        }
        return io;
    }

    static PointerIO<SizeT> sizeTInstance;

    public static PointerIO<SizeT> getSizeTInstance() {
        if (sizeTInstance == null)
            sizeTInstance = getInstance(SizeT.class);
        return sizeTInstance;
	}

    static PointerIO<CLong> clongInstance;

    public static PointerIO<CLong> getCLongInstance() {
        if (clongInstance == null)
            clongInstance = getInstance(CLong.class);
        return clongInstance;
	}

#foreach ($prim in $primitives)
    static PointerIO<${prim.WrapperName}> ${prim.Name}Instance;
    
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
