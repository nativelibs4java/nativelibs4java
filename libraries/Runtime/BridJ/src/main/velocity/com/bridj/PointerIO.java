package com.bridj;

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
	
	public int getTargetAlignment() {
		return getTargetSize();
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
            if (Pointer.class.equals(c))
                return (T)Pointer.pointerToAddress(pointer.getSizeT(JNI.POINTER_SIZE * index), (PointerIO)PointerIO.getInstanceByType(getTargetType()));
            if (Pointer.class.isAssignableFrom(c))
                return (T)c.getConstructor(Long.TYPE).newInstance(pointer.getSizeT(JNI.POINTER_SIZE * index));
            if (c == String.class || c == CharSequence.class)
                return (T)pointer.getCString(index * JNI.POINTER_SIZE);
			if (NativeObject.class.isAssignableFrom(c))
				return (T)pointer.toNativeObject((Class)c); 
				
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
            pointer.setCString(index * JNI.POINTER_SIZE, value.toString());
        else
            throw new UnsupportedOperationException("Cannot get value of type " + c.getName());
    }

    static Map<Type, PointerIO> ios = new WeakHashMap<Type, PointerIO>();
    public synchronized static <S extends StructObject> PointerIO<S> getInstance(final StructIO structIO) {
    	final int ss = structIO.getStructSize();
    	return new PointerIO<S>(structIO.getStructClass(), ss) {
			@Override
			public S get(Pointer<S> pointer, int index) {
				return (S)pointer.getNativeObject(index * ss, structIO.getStructType());
			}
			@Override
			public void set(Pointer<S> pointer, int index, S value) {
				Pointer<S> ps = Pointer.getPeer(value);
				pointer.getByteBuffer(index * ss).put(ps.getByteBuffer(0));
			}
		};
    }
    public synchronized static <P> PointerIO<P> getInstance(Class<P> type) {
        return getInstanceByType(type);
    }
    public synchronized static <P> PointerIO<P> getInstanceByType(Type type) {
    	final Class<?> cl = (type instanceof Class) ? (Class)type : (type instanceof ParameterizedType) ? (Class)((ParameterizedType)type).getRawType() : null;
    	
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
			else if (cl != null && Pointer.class.equals(cl))
                io = new PointerIO<Pointer>(type, Pointer.SIZE) {
                    @Override
                    public Pointer get(Pointer<Pointer> pointer, int index) {
                        return pointer.getPointer(index * Pointer.SIZE);
                    }
                    @Override
                    public void set(Pointer<Pointer> pointer, int index, Pointer value) {
                        pointer.setPointer(index * Pointer.SIZE, value);
                    }
                };
            else if (cl != null && Pointer.class.isAssignableFrom(cl))
                io = new PointerIO<Pointer>(type, Pointer.SIZE) {
                	java.lang.reflect.Constructor cons;
                    @Override
                    public Pointer get(Pointer<Pointer> pointer, int index) {
                    	try {
                    		return (Pointer)cons.newInstance(pointer.getSizeT(index * Pointer.SIZE));
                    	} catch (Exception ex) {
                    		throw new RuntimeException("Cannot create pointer of type " + cl.getName(), ex);
                    	}
                    }
                    @Override
                    public void set(Pointer<Pointer> pointer, int index, Pointer value) {
                        pointer.setPointer(index * Pointer.SIZE, value);
                    }
                    {
                    	try {
                    		cons = cl.getConstructor(long.class);
                    	} catch (Exception ex) {
                    		throw new RuntimeException("Cannot find constructor for " + cl.getName(), ex);
                    	}
                    }
                };
            else if (cl != null && SizeT.class.isAssignableFrom(cl))
                io = new PointerIO<SizeT>(type, SizeT.SIZE) {
                    @Override
                    public SizeT get(Pointer<SizeT> pointer, int index) {
                        return new SizeT(pointer.getSizeT(index * SizeT.SIZE));
                    }
                    @Override
                    public void set(Pointer<SizeT> pointer, int index, SizeT value) {
                        pointer.setSizeT(index * SizeT.SIZE, value == null ? 0 : value.longValue());
                    }
                };
			else if (cl != null && StructObject.class.isAssignableFrom(cl))
				io = getInstance(StructIO.getInstance((Class)cl, type, (CRuntime)BridJ.getRuntime(cl)));
            else
                io = new PointerIO(type, -1);

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


    static PointerIO pointerInstance;
    public synchronized static PointerIO<Pointer> getPointerInstance() {
        if (pointerInstance == null)
            pointerInstance = getInstance(Pointer.class);
        return pointerInstance;
    }

}
