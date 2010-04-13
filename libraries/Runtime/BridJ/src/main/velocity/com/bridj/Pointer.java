package com.bridj;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.*;
import java.util.Stack;
import java.util.NoSuchElementException;
import java.util.Iterator;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public abstract class Pointer<T> implements Comparable<Pointer<?>>, Iterable<T>
        //, com.sun.jna.Pointer<Pointer<T>>
{
    public static final Pointer NULL = null;
	public static final int SIZE = JNI.POINTER_SIZE;
    
	static {
        JNI.initLibrary();
    }

    protected PointerIO<T> io;
	
    
    public long getRemainingBytes() {
    	return -1;
    }
    public long getRemainingElements() {
    	long bytes = getRemainingBytes();
    	long elementSize = getTargetSize();
    	if (bytes < 0 || elementSize <= 0)
    		return -1;
    	return bytes / elementSize;
    }
    
    
    public Iterator<T> iterator() {
    	return new Iterator<T>() {
    		Pointer<T> next = Pointer.this;
    		@Override
			public T next() {
				if (next == null)
					throw new NoSuchElementException();
                T value = next.get();
				next = next.getRemainingElements() > 1 ? next.next(1) : null;
				return value;
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			@Override
			public boolean hasNext() {
				long rem;
				return next != null && ((rem = next.getRemainingBytes()) < 0 || rem > 0);
			}
    	};
    }

    
	public Pointer<T> order(ByteOrder order) {
		if (order.equals(order()))
			return this;
		
		return disorderedClone();
	}
    
	public ByteOrder order() {
		return ByteOrder.nativeOrder();
    }

    /**
     * Create a copy of this pointer
     * @return
     * @throws CloneNotSupportedException
     */
    @Override
    public Pointer<T> clone() {
        throw new UnsupportedOperationException(); // TODO
    }

    protected Pointer<T> disorderedClone() {
        throw new UnsupportedOperationException(); // TODO
    }
    
    public static <N extends NativeObject> Pointer<N> getPeer(N instance) {
		return getPeer(instance, null);
    }
    public static <R extends NativeObject> Pointer<R> getPeer(NativeObject instance, Class<R> targetType) {
		return (Pointer<R>)instance.peer;
    }
    public static long getAddress(NativeObject instance, Class targetType) {
		return getPeer(instance, targetType).getPeer();
    }
    
	public <O extends NativeObject> O getNativeObject(long byteOffset, Type type) {
		return (O)BridJ.createNativeObjectFromPointer((Pointer<O>)this, type);
	}
    public <O extends NativeObject> O getNativeObject(long byteOffset, Class<O> type) {
		return (O)getNativeObject(byteOffset, (Type)type);
	}
    public <O extends NativeObject> O toNativeObject(Class<O> type) {
		return getNativeObject(0, type);
	}
	
	/**
	 * Check that the pointer's peer is aligned to the target type alignment.
	 * If the pointer has no peer, this method returns true.
	 * @throw RuntimeException If the target type of this pointer is unknown
	 * @return !hasPeer() || getPeer() % alignment == 0
	 */
	public boolean isAligned() {
		PointerIO<T> io = getIO();
        if (io == null)
            throw new RuntimeException("Cannot check alignment without a properly defined targetType");

        return isAligned(io.getTargetAlignment());
	}
	
	/**
	 * Check that the pointer's peer is aligned to the given alignment.
	 * If the pointer has no peer, this method returns true.
	 * @return !hasPeer() || getPeer() % alignment == 0
	 */
	public boolean isAligned(int alignment) {
		if (!hasPeer())
			return true;
		return isAligned(getPeer(), alignment);
	}
	
	/**
	 * Check that the provided address is aligned to the given alignment.
	 * @return address % alignment == 0
	 */
	protected static boolean isAligned(long address, int alignment) {
		switch (alignment) {
		case 1:
			return true;
		case 2:
			return (address & 1) == 0;
		case 4:
			return (address & 3) == 0;
		case 8:
			return (address & 7) == 0;
		case 16:
			return (address & 15) == 0;
		case 32:
			return (address & 31) == 0;
		case 64:
			return (address & 63) == 0;
		default:
			return (address % alignment) == 0;
		}
	}
	
	public interface UpdatablePointer<P> {
		Pointer<P> getPointer();
		void update();
	}
	
	
	public static Pointer<Byte> pointerToCString(String string) {
		if (string == null)
			return null;
		
		byte[] bytes = string.getBytes();
		Pointer<Byte> p = allocateArray(Byte.class, bytes.length + 1);
        p.setBytes(0, bytes);
		p.setByte(bytes.length, (byte)0);
		//p.setString(0, string);
		return p;
	}
	
	/**
	 * The update will take place inside the release() call
	 */
    public static Pointer<Pointer<Byte>> pointerToCStrings(final String... strings) {
    	if (strings == null)
    		return null;
        final int len = strings.length;
        final Pointer<Byte>[] pointers = (Pointer<Byte>[])new Pointer[len];
        final Pointer<Pointer<?>> mem = allocatePointers(len);
        for (int i = 0; i < len; i++)
            mem.set(i, pointers[i] = pointerToCString(strings[i]));

		class UpdatableStringArrayPointer extends Memory<Pointer<Byte>> {
			public UpdatableStringArrayPointer(Memory mem) {
				super((PointerIO<Pointer<Byte>>)(PointerIO)PointerIO.getPointerInstance(), mem); // TODO
			}
			public Pointer<Pointer<Byte>> getPointer() {
				return this;
			}
            @Override
            public void release() {
                for (int i = 0; i < len; i++) {
                    strings[i] = mem.get(i).getCString(0);
                    pointers[i].release();
                }
                super.release();
            }
        };
		return new UpdatableStringArrayPointer((Memory)mem);
    }

	public abstract Pointer<Pointer<T>> getReference();

    /**
	 * Dereference this pointer (*ptr).
     * @throws RuntimeException if the pointer's target type is unknown (@see Pointer.setTargetType())
	 */
    public T get() {
        return get(0);
    }
    
	public T get(int index) {
        PointerIO<T> io = getIO();
        if (io == null)
            throw new RuntimeException("Cannot get pointed value without a properly defined targetType");

        return io.get(this, index);
    }
    
    public void set(T value) {
        set(0, value);
    }
    
	public Pointer<T> set(int index, T value) {
        PointerIO<T> io = getIO();
        if (io == null) {
            if (value == null)
                throw new RuntimeException("Cannot set pointed value without a properly defined targetType");
            else
                setTargetType(value.getClass());
        }
        io.set(this, index, value);
        return this;
    }
    
	public static long getOrAllocateTempPeer(Pointer<?> pointer) {
        return pointer == null ? 0 : pointer.getOrAllocateTempPeer();
    }
	
	public static long getPeer(Pointer<?> pointer) {
        return pointer == null ? 0 : pointer.getPeer();
    }
	
	public static void deleteTempPeer(Pointer<?> pointer, long tempPeer, boolean refresh) {
		if (pointer == null || tempPeer == 0)
			return;
		pointer.deleteTempPeer(tempPeer, refresh);
    }
	
	/**
	 * Returns a pointer which address value was obtained by this pointer's by adding a byte offset.
	 * If the pointer has a peer (<code>ptr.hasPeer()</code>), the following is true : <code>offset == (ptr.offset(offset).getPeer() - ptr.getPeer())</code>
	 */
    public abstract Pointer<T> offset(long offset);

	public int getTargetSize() {
		PointerIO<T> io = getIO();
        if (io == null)
            throw new RuntimeException("Cannot compute target size without a properly defined targetType");

        return io.getTargetSize();
	}
	
	/**
	 * Returns a pointer to the n-th next (or previous) target.
	 * Same as incrementing a C pointer of delta elements, but creates a new pointer instance.
	 * @return offset(getTargetSize() * delta)
	 */
	public Pointer<T> next(long delta) {
		PointerIO<T> io = getIO();
        if (io == null)
            throw new RuntimeException("Cannot get pointers to next or previous targets without a properly defined targetType");

        return offset(io.getTargetSize() * delta);
	}
	
	/**
	 * TODO JavaDoc
	 * Returns the long value of the address this Pointer instance points to.
	 * @throws UnsupportedOperationException if the pointer does not have a peer (@see hasPeer())
	 */
    public abstract long getPeer();
    
	public abstract boolean hasPeer();
	
	public abstract long getOrAllocateTempPeer();
    
	public abstract void deleteTempPeer(long tempPeer, boolean refresh);
	
	/**
	 * TODO JavaDoc
	 * Ask for early memory reclamation in case of manually allocated pointer.
	 */
    public void release() {}

    public static void release(Pointer... pointers) {
    		for (Pointer pointer : pointers)
    			if (pointer != null)
    				pointer.release();
	}
	
	/**
	 * TODO JavaDoc
	 * Updates linked Java structures, if any.
	 */
    public void update() {}

    /**
	 * Compare to another Pointer instance
	 */
    public abstract int compareTo(Pointer<?> o);

    /**
	 * Test equality of the 
	 */
	@Override
    public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Pointer))
			return false;
		
		Pointer p = (Pointer)obj;
		boolean hp = hasPeer();
		if (hp != p.hasPeer())
			return false;
		
		if (hp)
			return getPeer() == p.getPeer();
		else
			return identityEqual(p);
	}
	
	/**
	 * Used in case of pointers that don't have peers to compare.
	 * This can be overridden to provide an identity equality that takes underlying data into account.
	 */
	protected boolean identityEqual(Pointer p) {
		return super.equals(p);	 
	}

    @Deprecated
    public static Pointer<?> pointerToAddress(long address) {
        return address == 0 ? null : new DefaultPointer(null, address);
    }

    @Deprecated
    public static Pointer<?> pointerToAddress(long address, long size) {
        return pointerToAddress(address, size, null);
    }

    public interface Deallocator {
        void deallocate(long peer);
    }
    
    public static Pointer<?> pointerToAddress(long address, Class<?> type, final Deallocator deallocator) {
        return pointerToAddress(address, PointerIO.getInstance(type), deallocator);
    }
	static Pointer<?> pointerToAddress(long address, PointerIO io, final Deallocator deallocator) {
        return address == 0 ? null : new Memory(io, address, -1) {
            @Override
            protected void free(long peer) {
                if (deallocator != null)
                    deallocator.deallocate(peer);
            }
        };
    }
	
	@Deprecated
    public static Pointer<?> pointerToAddress(long address, final Deallocator deallocator) {
		return pointerToAddress(address, -1, deallocator);
	}
    
	public static Pointer<?> pointerToAddress(long address, long size, final Deallocator deallocator) {
        return address == 0 ? null : new Memory(null, address, -1) {
            @Override
            protected void free(long peer) {
                if (deallocator != null)
                    deallocator.deallocate(peer);
            }
        };
    }
	
	@Deprecated
    public static <P> Pointer<P> pointerToAddress(long address, Class<P> targetClass) {
        return address == 0 ? null : new DefaultPointer(PointerIO.getInstance(targetClass), address);
    }

    static <P> Pointer<P> pointerToAddress(long address, PointerIO<P> io) {
        return address == 0 ? null : new DefaultPointer(io, address);
    }

    public static <P extends TypedPointer> Pointer<P> allocateTypedPointer(Class<P> type) {
    	return (Pointer<P>)(Pointer)allocate(PointerIO.getInstance(type), Pointer.SIZE);
    }
    public static <P extends TypedPointer> Pointer<P> allocateTypedPointers(Class<P> type, long arrayLength) {
    	return (Pointer<P>)(Pointer)allocate(PointerIO.getInstance(type), Pointer.SIZE * arrayLength);
    }
    public static <P> Pointer<Pointer<P>> allocatePointer(Class<P> type) {
    	return (Pointer<Pointer<P>>)(Pointer)allocate(PointerIO.getPointerInstance(), Pointer.SIZE); // TODO 
    }
    public static <V> Pointer<Pointer<?>> allocatePointer() {
    	return (Pointer)allocate(PointerIO.getPointerInstance(), Pointer.SIZE);
    }
    public static Pointer<Pointer<?>> allocatePointers(int arrayLength) {
		return (Pointer<Pointer<?>>)(Pointer)allocate(PointerIO.getPointerInstance(), JNI.POINTER_SIZE * arrayLength); 
	}
	
    public static <P> Pointer<Pointer<P>> allocatePointers(Class<P> type, int arrayLength) {
		return (Pointer<Pointer<P>>)(Pointer)allocate(PointerIO.getPointerInstance(), JNI.POINTER_SIZE * arrayLength); // TODO 
	}
	
    
    public static <V> Pointer<V> allocate(Class<V> elementClass) {
        return allocateArray(elementClass, 1);
    }
    
    public static <V> Pointer<V> allocate(PointerIO<V> io, long byteSize) {
        return byteSize == 0 ? null : new Memory<V>(io, byteSize);
    }
    
    public static <V> Pointer<V> allocateArray(Class<V> elementClass, int arrayLength) {
		if (arrayLength == 0)
			return null;
		
        #foreach ($prim in $primitivesNoBool)
        if (elementClass == ${prim.WrapperName}.TYPE || elementClass == ${prim.WrapperName}.class)
            return (Pointer<V>)allocate(PointerIO.get${prim.CapName}Instance(), ${prim.Size} * arrayLength);
        #end
        if (Pointer.class.isAssignableFrom(elementClass))
            return (Pointer<V>)allocate(PointerIO.getPointerInstance(), Pointer.SIZE * arrayLength); // TODO
        if (SizeT.class.isAssignableFrom(elementClass))
            return (Pointer<V>)allocate(PointerIO.getSizeTInstance(), SizeT.SIZE * arrayLength); // TODO
        if (StructObject.class.isAssignableFrom(elementClass)) {
        	CRuntime runtime = (CRuntime)BridJ.getRuntime(elementClass);
        	StructIO sio = StructIO.getInstance(elementClass, elementClass, runtime);
        	PointerIO pio = PointerIO.getInstance(sio);
        	return (Pointer<V>)allocate(pio, sio.getStructSize() * arrayLength); // TODO
        }
        //if (CLong.class.isAssignableFrom(elementClass))
        //    return (Pointer<V>)allocate(PointerIO.getPointerInstance(), Pointer.SIZE * arrayLength); // TODO
        throw new UnsupportedOperationException("Cannot allocate memory for type " + elementClass.getName());
    }

	public static Pointer<?> pointerToBuffer(Buffer buffer) {
		return pointerToBuffer(buffer, 0);
    }

    protected static Pointer<?> pointerToBuffer(Buffer buffer, long byteOffset) {
        if (buffer == null)
			return null;
		
		#foreach ($prim in $primitivesNoBool)
		if (buffer instanceof ${prim.BufferName})
			return (Pointer)pointerTo${prim.CapName}s((${prim.BufferName})buffer, byteOffset);
		#end
        throw new UnsupportedOperationException();
	}

