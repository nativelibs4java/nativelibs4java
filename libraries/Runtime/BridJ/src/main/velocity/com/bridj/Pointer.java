package com.bridj;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.*;
import java.util.*;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * Pointer to a native memory location.<br/>
 * Pointer is the entry point of any pointer-related operation in BridJ.
 * <p>
 * <u><b>Manipulating memory</b></u>
 * <p>
 * <ul>
 *	<li>Wrapping a memory address as a pointer : {@link Pointer#pointerToAddress(long)}
 *  </li>
 *	<li>Reading / writing a primitive from / to the pointed memory location :<br/>
 *		#foreach ($prim in $primitivesNoBool)
 *		{@link Pointer#get${prim.CapName}()} / {@link Pointer#set${prim.CapName}(${prim.Name})} ; With an offset : {@link Pointer#get${prim.CapName}(long)} / {@link Pointer#set${prim.CapName}(long, ${prim.Name})}<br/>
 *       #end
 *		{@link Pointer#getSizeT()} / {@link Pointer#setSizeT(long)} ; With an offset : {@link Pointer#getSizeT(long)} / {@link Pointer#setSizeT(long, long)} <br/>
 *  </li>
 *	<li>Reading / writing an array of primitives from / to the pointed memory location :<br/>
 *		#foreach ($prim in $primitivesNoBool)
 *		{@link Pointer#get${prim.CapName}s(int)} / {@link Pointer#set${prim.CapName}s(${prim.Name}[])} ; With an offset : {@link Pointer#get${prim.CapName}s(long, int)} / {@link Pointer#set${prim.CapName}s(long, ${prim.Name}[])}<br/>
 *       #end
 *		{@link Pointer#getSizeTs(int)} / {@link Pointer#setSizeTs(long[])} ; With an offset : {@link Pointer#getSizeTs(long, int)} / {@link Pointer#setSizeTs(long, long[])}<br/>
 *  </li>
 *	<li>Reading / writing an NIO buffer of primitives from / to the pointed memory location :<br/>
 *		#foreach ($prim in $primitivesNoBool)
 *		{@link Pointer#get${prim.BufferName}(long)} (can be used for writing as well) / set${prim.CapName}s(${prim.BufferName})<br/>
 *       #end
 *  </li>
 * </ul>
 * <p>
 * <u><b>Allocating memory</b></u>
 * <p>
 * <ul>
 *	<li>Getting the pointer to a struct / a C++ class / a COM object :
 *		{@link Pointer#getPointer(NativeObject)}
 *  </li>
 *	<li>Allocating a primitive with / without an initial value (zero-initialized) :<br/>
 *		#foreach ($prim in $primitivesNoBool)
 *		{@link Pointer#pointerTo${prim.CapName}(${prim.Name})} / {@link Pointer#allocate${prim.CapName}()}<br/>
 *       #end
 *		{@link Pointer#pointerToSizeT(long)} / {@link Pointer#allocateSizeT()}<br/>
 *  </li>
 *	<li>Allocating an array of primitives with / without initial values (zero-initialized) :<br/>
 *		#foreach ($prim in $primitivesNoBool)
 *		{@link Pointer#pointerTo${prim.CapName}s(${prim.Name}[])} or {@link Pointer#pointerTo${prim.CapName}s(${prim.BufferName})} / {@link Pointer#allocate${prim.CapName}s(long)}<br/>
 *       #end
 *		{@link Pointer#pointerToSizeTs(long[])} / {@link Pointer#allocateSizeTs(long)}<br/>
 *		{@link Pointer#pointerToBuffer(Buffer)} / n/a<br/>
 *  </li>
 * </ul>
 */
public class Pointer<T> implements Comparable<Pointer<?>>, Iterable<T>
        //, com.sun.jna.Pointer<Pointer<T>>
{
    public static final Pointer NULL = null;
	public static final int SIZE = JNI.POINTER_SIZE;
    
	static {
        JNI.initLibrary();
    }
    
    
	private static long UNKNOWN_VALIDITY = -1;
	
	private final PointerIO<T> io;
	private final long peer, offsetInParent;
	private final Pointer<?> parent;
	private final Object sibling;
	private final long validStart, validEnd;
	private final boolean ordered;

	private volatile Releaser releaser;
	interface Releaser {
		void release(Pointer<?> p);
	}
	
	Pointer(PointerIO<T> io, long peer) {
		this(io, peer, true, UNKNOWN_VALIDITY, UNKNOWN_VALIDITY, null, 0, null, null);
	}
	Pointer(PointerIO<T> io, long peer, boolean ordered, long validStart, long validEnd, Pointer<?> parent, long offsetInParent, Object sibling, Releaser releaser) {
		this.io = io;
		this.peer = peer;
		this.ordered = ordered;
		this.validStart = validStart;
		this.validEnd = validEnd;
		this.parent = parent;
		this.offsetInParent = offsetInParent;
		this.sibling = sibling;
		this.releaser = releaser;
		assert !(sibling != null && releaser != null); // Pointer with both a sibling and a releaser !
	}
	public synchronized void release() {
		if (releaser != null) {
			releaser.release(this);
			releaser = null;
		}
	}

	/**
	 * Compare to another pointer based on pointed addresses.
	 * @param p other pointer
	 * @return 1 if this pointer's address is greater than p's (or if p is null), -1 if the opposite is true, 0 if this and p point to the same memory location.
	 */
	@Override
    public int compareTo(Pointer<?> p) {
		if (p == null)
			return 1;
		
		long p1 = getPeer(), p2 = p.getPeer();
		return p1 == p2 ? 0 : p1 < p2 ? -1 : 1;
	}
	
    /**
	 * Compute a hash code based on pointed address.
	 */
	@Override
    public int hashCode() {
		int hc = new Long(getPeer()).hashCode();
		return hc;
    }
    
    protected final long getCheckedPeer(long byteOffset, long validityCheckLength) {
    	long offsetPeer = getPeer() + byteOffset;
    	checkPeer(offsetPeer, validityCheckLength);
        return offsetPeer;
    }
    protected final void checkPeer(long peerToCheck, long validityCheckLength) {
		if (validStart == UNKNOWN_VALIDITY)
			return;
		
    	if (peerToCheck < validStart || (peerToCheck + validityCheckLength) > validEnd)
            throw new IndexOutOfBoundsException("Cannot access to memory data of length " + validityCheckLength + " at offset " + (peerToCheck - getPeer()) + " : valid memory start is " + validStart + ", valid memory size is " + (validEnd - validStart));
    }

    /**
	 * Returns a pointer which address value was obtained by this pointer's by adding a byte offset.<br/>
	 * The returned pointer will prevent the memory associated to this pointer from being automatically reclaimed as long as it lives, unless Pointer.release() is called on the originally-allocated pointer.
	 * @param byteOffset offset in bytes of the new pointer vs. this pointer. The expression {@code p.offset(byteOffset).getPeer() - p.getPeer() == byteOffset} is always true.
	 */
    public Pointer<T> offset(long byteOffset) {
    	return offset(byteOffset, getIO());
    }

    <U> Pointer<U> offset(long byteOffset, PointerIO<U> pio) {
    	if (byteOffset == 0)
			return pio == this.io ? (Pointer<U>)this : withIO(pio);
	
		long newPeer = getPeer() + byteOffset;
		
		if (validStart == UNKNOWN_VALIDITY)
			return new Pointer<U>(pio, newPeer, ordered, UNKNOWN_VALIDITY, UNKNOWN_VALIDITY, null, 0, this, releaser);	
		if (newPeer >= validEnd || newPeer < validStart)
			throw new IndexOutOfBoundsException("Invalid pointer offset !");
		
		return new Pointer<U>(pio, newPeer, ordered, validStart, validEnd, null, 0, getSibling() != null ? getSibling() : this, null);    	
	}
	/**
	 * Returns a pointer to this pointer.<br/>
	 * It will only succeed if this pointer was dereferenced from another pointer.<br/>
	 * Let's take the following C++ code :
	 * <pre>{@code
	int** pp = ...;
	int* p = pp[10];
	int** ref = &p;
	ASSERT(pp == ref);
	 }</pre>
	 * Here is its equivalent Java code :
	 * <pre>{@code
	Pointer<Pointer<Integer>> pp = ...;
	Pointer<Integer> p = pp.get(10);
	Pointer<Pointer<Integer>> ref = p.getReference();
	assert pp.equals(ref);
	 }</pre>
	 */
    public Pointer<Pointer<T>> getReference() {
		if (parent == null)
			throw new UnsupportedOperationException("Cannot get reference to this pointer, it wasn't created from Pointer.getPointer(offset) or from a similar method.");
		
		PointerIO io = getIO();
		return parent.offset(offsetInParent).withIO(io == null ? null : io.getReferenceIO());
	}
	public final long getPeer() {
		return peer;
	}
    
    
    /**
     * Cast this pointer to another pointer type
     * @param <U>
     * @param newIO
     * @return
     */
    public <U> Pointer<U> withIO(PointerIO<U> newIO) {
    	return cloneAs(isOrdered(), newIO);
    }
    /**
     * Create a clone of this pointer that has the byte order provided in argument, or return this if this pointer already uses the requested byte order.
     * @param order byte order (endianness) of the returned pointer
     */
    public Pointer<T> order(ByteOrder order) {
		if (order.equals(ByteOrder.nativeOrder()) == isOrdered())
			return this;
		
		return cloneAs(!isOrdered(), getIO());
	}
    
	/**
     * Get the byte order (endianness) of this pointer.
     */
    public ByteOrder order() {
		return isOrdered() ? ByteOrder.nativeOrder() : ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
    }

    <U> Pointer<U> cloneAs(boolean ordered, PointerIO<U> newIO) {
    	if (newIO == io && ordered == isOrdered())
    		return (Pointer<U>)this;
    	else
    		return new Pointer<U>(newIO, getPeer(), ordered, getValidStart(), getValidEnd(), getParent(), getOffsetInParent(), getSibling() != null ? getSibling() : this, null);
    }

    public final PointerIO<T> getIO() {
		return io;
	}
    protected final boolean isOrdered() {
    	return ordered;
    }
    protected final long getOffsetInParent() {
		return offsetInParent;
	}
    protected final Pointer<?> getParent() {
		return parent;
	}
    protected final Object getSibling() {
		return sibling;
	}
    
    protected final Releaser getReleaser() {
		return releaser;
	}
    protected final long getValidEnd() {
		return validEnd;
	}
    protected final long getValidStart() {
		return validStart;
	}

    /**
     * Cast this pointer to another pointer type
     * @param <U>
     * @param newIO
     * @return
     */
    public <U> Pointer<U> asPointerTo(Type type) {
    	PointerIO<U> pio = PointerIO.getInstance(type);
    	return withIO(pio);
    }

    /**
     * Cast this pointer to another pointer type
     * {@link Pointer#asPointerTo(Type)}
     * @param <U>
     * @param newIO
     * @return
     */
    public <U> Pointer<U> as(Class<U> type) {
    	return asPointerTo(type);
    }

    public <U> Pointer<U> getPointer(long byteOffset, PointerIO<U> pio) {
    	long value = getSizeT(byteOffset);
    	if (value == 0)
    		return null;
    	return new Pointer<U>(pio, value, isOrdered(), UNKNOWN_VALIDITY, UNKNOWN_VALIDITY, this, byteOffset, null, null);
    }

    /**
     * Write a pointer value to the pointed memory location
     */
    public Pointer<T> setPointer(Pointer<?> value) {
    	return setPointer(0, value);
    }
    public Pointer<T> setPointer(long byteOffset, Pointer<?> value) {
        setSizeT(byteOffset, value == null ? 0 : value.getPeer());
        return this;
    }
    
    /**
     * Get the amount of memory known to be valid from this pointer, or -1 if it is unknown.<br/>
     * Memory validity information is available when the pointer was created out of another pointer (with {@link #offset(long)}, {@link #next()}, {@link #next(long)}) or from a direct NIO buffer ({@link #pointerToBuffer(Buffer)}, {@link #pointerToInts(IntBuffer)}...)
     * @return amount of bytes that can be safely read or written from this pointer, or -1 if this amount is unknown
     */
    public long getRemainingBytes() {
    	long ve = getValidEnd();
    	return ve == UNKNOWN_VALIDITY ? -1 : ve - getPeer();
    }
    
    /**
    * Get the amount of memory known to be valid from this pointer (expressed in elements of the target type, see {@link #getTargetType()}) or -1 if it is unknown.<br/>
     * Memory validity information is available when the pointer was created out of another pointer (with {@link #offset(long)}, {@link #next()}, {@link #next(long)}) or from a direct NIO buffer ({@link #pointerToBuffer(Buffer)}, {@link #pointerToInts(IntBuffer)}...)
     * @return amount of elements that can be safely read or written from this pointer, or -1 if this amount is unknown
     */
    public long getRemainingElements() {
    	long bytes = getRemainingBytes();
    	long elementSize = getTargetSize();
    	if (bytes < 0 || elementSize <= 0)
    		return -1;
    	return bytes / elementSize;
    }
    
    /**
     * Returns an iterator over the elements pointed by this pointer.<br/>
     * If this pointer was allocated from Java with the allocateXXX, pointerToXXX methods (or is a view or a clone of such a pointer), the iteration is safely bounded.<br/>
     * If this iterator is just a wrapper for a native-allocated pointer (or a view / clone of such a pointer), iteration will go forever (until illegal areas of memory are reached and cause a JVM crash).
     */
    public Iterator<T> iterator() {
    	return new Iterator<T>() {
    		Pointer<T> next = Pointer.this.getRemainingElements() > 0 ? Pointer.this : null;
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
    
    
    /**
     * Get a pointer to a native object (C++ or ObjectiveC class, struct, union, callback...) 
     */
    public static <N extends NativeObject> Pointer<N> pointerTo(N instance) {
    		return getPointer(instance);
    }
    /**
     * Get a pointer to a native object (C++ or ObjectiveC class, struct, union, callback...) 
     */
    public static <N extends NativeObject> Pointer<N> getPointer(N instance) {
		return getPointer(instance, null);
    }
    
    /**
     * Get a pointer to a native object, specifying the type of the pointer's target.<br/>
     * In C++, the address of the pointer to an object as its canonical class is not always the same as the address of the pointer to the same object cast to one of its parent classes. 
     */
    public static <R extends NativeObject> Pointer<R> getPointer(NativeObject instance, Class<R> targetType) {
		return (Pointer<R>)instance.peer;
    }
    /**
    * Get the address of a native object, specifying the type of the pointer's target (same as {@code getPointer(instance, targetType).getPeer()}, see {@link getPointer(NativeObject, Class)}).<br/>
     * In C++, the address of the pointer to an object as its canonical class is not always the same as the address of the pointer to the same object cast to one of its parent classes. 
     */
    public static long getAddress(NativeObject instance, Class targetType) {
		return getPointer(instance, targetType).getPeer();
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
	 * @throw RuntimeException If the target type of this pointer is unknown
	 * @return getPeer() % alignment == 0
	 */
	public boolean isAligned() {
		PointerIO<T> io = getIO();
        if (io == null)
            throwBecauseUntyped("Cannot check alignment");

        return isAligned(io.getTargetAlignment());
	}
	
	/**
	 * Check that the pointer's peer is aligned to the given alignment.
	 * If the pointer has no peer, this method returns true.
	 * @return getPeer() % alignment == 0
	 */
	public boolean isAligned(int alignment) {
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
		Pointer<Byte> p = allocateBytes(bytes.length + 1);
        p.setBytes(0, bytes);
		p.setByte(bytes.length, (byte)0);
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
        Pointer<Pointer<Byte>> mem = allocate((PointerIO<Pointer<Byte>>)(PointerIO)PointerIO.getPointerInstance(Byte.class), len * Pointer.SIZE, new Releaser() {
        	@Override
        	public void release(Pointer<?> p) {
        		Pointer<Pointer<Byte>> mem = (Pointer<Pointer<Byte>>)p;
        		for (int i = 0; i < len; i++) {
        			Pointer<Byte> pp = mem.get(i);
        			if (pp != null)
        				strings[i] = pp.getCString(0);
        			pp = pointers[i];
        			if (pp != null)
        				pp.release();
                }
        	}
        });
        for (int i = 0; i < len; i++)
            mem.set(i, pointers[i] = pointerToCString(strings[i]));

		return mem;
    }

    /**
	 * Dereference this pointer (*ptr).<br/>
     Take the following C++ code fragment :
     <pre>{@code
     int* array = new int[10];
     for (int index = 0; index < 10; index++, array++) 
     	printf("%i\n", *array);
     }</pre>
     Here is its equivalent in Java :
     <pre>{@code
     import static com.bridj.Pointer.*;
     ...
     Pointer<Integer> array = allocateInts(10);
     for (int index = 0; index < 10; index++) { 
     	System.out.println("%i\n".format(array.get()));
     	array = array.next();
	 }
     }</pre>
     Here is a simpler equivalent in Java :
     <pre>{@code
     import static com.bridj.Pointer.*;
     ...
     Pointer<Integer> array = allocateInts(10);
     for (int value : array) // array knows its size, so we can iterate on it
     	System.out.println("%i\n".format(value));
     }</pre>
     @throws RuntimeException if called on an untyped {@code Pointer<?>} instance (see {@link  Pointer#getTargetType()}) 
	 */
    public T get() {
        return get(0);
    }
    
    /**
     Gets the n-th element from this pointer.<br/>
     This is equivalent to the C/C++ square bracket syntax.<br/>
     Take the following C++ code fragment :
     <pre>{@code
	int* array = new int[10];
	int index = 5;
	int value = array[index];
     }</pre>
     Here is its equivalent in Java :
     <pre>{@code
	import static com.bridj.Pointer.*;
	...
	Pointer<Integer> array = allocateInts(10);
	int index = 5;
	int value = array.get(index);
     }</pre>
     @param index offset in pointed elements at which the value should be copied. Can be negative if the pointer was offset and the memory before it is valid.
     @throws RuntimeException if called on an untyped {@code Pointer<?>} instance ({@link  Pointer#getTargetType()}) 
	 */
	public T get(int index) {
        PointerIO<T> io = getIO();
        if (io == null)
            throwBecauseUntyped("Cannot get pointed value");

        return io.get(this, index);
    }
    
    /**
	 Assign a value to the pointed memory location.<br/>
     Take the following C++ code fragment :
     <pre>{@code
	int* array = new int[10];
	for (int index = 0; index < 10; index++, array++) { 
		int value = index;
		*array = value;
	}
     }</pre>
     Here is its equivalent in Java :
     <pre>{@code
	import static com.bridj.Pointer.*;
	...
	Pointer<Integer> array = allocateInts(10);
	for (int index = 0; index < 10; index++) {
		int value = index;
		array.set(value);
		array = array.next();
	}
     }</pre>
     @throws RuntimeException if called on an untyped {@code Pointer<?>} instance ({@link  Pointer#getTargetType()}) 
	 */
    public void set(T value) {
        set(0, value);
    }
    
    void throwBecauseUntyped(String message) {
    	throw new RuntimeException("Pointer is not typed (call Pointer.asPointerTo(Type) to create a typed pointer) : " + message);
    }
    void throwUnexpected(Throwable ex) {
    	throw new RuntimeException("Unexpected error", ex);
    }
	/**
     Sets the n-th element from this pointer.<br/>
     This is equivalent to the C/C++ square bracket assignment syntax.<br/>
     Take the following C++ code fragment :
     <pre>{@code
     float* array = new float[10];
     int index = 5;
     float value = 12;
     array[index] = value;
     }</pre>
     Here is its equivalent in Java :
     <pre>{@code
     import static com.bridj.Pointer.*;
     ...
     Pointer<Float> array = allocateFloats(10);
     int index = 5;
     float value = 12;
     array.set(index, value);
     }</pre>
     @param index offset in pointed elements at which the value should be copied. Can be negative if the pointer was offset and the memory before it is valid.
     @param value value to set at pointed memory location
     @throws RuntimeException if called on an untyped {@code Pointer<?>} instance ({@link  Pointer#getTargetType()}) 
	 */
	public Pointer<T> set(int index, T value) {
        PointerIO<T> io = getIO();
        if (io == null)
            throwBecauseUntyped("Cannot set pointed value");
        
        io.set(this, index, value);
        return this;
    }
	
	public static long getPeer(Pointer<?> pointer) {
        return pointer == null ? 0 : pointer.getPeer();
    }
	
	public int getTargetSize() {
		PointerIO<T> io = getIO();
        if (io == null)
            throwBecauseUntyped("Cannot compute target size");

        return io.getTargetSize();
	}
	
	/**
	 * Returns a pointer to the next target.
	 * Same as incrementing a C pointer of delta elements, but creates a new pointer instance.
	 * @return next(1)
	 */
	public Pointer<T> next() {
		return next(1);
	}
	
	/**
	 * Returns a pointer to the n-th next (or previous) target.
	 * Same as incrementing a C pointer of delta elements, but creates a new pointer instance.
	 * @return offset(getTargetSize() * delta)
	 */
	public Pointer<T> next(long delta) {
		PointerIO<T> io = getIO();
        if (io == null)
            throwBecauseUntyped("Cannot get pointers to next or previous targets");

        return offset(io.getTargetSize() * delta);
	}
	
	public static void release(Pointer... pointers) {
    		for (Pointer pointer : pointers)
    			if (pointer != null)
    				pointer.release();
	}

    /**
	 * Test equality of the 
	 */
	@Override
    public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Pointer))
			return false;
		
		Pointer p = (Pointer)obj;
		return getPeer() == p.getPeer();
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
        return address == 0 ? null : new Pointer(null, address);
    }

    @Deprecated
    public static Pointer<?> pointerToAddress(long address, long size) {
        return pointerToAddress(address, size, null, null);
    }
    
    public static Pointer<?> pointerToAddress(long address, Class<?> type, final Releaser releaser) {
        return pointerToAddress(address, PointerIO.getInstance(type), releaser);
    }
    static <P> Pointer<P> pointerToAddress(long address, PointerIO<P> io) {
        return pointerToAddress(address, UNKNOWN_VALIDITY, io, null);
	}
	static <P> Pointer<P> pointerToAddress(long address, PointerIO<P> io, Releaser releaser) {
        return pointerToAddress(address, UNKNOWN_VALIDITY, io, releaser);
	}
	static <U> Pointer<U> pointerToAddress(long address, long size, PointerIO<U> io) {
		return pointerToAddress(address, size, io, null);
	}
	static <U> Pointer<U> pointerToAddress(long address, long size, PointerIO<U> io, Releaser releaser) {
		if (address == 0)
			return null;
		
		long start, end;
		if (size == UNKNOWN_VALIDITY)
			start = end = UNKNOWN_VALIDITY;
		else {
			start = address;
			end = address + size;
		}
		if (releaser == null)
			return new Pointer<U>(io, address, true, start, end, null, -1, null, null);
        else
			return new Pointer<U>(io, address, true, start, end, null, -1, null, releaser) {
				protected void finalize() {
					release();
				}
			};
    }
	
	@Deprecated
    public static Pointer<?> pointerToAddress(long address, Releaser releaser) {
		return pointerToAddress(address, UNKNOWN_VALIDITY, null, releaser);
	}
    
	public static Pointer<?> pointerToAddress(long address, long size, Releaser releaser) {
        return pointerToAddress(address, size, null, releaser);
    }
	
	@Deprecated
    public static <P> Pointer<P> pointerToAddress(long address, Class<P> targetClass) {
        return pointerToAddress(address, UNKNOWN_VALIDITY, (PointerIO<P>)PointerIO.getInstance(targetClass), null);
    }

    /**
     * Create a memory area large enough to hold a single typed pointer.
     * @param type type the the typed pointer
     * @return a pointer to a new memory area large enough to hold a single typed pointer
     */
    public static <P extends TypedPointer> Pointer<P> allocateTypedPointer(Class<P> type) {
    	return (Pointer<P>)(Pointer)allocate(PointerIO.getInstance(type), Pointer.SIZE);
    }
    /**
     * Create a memory area large enough to hold an array of arrayLength typed pointers.
     * @param type type the the typed pointers
     * @param arrayLength size of the allocated array, in elements
     * @return a pointer to a new memory area large enough to hold an array of arrayLength typed pointers
     */
    public static <P extends TypedPointer> Pointer<P> allocateTypedPointers(Class<P> type, long arrayLength) {
    	return (Pointer<P>)(Pointer)allocate(PointerIO.getInstance(type), Pointer.SIZE * arrayLength);
    }
    /**
     * Create a memory area large enough to hold a single typed pointer.
     * @param targetType target type of the pointer values to be stored in the allocated memory 
     * @return a pointer to a new memory area large enough to hold a single typed pointer
     */
    public static <P> Pointer<Pointer<P>> allocatePointer(Class<P> targetType) {
    	return (Pointer<Pointer<P>>)(Pointer)allocate(PointerIO.getPointerInstance(targetType), Pointer.SIZE); // TODO 
    }
    /**
     * Create a memory area large enough to hold a single untyped pointer.
     * @return a pointer to a new memory area large enough to hold a single untyped pointer
     */
    public static <V> Pointer<Pointer<?>> allocatePointer() {
    	return (Pointer)allocate(PointerIO.getPointerInstance(), Pointer.SIZE);
    }
    /**
     * Create a memory area large enough to hold an array of arrayLength untyped pointers.
     * @param arrayLength size of the allocated array, in elements
     * @return a pointer to a new memory area large enough to hold an array of arrayLength untyped pointers
     */
    public static Pointer<Pointer<?>> allocatePointers(int arrayLength) {
		return (Pointer<Pointer<?>>)(Pointer)allocate(PointerIO.getPointerInstance(), JNI.POINTER_SIZE * arrayLength); 
	}
	
    /**
     * Create a memory area large enough to hold an array of arrayLength typed pointers.
     * @param targetType target type of element pointers in the resulting pointer array. 
     * @param arrayLength size of the allocated array, in elements
     * @return a pointer to a new memory area large enough to hold an array of arrayLength typed pointers
     */
    public static <P> Pointer<Pointer<P>> allocatePointers(Class<P> targetType, int arrayLength) {
		return (Pointer<Pointer<P>>)(Pointer)allocate(PointerIO.getPointerInstance(targetType), JNI.POINTER_SIZE * arrayLength); // TODO 
	}
	
    
    /**
     * Create a memory area large enough to a single items of type elementClass.
     * @param elementClass type of the array elements
     * @return a pointer to a new memory area large enough to hold a single item of type elementClass.
     */
    public static <V> Pointer<V> allocate(Class<V> elementClass) {
        return allocateArray(elementClass, 1);
    }
    
    public static <V> Pointer<V> allocate(PointerIO<V> io, long byteSize) {
    	return allocate(io, byteSize, null);
    }
    static <V> Pointer<V> allocate(PointerIO<V> io, long byteSize, final Releaser beforeDeallocation) {
        if (byteSize == 0)
        	return null;
        
        long address = JNI.malloc(byteSize);
        if (address == 0)
        	throw new RuntimeException("Failed to allocate " + byteSize);

		JNI.memset(address, (byte)0, byteSize);        
        return pointerToAddress(address, byteSize, io, beforeDeallocation == null ? freeReleaser : new Releaser() {
        	@Override
        	public void release(Pointer<?> p) {
        		beforeDeallocation.release(p);
        		freeReleaser.release(p);
        	}
        });
    }
    static FreeReleaser freeReleaser = new FreeReleaser();
    static class FreeReleaser implements Releaser {
    	@Override
		public void release(Pointer<?> p) {
			assert p.getSibling() == null;
			assert p.validStart == p.getPeer();
    		JNI.free(p.getPeer());
    	}
    }
    
    /**
     * Create a memory area large enough to hold arrayLength items of type elementClass.
     * @param elementClass type of the array elements
     * @param arrayLength length of the array in elements
     * @return a pointer to a new memory area large enough to hold arrayLength items of type elementClass.  
     */
    public static <V> Pointer<V> allocateArray(Class<V> elementClass, int arrayLength) {
		if (arrayLength == 0)
			return null;
		
        #foreach ($prim in $primitivesNoBool)
        if (elementClass == ${prim.WrapperName}.TYPE || elementClass == ${prim.WrapperName}.class)
            return (Pointer<V>)allocate(PointerIO.get${prim.CapName}Instance(), ${prim.Size} * arrayLength);
        #end
        if (Pointer.class.isAssignableFrom(elementClass))
            return (Pointer<V>)allocate(PointerIO.getPointerInstance(elementClass), Pointer.SIZE * arrayLength); // TODO
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

    /**
     * Create a pointer to the memory location used by a direct NIO buffer.<br/>
     * The returned pointer (and its subsequent clones returned by {@link #clone()}, {@link #offset(long)} or {@link #next(long)}) retains a reference to the original NIO buffer, so its lifespan is at least that of the pointer.</br>
     * @throws UnsupportedOperationException if the buffer is not direct
     */
    public static Pointer<?> pointerToBuffer(Buffer buffer) {
        if (buffer == null)
			return null;
		
		#foreach ($prim in $primitivesNoBool)
		if (buffer instanceof ${prim.BufferName})
			return (Pointer)pointerTo${prim.CapName}s((${prim.BufferName})buffer);
		#end
        throw new UnsupportedOperationException();
	}

#foreach ($prim in $primitivesNoBool)
    /**
     * Allocate enough memory for a single ${prim.Name} value, copy the value provided in argument into it and return a pointer to that memory.<br/>
     * The memory will be automatically be freed when the pointer is garbage-collected or upon manual calls to Pointer.release().<br/>
     * The pointer won't be garbage-collected until all its clones / views are garbage-collected themselves (see {@link #clone()}, {@link #offset(long)}, {@link #next(int)}, {@link #next()}).<br/>
     * @param value initial value for the created memory location
     * @return pointer to a new memory location that initially contains the ${prim.Name} value given in argument
     */
    public static Pointer<${prim.WrapperName}> pointerTo${prim.CapName}(${prim.Name} value) {
        Pointer<${prim.WrapperName}> mem = allocate(PointerIO.get${prim.CapName}Instance(), ${prim.Size});
        mem.set${prim.CapName}(0, value);
        return mem;
    }
	
	/**
     * Allocate enough memory for values.length ${prim.Name} values, copy the values provided as argument into it and return a pointer to that memory.<br/>
     * The memory will be automatically be freed when the pointer is garbage-collected or upon manual calls to Pointer.release().<br/>
     * The pointer won't be garbage-collected until all its clones / views are garbage-collected themselves (see {@link #clone()}, {@link #offset(long)}, {@link #next(int)}, {@link #next()}).<br/>
     * The returned pointer is also an {@code Iterable<${prim.WrapperName}>} instance that can be safely iterated upon :
     <pre>{@code
     for (float f : pointerTo(1f, 2f, 3.3f))
     	System.out.println(f); }</pre>
     * @param values initial values for the created memory location
     * @return pointer to a new memory location that initially contains the ${prim.Name} consecutive values provided in argument
     */
    public static Pointer<${prim.WrapperName}> pointerTo${prim.CapName}s(${prim.Name}... values) {
        if (values == null)
			return null;
		Pointer<${prim.WrapperName}> mem = allocate(PointerIO.get${prim.CapName}Instance(), ${prim.Size} * values.length);
        mem.set${prim.CapName}s(0, values, 0, values.length);
        return mem;
    }
	
	/**
     * Create a pointer to the memory location used by a direct NIO ${prim.BufferName}}.<br/>
     * The returned pointer (and its subsequent clones returned by {@link #clone()}, {@link #offset(long)} or {@link #next(long)}) retains a reference to the original NIO buffer, so its lifespan is at least that of the pointer.</br>
     * @throws UnsupportedOperationException if the buffer is not direct
     */
    public static Pointer<${prim.WrapperName}> pointerTo${prim.CapName}s(${prim.BufferName} buffer) {
        if (buffer == null)
			return null;
		
		if (!buffer.isDirect())
			throw new UnsupportedOperationException("Cannot create pointers to indirect ${prim.BufferName} buffers");
		
		long address = JNI.getDirectBufferAddress(buffer);
		long size = JNI.getDirectBufferCapacity(buffer);
		if (address == 0 || size == 0)
			return null;
		
		PointerIO<${prim.WrapperName}> io = CommonPointerIOs.${prim.Name}IO;
		
		return new Pointer<${prim.WrapperName}>(io, address, buffer.order().equals(ByteOrder.nativeOrder()), address, address + size, null, -1, buffer, null);
		//return pointerToAddress(address, size, io);
    }
	
    /**
     * Allocate enough memory for a ${prim.Name} value and return a pointer to it.<br/>
     * The memory will be automatically be freed when the pointer is garbage-collected or upon manual calls to Pointer.release().<br/>
     * @return pointer to a single zero-initialized ${prim.Name} value
     */
    public static Pointer<${prim.WrapperName}> allocate${prim.CapName}() {
        return allocate(PointerIO.get${prim.CapName}Instance(), ${prim.Size});
    }
    /**
     * Allocate enough memory for arrayLength ${prim.Name} values and return a pointer to that memory.<br/>
     * The memory will be automatically be freed when the pointer is garbage-collected or upon manual calls to Pointer.release().<br/>
     * The pointer won't be garbage-collected until all its clones / views are garbage-collected themselves (see {@link #clone()}, {@link #offset(long)}, {@link #next(int)}, {@link #next()}).<br/>
     * The returned pointer is also an {@code Iterable<${prim.WrapperName}>} instance that can be safely iterated upon.
     * @return pointer to arrayLength zero-initialized ${prim.Name} consecutive values
     */
    public static Pointer<${prim.WrapperName}> allocate${prim.CapName}s(long arrayLength) {
        return allocate(PointerIO.get${prim.CapName}Instance(), ${prim.Size} * arrayLength);
    }
#end
    
    public Type getTargetType() {
        PointerIO<T> io = getIO();
        return io == null ? null : io.getTargetType();
    }
    public Pointer<?> getPointer(long byteOffset) {
        return getPointer(byteOffset, (PointerIO)null);
    }
    public <U> Pointer<U> getPointer(long byteOffset, Type t) {
        return getPointer(byteOffset, t == null ? null : (PointerIO<U>)PointerIO.getInstance(t));
    }
    public <U> Pointer<U> getPointer(long byteOffset, Class<U> t) {
        return getPointer(byteOffset, t == null ? null : (PointerIO<U>)PointerIO.getInstance(t));
    }
	
    public Pointer<?>[] getPointers(long byteOffset, int arrayLength) {
        return getPointers(byteOffset, arrayLength, (PointerIO)null);
    }
    public <U> Pointer<U>[] getPointers(long byteOffset, int arrayLength, Type t) {
        return getPointers(byteOffset, arrayLength, t == null ? null : (PointerIO<U>)PointerIO.getInstance(t));
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
		if (values == null)
			throw new IllegalArgumentException("Null values");
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
		if (values == null)
			return null;
		int n = values.length, s = JNI.SIZE_T_SIZE;
		return allocate(PointerIO.getSizeTInstance(), s * n).setSizeTs(0, values);
	}
	public static Pointer<SizeT> pointerToSizeTs(SizeT[] values) {
		if (values == null)
			return null;
		int n = values.length, s = JNI.SIZE_T_SIZE;
		return allocate(PointerIO.getSizeTInstance(), s * n).setSizeTs(0, values);
	}
	
	public static Pointer<SizeT> pointerToSizeTs(int[] values) {
		if (values == null)
			return null;
		int n = values.length, s = JNI.SIZE_T_SIZE;
		return allocate(PointerIO.getSizeTInstance(), s * n).setSizeTs(0, values);
	}
	
	public static <T> Pointer<Pointer<T>> pointerToPointer(Pointer<T> value) {
		// TODO
		Pointer<Pointer<T>> p = (Pointer<Pointer<T>>)(Pointer)allocate(PointerIO.getPointerInstance(), JNI.POINTER_SIZE);
		p.setPointer(0, value);
		return p;
	}
	public static <T> Pointer<Pointer<T>> pointerToPointers(Pointer<T>... values) {
		if (values == null)
			return null;
		int n = values.length, s = JNI.POINTER_SIZE;
		// TODO
		Pointer<Pointer<T>> p = (Pointer<Pointer<T>>)(Pointer)allocate(PointerIO.getPointerInstance(), s * n);
		for (int i = 0; i < n; i++) {
			p.setPointer(i * s, values[i]);
		}
		return p;
	}
	
	/**
     * Allocate enough memory for arrayLength size_t values and return a pointer to that memory.<br/>
     * The memory will be automatically be freed when the pointer is garbage-collected or upon manual calls to Pointer.release().<br/>
     * The pointer won't be garbage-collected until all its clones / views are garbage-collected themselves (see {@link #clone()}, {@link #offset(long)}, {@link #next(int)}, {@link #next()}).<br/>
     * The returned pointer is also an {@code Iterable<SizeT>} instance that can be safely iterated upon.
     * @return pointer to arrayLength zero-initialized ${prim.Name} consecutive values
     */
    public static Pointer<SizeT> allocateSizeTs(long arrayLength) {
		return allocate(PointerIO.getSizeTInstance(), JNI.SIZE_T_SIZE * arrayLength);
	}
	/**
     * Allocate enough memory for a size_t value and return a pointer to it.<br/>
     * The memory will be automatically be freed when the pointer is garbage-collected or upon manual calls to Pointer.release().<br/>
     * @return pointer to a single zero-initialized size_t value
     */
    public static Pointer<SizeT> allocateSizeT() {
		return allocate(PointerIO.getSizeTInstance(), JNI.SIZE_T_SIZE);
	}
	
	/**
     * Read a size_t value from the pointed memory location
     */
    public long getSizeT() {
		return getSizeT(0);
	}
	/**
     * Read a size_t value from the pointed memory location shifted by a byte offset
     */
    public long getSizeT(long byteOffset) {
		return is64 ? getLong(byteOffset) : 0xffffffffL & getInt(byteOffset);
	}
	/**
     * Read an array of size_t values of the specified size from the pointed memory location
     */
    public long[] getSizeTs(int arrayLength) {
		return getSizeTs(0, arrayLength);
	}
	/**
     * Read an array of size_t values of the specified size from the pointed memory location shifted by a byte offset
     */
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
	
	/**
     * Write a size_t value to the pointed memory location
     */
    public Pointer<T> setSizeT(long value) {
		return setSizeT(0, value);
	}
    /**
     * Write a size_t value to the pointed memory location
     */
    public Pointer<T> setSizeT(SizeT value) {
		return setSizeT(0, value);
	}
    /**
     * Write a size_t value to the pointed memory location shifted by a byte offset
     */
    public Pointer<T> setSizeT(long byteOffset, long value) {
		if (is64)
			setLong(byteOffset, value);
		else {
			setInt(byteOffset, SizeT.safeIntCast(value));
		}
		return this;
	}
	
    /**
     * Write a size_t value to the pointed memory location shifted by a byte offset
     */
    public Pointer<T> setSizeT(long byteOffset, SizeT value) {
		return setSizeT(byteOffset, value.longValue());
	}
	/**
     * Write an array of size_t values to the pointed memory location
     */
    public Pointer<T> setSizeTs(long[] values) {
		return setSizeTs(0, values);
	}
	/**
     * Write an array of size_t values to the pointed memory location
     */
    public Pointer<T> setSizeTs(int[] values) {
		return setSizeTs(0, values);
	}
	/**
     * Write an array of size_t values to the pointed memory location
     */
    public Pointer<T> setSizeTs(SizeT[] values) {
		return setSizeTs(0, values);
	}
	/**
     * Write an array of size_t values to the pointed memory location shifted by a byte offset
     */
    public Pointer<T> setSizeTs(long byteOffset, long[] values) {
		if (is64) {
			setLongs(byteOffset, values);
		} else {
			int n = values.length, s = 4;
			for (int i = 0; i < n; i++)
				setInt(i * s, (int)values[i]);
		}
		return this;
	}
	/**
     * Write an array of size_t values to the pointed memory location shifted by a byte offset
     */
    public Pointer<T> setSizeTs(long byteOffset, SizeT... values) {
		if (values == null)
			throw new IllegalArgumentException("Null values");
		int n = values.length, s = 4;
		for (int i = 0; i < n; i++)
			setSizeT(i * s, values[i].longValue());
		return this;
	}
	/**
     * Write an array of size_t values to the pointed memory location shifted by a byte offset
     */
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

    /**
     * Copy bytes from the memory location indicated by this pointer to that of another pointer (with byte offsets for both the source and the destination).
     */
    public void copyTo(long byteOffset, Pointer<?> destination, long byteOffsetInDestination, long byteCount) {
    		JNI.memcpy(destination.getCheckedPeer(byteOffsetInDestination, byteCount), getCheckedPeer(byteOffset, byteCount), byteCount);
    }
    
    /**
    * Copy remaining bytes from this pointer to a destination (see {@link #copyTo(long, Pointer, long, long)}, {@link #getRemainingBytes})
     */
    public void copyTo(Pointer<?> destination) {
    		copyTo(0, destination, 0, getRemainingBytes());
    }

#foreach ($prim in $primitivesNoBool)

	/**
	 * Write a ${prim.Name} value to the pointed memory location
	 */
    public Pointer<T> set${prim.CapName}(${prim.Name} value) {
		return set${prim.CapName}(0, value);
	}
	
	/**
	 * Read a ${prim.Name} value from the pointed memory location shifted by a byte offset
	 */
    public Pointer<T> set${prim.CapName}(long byteOffset, ${prim.Name} value) {
    	#if ($prim.Name != "byte" && $prim.Name != "float" && $prim.Name != "double")
        if (isOrdered())
        	JNI.set_${prim.Name}(getCheckedPeer(byteOffset, ${prim.Size}), value);
        else
        	JNI.set_${prim.Name}_disordered(getCheckedPeer(byteOffset, ${prim.Size}), value);
        #else
		JNI.set_${prim.Name}(getCheckedPeer(byteOffset, ${prim.Size}), value);
        #end
        return this;
    }

	
	/**
	 * Write an array of ${prim.Name} values of the specified length to the pointed memory location
	 */
    public Pointer<T> set${prim.CapName}s(${prim.Name}[] values) {
		return set${prim.CapName}s(0, values, 0, values.length);
	}	
	
	/**
	 * Write an array of ${prim.Name} values of the specified length to the pointed memory location shifted by a byte offset
	 */
    public Pointer<T> set${prim.CapName}s(long byteOffset, ${prim.Name}[] values) {
        return set${prim.CapName}s(byteOffset, values, 0, values.length);
    }
    
    /**
	 * Write an array of ${prim.Name} values of the specified length to the pointed memory location shifted by a byte offset, reading values at the given array offset and for the given length from the provided array.
	 */
    public Pointer<T> set${prim.CapName}s(long byteOffset, ${prim.Name}[] values, int valuesOffset, int length) {
        #if ($prim.Name != "byte" && $prim.Name != "float" && $prim.Name != "double")
        if (isOrdered())
        	JNI.set_${prim.Name}_array(getCheckedPeer(byteOffset, ${prim.Size} * length), values, valuesOffset, length);
        else
        	JNI.set_${prim.Name}_array_disordered(getCheckedPeer(byteOffset, ${prim.Size} * length), values, valuesOffset, length);
        #else
		JNI.set_${prim.Name}_array(getCheckedPeer(byteOffset, ${prim.Size} * length), values, valuesOffset, length);
		#end
        	
        return this;
    }
    
	/**
	 * Write a buffer of ${prim.Name} values of the specified length to the pointed memory location
	 */
    public Pointer<T> set${prim.CapName}s(${prim.BufferName} values) {
		return set${prim.CapName}s(0, values, 0, values.capacity());
	}

    /**
	 * Write a buffer of ${prim.Name} values of the specified length to the pointed memory location shifted by a byte offset, reading values at the given buffer offset and for the given length from the provided buffer.
	 */
    public Pointer<T> set${prim.CapName}s(long byteOffset, ${prim.BufferName} values, long valuesOffset, long length) {
        if (values == null)
			throw new IllegalArgumentException("Null values");
		if (values.isDirect()) {
            long len = length * ${prim.Size}, off = valuesOffset * ${prim.Size};
            long cap = JNI.getDirectBufferCapacity(values);
            if (cap < off + len)
                throw new IndexOutOfBoundsException("The provided buffer has a capacity (" + cap + " bytes) smaller than the requested write operation (" + len + " bytes starting at byte offset " + off + ")");
            
			JNI.memcpy(getCheckedPeer(byteOffset, ${prim.Size} * length), JNI.getDirectBufferAddress(values) + off, len);
        } else if (values.isReadOnly()) {
            get${prim.BufferName}(byteOffset, length).put(values.duplicate());
        } else {
            set${prim.CapName}s(byteOffset, values.array(), (int)(values.arrayOffset() + valuesOffset), (int)length);
        }
        return this;
    }
    
	/**
	 * Read a buffer of ${prim.Name} values of the specified length from the pointed memory location 
	 */
    public ${prim.BufferName} get${prim.BufferName}(long length) {
		return get${prim.BufferName}(0, length);
	}
	
	/**
	 * Read a buffer of ${prim.Name} values of the remaining length from the pointed memory location 
	 */
    public ${prim.BufferName} get${prim.BufferName}() {
    		long rem = getRemainingElements();
    		if (rem < 0)
    			throwBecauseUntyped("Cannot create buffer if remaining length is not known. Please use get${prim.BufferName}(long length) instead.");
		return get${prim.BufferName}(0, rem);
	}
	
	/**
	 * Read a buffer of ${prim.Name} values of the specified length from the pointed memory location shifted by a byte offset
	 */
    public ${prim.BufferName} get${prim.BufferName}(long byteOffset, long length) {
        long blen = ${prim.Size} * length;
        ByteBuffer buffer = JNI.newDirectByteBuffer(getCheckedPeer(byteOffset, blen), blen);
		buffer.order(order());
        #if ($prim.Name == "byte")
        return buffer;
        #else
        return buffer.as${prim.BufferName}();
        #end
    }
    
    
	/**
	 * Read a ${prim.Name} value from the pointed memory location
	 */
    public ${prim.Name} get${prim.CapName}() {
		return get${prim.CapName}(0);
	}
	
    /**
	 * Read a ${prim.Name} value from the pointed memory location shifted by a byte offset
	 */
    public ${prim.Name} get${prim.CapName}(long byteOffset) {
        #if ($prim.Name != "byte" && $prim.Name != "float" && $prim.Name != "double")
        if (isOrdered())
        	return JNI.get_${prim.Name}(getCheckedPeer(byteOffset, ${prim.Size}));
        else
        	return JNI.get_${prim.Name}_disordered(getCheckedPeer(byteOffset, ${prim.Size}));
        #else
        return JNI.get_${prim.Name}(getCheckedPeer(byteOffset, ${prim.Size}));
        #end
        	
    }

    /**
	 * Read an array of ${prim.Name} values of the specified length from the pointed memory location
	 */
    public ${prim.Name}[] get${prim.CapName}s(int length) {
    	return get${prim.CapName}s(0, length);
    }
    
    /**
	 * Read an array of ${prim.Name} values of the specified length from the pointed memory location shifted by a byte offset
	 */
    public ${prim.Name}[] get${prim.CapName}s(long byteOffset, int length) {
        #if ($prim.Name != "byte" && $prim.Name != "float" && $prim.Name != "double")
        if (isOrdered())
        	return JNI.get_${prim.Name}_array(getCheckedPeer(byteOffset, ${prim.Size} * length), length);
        else
        	return JNI.get_${prim.Name}_array_disordered(getCheckedPeer(byteOffset, ${prim.Size} * length), length);
        #else
        return JNI.get_${prim.Name}_array(getCheckedPeer(byteOffset, ${prim.Size} * length), length);
        #end
        	
    }
    
#end

    public enum StringType {
        Pascal, C, WideC
    }
	
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
            throwUnexpected(ex);
            return null;
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
            throwUnexpected(ex);
            return null;
        }
	}
	
	public String getWideCString(long byteOffset) {
		try {
            return getString(byteOffset, Charset.defaultCharset(), StringType.WideC);
        } catch (UnsupportedEncodingException ex) {
            throwUnexpected(ex);
            return null;
        }
	}
	
	@Deprecated
    public String getCString(long byteOffset, boolean wide) {
        try {
            return getString(byteOffset, Charset.defaultCharset(), wide ? StringType.WideC : StringType.C);
        } catch (UnsupportedEncodingException ex) {
            throwUnexpected(ex);
            return null;
        }
    }
    
	public String getCString(long byteOffset) {
        return getCString(byteOffset, false);
    }

    protected long strlen(long byteOffset) {
		return JNI.strlen(getCheckedPeer(byteOffset, 1));
	}
}