#foreach ($prim in $primitivesNoBool)
    public static Pointer<${prim.WrapperName}> pointerTo${prim.CapName}(${prim.Name} value) {
        Pointer<${prim.WrapperName}> mem = allocate(PointerIO.get${prim.CapName}Instance(), ${prim.Size});
        mem.set${prim.CapName}(0, value);
        return mem;
    }
	
	public static Pointer<${prim.WrapperName}> pointerTo${prim.CapName}s(${prim.Name}... values) {
        Pointer<${prim.WrapperName}> mem = allocate(PointerIO.get${prim.CapName}Instance(), ${prim.Size} * values.length);
        mem.set${prim.CapName}s(0, values, 0, values.length);
        return mem;
    }
	
	public static Pointer<${prim.WrapperName}> pointerTo${prim.CapName}s(${prim.BufferName} buffer) {
		return pointerTo${prim.CapName}s(buffer, 0);
	}
	
    public static Pointer<${prim.WrapperName}> pointerTo${prim.CapName}s(${prim.BufferName} buffer, long byteOffset) {
        if (buffer == null)
			return null;
		
		PointerIO<${prim.WrapperName}> io = PointerIO.get${prim.CapName}Instance();
		Pointer<${prim.WrapperName}> pointer;
		if (buffer.isDirect()) {
			long address = JNI.getDirectBufferAddress(buffer);
			long size = JNI.getDirectBufferCapacity(buffer);
			if (address == 0 || size == 0)
				return null;
			
			pointer = new Memory(io, address, size, buffer).offset(byteOffset);
		} else {
			#if (${prim.Name} == "byte")
			pointer = new IndirectBufferPointer<${prim.WrapperName}>(io, buffer, byteOffset).order(buffer.order());
			#else
			throw new UnsupportedOperationException("Cannot create pointers to indirect ${prim.BufferName} buffers (try with a ByteBuffer or with any direct buffer)");
			#end
		}
		
		return pointer;
    }
	
	/*
	public static Pointer<${prim.WrapperName}> pointerTo(${prim.BufferName} buffer) {
		if (buffer == null)
			return null;

        PointerIO<${prim.WrapperName}> io = PointerIO.get${prim.CapName}Instance();
		if (buffer.isDirect()) {
			long address = JNI.getDirectBufferAddress(buffer);
			if (address == 0)
				return null;
	
			long size = JNI.getDirectBufferCapacity(buffer);
	
			Pointer<T> pointer = new Memory(PointerIO.getBufferPrimitiveInstance(buffer), address, size, buffer);
			#if (${prim.Name} != "byte")
			pointer.order(buffer.order());
			#end
			return pointer;
		}
		
		#if (${prim.Name} != "byte")
		return new IndirectBufferPointer<${prim.WrapperName}>(io, buffer);
		#else
		throw new UnsupportedOperationException("Cannot create pointers to indirect ${prim.BufferName} buffers (try with a ByteBuffer or with any direct buffer)");
		#end
    }
	*/
	
	
    public static Pointer<${prim.WrapperName}> allocate${prim.CapName}() {
        return allocate(PointerIO.get${prim.CapName}Instance(), ${prim.Size});
    }
    public static Pointer<${prim.WrapperName}> allocate${prim.CapName}s(long arrayLength) {
        return allocate(PointerIO.get${prim.CapName}Instance(), ${prim.Size} * arrayLength);
    }
#end

    protected PointerIO<T> getIO() {
        return io;
    }
    
	void setIO(PointerIO pointerIO) {
		this.io = pointerIO;	
	}
    
    public Type getTargetType() {
        PointerIO<T> io = getIO();
        return io == null ? null : io.getTargetType();
    }
    public <N> Pointer<N> setTargetType(Type type) {
        io = PointerIO.change(io, type);
        return (Pointer<N>)this;
    }
    public <N> Pointer<N> setTargetClass(Class<N> type) {
        return setTargetType((Type)type);
    }

    public <U> Pointer<U> getPointer(long byteOffset, PointerIO pio) {
        long peer = getSizeT(byteOffset);
        return peer == 0 ? null : new DefaultPointer(pio, getSizeT(byteOffset));
    }
    public Pointer<?> getPointer(long byteOffset) {
        return getPointer(byteOffset, (PointerIO)null);
    }
    public <U> Pointer<U> getPointer(long byteOffset, Type t) {
        return getPointer(byteOffset, t == null ? null : PointerIO.getInstanceByType(t));
    }
    public <U> Pointer<U> getPointer(long byteOffset, Class<U> t) {
        return getPointer(byteOffset, t == null ? null : PointerIO.getInstance(t));
    }
	
    public Pointer<?>[] getPointers(long byteOffset, int arrayLength) {
        return getPointers(byteOffset, arrayLength, (PointerIO)null);
    }
    public <U> Pointer<U>[] getPointers(long byteOffset, int arrayLength, Type t) {
        return getPointers(byteOffset, arrayLength, t == null ? null : PointerIO.getInstanceByType(t));
    }
    public <U> Pointer<U>[] getPointers(long byteOffset, int arrayLength, Class<U> t) {
        return getPointers(byteOffset, arrayLength, t == null ? null : PointerIO.getInstance(t));
    }
    
    public <U> Pointer<U>[] getPointers(long byteOffset, int arrayLength, PointerIO pio) {
    	Pointer<U>[] values = (Pointer<U>[])new Pointer[arrayLength];
		int s = JNI.POINTER_SIZE;
		for (int i = 0; i < arrayLength; i++)
			values[i] = getPointer(i * s, pio);
		return values;
	}
	public Pointer<T> setPointers(long byteOffset, Pointer<?>... values) {
		int n = values.length, s = JNI.POINTER_SIZE;
		for (int i = 0; i < n; i++)
			setPointer(i * s, values[i]);
		return this;
	}
	
	static final boolean is64 = JNI.POINTER_SIZE == 8; 
	
	public static Pointer<SizeT> pointerToSizeT(long value) {
		Pointer<SizeT> p = allocate(PointerIO.getSizeTInstance(), JNI.SIZE_T_SIZE);
		p.setSizeT(0, value);
		return p;
	}
	public static Pointer<SizeT> pointerToSizeTs(long... values) {
		int n = values.length, s = JNI.SIZE_T_SIZE;
		return allocate(PointerIO.getSizeTInstance(), s * n).setSizeTs(0, values);
	}
	
	public static Pointer<SizeT> pointerToSizeTs(int[] values) {
		int n = values.length, s = JNI.SIZE_T_SIZE;
		return allocate(PointerIO.getSizeTInstance(), s * n).setSizeTs(0, values);
	}
	
	public static <T> Pointer<Pointer<T>> pointerToPointer(Pointer<T> value) {
		// TODO
		Pointer<Pointer<T>> p = (Pointer<Pointer<T>>)(Pointer)allocate(PointerIO.getPointerInstance(/*value.getIO()*/), JNI.POINTER_SIZE);
		p.setPointer(0, value);
		return p;
	}
	public static <T> Pointer<Pointer<T>> pointerToPointers(Pointer<T>... values) {
		int n = values.length, s = JNI.POINTER_SIZE;
		// TODO
		Pointer<Pointer<T>> p = (Pointer<Pointer<T>>)(Pointer)allocate(PointerIO.getPointerInstance(/*values[0].getIO()*/), s * n);
		for (int i = 0; i < n; i++) {
			p.setPointer(i * s, values[i]);
		}
		return p;
	}
	
    public static Pointer<SizeT> allocateSizeTs(long arrayLength) {
		return allocate(PointerIO.getSizeTInstance(), JNI.SIZE_T_SIZE * arrayLength);
	}
	public static Pointer<SizeT> allocateSizeT() {
		return allocate(PointerIO.getSizeTInstance(), JNI.SIZE_T_SIZE);
	}
	
	public long getSizeT(long byteOffset) {
		return is64 ? getLong(byteOffset) : 0xffffffffL & getInt(byteOffset);
	}
	public long[] getSizeTs(long byteOffset, int arrayLength) {
		if (is64)  
			return getLongs(byteOffset, arrayLength);
		
		int[] values = getInts(byteOffset, arrayLength);
		long[] ret = new long[arrayLength];
		for (int i = 0; i < arrayLength; i++) {
			ret[i] = 0xffffffffL & values[i];
		}
		return ret;
	}
	
    public Pointer<T> setSizeT(long byteOffset, long value) {
		if (is64)
			setLong(byteOffset, value);
		else {
			setInt(byteOffset, SizeT.safeIntCast(value));
		}
		return this;
	}
	public Pointer<T> setSizeTs(long byteOffset, long... values) {
		if (is64) {
			setLongs(byteOffset, values);
		} else {
			int n = values.length, s = 4;
			for (int i = 0; i < n; i++)
				setInt(i * s, (int)values[i]);
		}
		return this;
	}
	
	public Pointer<T> setSizeTs(long byteOffset, int[] values) {
		if (!is64) {
			setInts(byteOffset, values);
		} else {
			int n = values.length, s = 8;
			for (int i = 0; i < n; i++)
				setLong(i * s, values[i]);
		}
		return this;
	}
	
	static Class<?> getPrimitiveType(Buffer buffer) {

        #foreach ($prim in $primitivesNoBool)
		if (buffer instanceof ${prim.BufferName})
			return ${prim.WrapperName}.TYPE;
		#end
        throw new UnsupportedOperationException();
    }
    
    public void setValues(long byteOffset, Buffer values, int valuesOffset, int length) {
        #foreach ($prim in $primitivesNoBool)
        if (values instanceof ${prim.BufferName}) {
            set${prim.CapName}s(byteOffset, (${prim.BufferName})values, valuesOffset, length);
            return;
        }
        #end
        throw new UnsupportedOperationException();
    }


#foreach ($prim in $primitivesNoBool)

	public abstract Pointer<T> set${prim.CapName}(long byteOffset, ${prim.Name} value);
	
	public Pointer<T> set${prim.CapName}(${prim.Name} value) {
		return set${prim.CapName}(0, value);
	}	
	
	public Pointer<T> set${prim.CapName}s(${prim.Name}[] values) {
		return set${prim.CapName}s(0, values, 0, values.length);
	}	
	
	public abstract Pointer<T> set${prim.CapName}s(long byteOffset, ${prim.Name}[] values, int valuesOffset, int length);

	public Pointer<T> set${prim.CapName}s(long byteOffset, ${prim.Name}[] values) {
        return set${prim.CapName}s(byteOffset, values, 0, values.length);
    }
    
	public Pointer<T> set${prim.CapName}s(${prim.BufferName} values) {
		return set${prim.CapName}s(0, values, 0, values.capacity());
	}

	public abstract Pointer<T> set${prim.CapName}s(long byteOffset, ${prim.BufferName} values, long valuesOffset, long length);

	public ${prim.BufferName} get${prim.BufferName}(long length) {
		return get${prim.BufferName}(0, length);
	}
    
    public abstract ${prim.BufferName} get${prim.BufferName}(long byteOffset, long length);
    
    public ${prim.Name} get${prim.CapName}() {
		return get${prim.CapName}(0);
	}

    public abstract ${prim.Name} get${prim.CapName}(long byteOffset);

    public ${prim.Name}[] get${prim.CapName}s(int length) {
    	return get${prim.CapName}s(length);
    }
    
    public abstract ${prim.Name}[] get${prim.CapName}s(long byteOffset, int length);

#end

    public enum StringType {
        Pascal, C, WideC
    }
	
	protected abstract long strlen(long byteOffset);
	
	public String getString(long byteOffset, Charset charset, StringType type) throws UnsupportedEncodingException {
        long len;
        if (type == StringType.Pascal) {
            len = getByte(byteOffset) & 0xff;
            byteOffset++;
		} else {
            len = strlen(byteOffset);
            if (len >= Integer.MAX_VALUE)
                throw new IllegalArgumentException("No null-terminated string at this address");

            if (type == StringType.WideC)
                throw new UnsupportedOperationException("Wide strings are not supported yet");
        }
		return new String(getBytes(byteOffset, SizeT.safeIntCast(len)), charset.name());
	}

    public Pointer<T> setCString(long byteOffset, String s) {
        try {
            return setString(byteOffset, s, Charset.defaultCharset(), StringType.C);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Unexpected error", ex);
        }
    }
    
	public Pointer<T> setString(long byteOffset, String s, Charset charset, StringType type) throws UnsupportedEncodingException {
        if (type == StringType.WideC)
            throw new UnsupportedOperationException("Wide strings are not supported yet");
        
        byte[] bytes = s.getBytes(charset.name());
        int bytesCount = bytes.length;

        if (type == StringType.Pascal) {
            if (bytesCount > 255)
                throw new IllegalArgumentException("Pascal strings cannot be more than 255 chars long (tried to write string of byte length " + bytesCount + ")");
            byteOffset++;
        }
        setBytes(byteOffset, bytes, 0, bytesCount);
        if (type == StringType.C)
            setByte(byteOffset + bytesCount, (byte)0);

        return this;
    }
	
	public String getPascalString(long byteOffset) {
		try {
            return getString(byteOffset, Charset.defaultCharset(), StringType.Pascal);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Unexpected error", ex);
        }
	}
	
	public String getWideCString(long byteOffset) {
		try {
            return getString(byteOffset, Charset.defaultCharset(), StringType.WideC);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Unexpected error", ex);
        }
	}
	
	@Deprecated
    public String getCString(long byteOffset, boolean wide) {
        try {
            return getString(byteOffset, Charset.defaultCharset(), wide ? StringType.WideC : StringType.C);
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Unexpected error", ex);
        }
    }
    
	public String getCString(long byteOffset) {
        return getCString(byteOffset, false);
    }
        
    public void setPointer(long byteOffset, Pointer value) {
        setSizeT(byteOffset, value == null ? 0 : value.getPeer());
    }

}
