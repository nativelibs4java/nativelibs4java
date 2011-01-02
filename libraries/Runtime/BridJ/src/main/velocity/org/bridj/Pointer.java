package org.bridj;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.Array;
import java.nio.*;
import java.util.*;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import static org.bridj.SizeT.safeIntCast;

/**
 * Pointer to a native memory location.<br>
 * Pointer is the entry point of any pointer-related operation in BridJ.
 * <p>
 * <u><b>Manipulating memory</b></u>
 * <p>
 * <ul>
 *	<li>Wrapping a memory address as a pointer : {@link Pointer#pointerToAddress(long)}
 *  </li>
 *	<li>Reading / writing a primitive from / to the pointed memory location :<br>
#foreach ($prim in $primitives)
 *		{@link Pointer#get${prim.CapName}()} / {@link Pointer#set${prim.CapName}(${prim.Name})} ; With an offset : {@link Pointer#get${prim.CapName}(long)} / {@link Pointer#set${prim.CapName}(long, ${prim.Name})}<br>
#end
#foreach ($sizePrim in ["SizeT", "CLong"])
 *		{@link Pointer#get${sizePrim}()} / {@link Pointer#set${sizePrim}(long)} ; With an offset : {@link Pointer#get${sizePrim}(long)} / {@link Pointer#set${sizePrim}(long, long)} <br>
#end
 *  </li>
 *	<li>Reading / writing an array of primitives from / to the pointed memory location :<br>
#foreach ($prim in $primitives)
 *		{@link Pointer#get${prim.CapName}s(int)} / {@link Pointer#set${prim.CapName}s(${prim.Name}[])} ; With an offset : {@link Pointer#get${prim.CapName}s(long, int)} / {@link Pointer#set${prim.CapName}s(long, ${prim.Name}[])}<br>
#end
#foreach ($sizePrim in ["SizeT", "CLong"])
 *		{@link Pointer#get${sizePrim}s(int)} / {@link Pointer#set${sizePrim}s(long[])} ; With an offset : {@link Pointer#get${sizePrim}s(long, int)} / {@link Pointer#set${sizePrim}s(long, long[])}<br>
#end
 *  </li>
 *	<li>Reading / writing an NIO buffer of primitives from / to the pointed memory location :<br>
#foreach ($prim in $primitivesNoBool)
*		{@link Pointer#get${prim.BufferName}(long)} (can be used for writing as well) / {@link Pointer#set${prim.CapName}s(${prim.BufferName})}<br>
#end
 *  </li>
 *  <li>Reading / writing a String from / to the pointed memory location using the default charset :<br>
#foreach ($string in ["C", "WideC"])
*		{@link Pointer#get${string}String()} / {@link Pointer#set${string}String(String)} ; With an offset : {@link Pointer#get${string}String(long)} / {@link Pointer#set${string}String(long, String)}<br>
#end
 *  </li>
 *  <li>Reading / writing a String with control on the charset :<br>
 *		{@link Pointer#getString(long, StringType, Charset)} / {@link Pointer#setString(long, String, StringType, Charset)}<br>
 * </ul>
 * <p>
 * <u><b>Allocating memory</b></u>
 * <p>
 * <ul>
 *	<li>Getting the pointer to a struct / a C++ class / a COM object :
 *		{@link Pointer#pointerTo(NativeObject)}
 *  </li>
 *	<li>Allocating a primitive with / without an initial value (zero-initialized) :<br>
#foreach ($prim in $primitives)
 *		{@link Pointer#pointerTo${prim.CapName}(${prim.Name})} / {@link Pointer#allocate${prim.CapName}()}<br>
#end
#foreach ($sizePrim in ["SizeT", "CLong"])
 *		{@link Pointer#pointerTo${sizePrim}(long)} / {@link Pointer#allocate${sizePrim}()}<br>
#end
 *  </li>
 *	<li>Allocating an array of primitives with / without initial values (zero-initialized) :<br>
#foreach ($prim in $primitives)
 *		{@link Pointer#pointerTo${prim.CapName}s(${prim.Name}[])} or {@link Pointer#pointerTo${prim.CapName}s(${prim.BufferName})} / {@link Pointer#allocate${prim.CapName}s(long)}<br>
#end
#foreach ($sizePrim in ["SizeT", "CLong"])
 *		{@link Pointer#pointerTo${sizePrim}s(long[])} / {@link Pointer#allocate${sizePrim}s(long)}<br>
#end
 *		{@link Pointer#pointerToBuffer(Buffer)} / n/a<br>
 *  </li>
 *  <li>Allocating a native String :<br>
#foreach ($string in ["C", "WideC", "Pascal", "WidePascal"])
*		{@link Pointer#pointerTo${string}String(String)} (default charset)<br>
#end
 *		{@link Pointer#pointerToString(String, StringType, Charset)}<br>
 *  </li>
 * </ul>
 */
public class Pointer<T> implements Comparable<Pointer<?>>, List<T>//Iterable<T>
        //, com.sun.jna.Pointer<Pointer<T>>
{
	
#macro (docAllocateCopy $cPrimName $primWrapper)
	/**
     * Allocate enough memory for a single $cPrimName value, copy the value provided in argument into it and return a pointer to that memory.<br>
     * The memory will be automatically be freed when the pointer is garbage-collected or upon manual calls to {@link Pointer#release()}.<br>
     * The pointer won't be garbage-collected until all its clones / views are garbage-collected themselves (see {@link Pointer#clone()}, {@link Pointer#offset(long)}, {@link Pointer#next(long)}, {@link Pointer#next()}).<br>
     * @param value initial value for the created memory location
     * @return pointer to a new memory location that initially contains the $cPrimName value given in argument
     */
#end
#macro (docAllocateArrayCopy $cPrimName $primWrapper)
	/**
     * Allocate enough memory for values.length $cPrimName values, copy the values provided as argument into it and return a pointer to that memory.<br>
     * The memory will be automatically be freed when the pointer is garbage-collected or upon manual calls to {@link Pointer#release()}.<br>
     * The pointer won't be garbage-collected until all its clones / views are garbage-collected themselves (see {@link Pointer#clone()}, {@link Pointer#offset(long)}, {@link Pointer#next(long)}, {@link Pointer#next()}).<br>
     * The returned pointer is also an {@code Iterable<$primWrapper>} instance that can be safely iterated upon :
     <pre>{@code
     for (float f : pointerTo(1f, 2f, 3.3f))
     	System.out.println(f); }</pre>
     * @param values initial values for the created memory location
     * @return pointer to a new memory location that initially contains the $cPrimName consecutive values provided in argument
     */
#end
#macro (docAllocateArray2DCopy $cPrimName $primWrapper)
    /**
     * Allocate enough memory for all the values in the 2D $cPrimName array, copy the values provided as argument into it as packed multi-dimensional C array and return a pointer to that memory.<br>
     * Assumes that all of the subarrays of the provided array are non null and have the same size.<br>
     * The memory will be automatically be freed when the pointer is garbage-collected or upon manual calls to {@link Pointer#release()}.<br>
     * The pointer won't be garbage-collected until all its clones / views are garbage-collected themselves (see {@link Pointer#clone()}, {@link Pointer#offset(long)}, {@link Pointer#next(long)}, {@link Pointer#next()}).<br>
     * @param values initial values for the created memory location
     * @return pointer to a new memory location that initially contains the $cPrimName values provided in argument packed as a 2D C array would be
     */
#end
#macro (docAllocateArray3DCopy $cPrimName $primWrapper)
    /**
     * Allocate enough memory for all the values in the 3D $cPrimName array, copy the values provided as argument into it as packed multi-dimensional C array and return a pointer to that memory.<br>
     * Assumes that all of the subarrays of the provided array are non null and have the same size.<br>
     * The memory will be automatically be freed when the pointer is garbage-collected or upon manual calls to {@link Pointer#release()}.<br>
     * The pointer won't be garbage-collected until all its clones / views are garbage-collected themselves (see {@link Pointer#clone()}, {@link Pointer#offset(long)}, {@link Pointer#next(long)}, {@link Pointer#next()}).<br>
     * @param values initial values for the created memory location
     * @return pointer to a new memory location that initially contains the $cPrimName values provided in argument packed as a 3D C array would be
     */
#end
#macro (docAllocate $cPrimName $primWrapper)
	/**
     * Allocate enough memory for a $cPrimName value and return a pointer to it.<br>
     * The memory will be automatically be freed when the pointer is garbage-collected or upon manual calls to {@link Pointer#release()}.<br>
     * @return pointer to a single zero-initialized $cPrimName value
     */
#end
#macro (docAllocateArray $cPrimName $primWrapper)
	/**
     * Allocate enough memory for arrayLength $cPrimName values and return a pointer to that memory.<br>
     * The memory will be automatically be freed when the pointer is garbage-collected or upon manual calls to {@link Pointer#release()}.<br>
     * The pointer won't be garbage-collected until all its clones / views are garbage-collected themselves (see {@link Pointer#clone()}, {@link Pointer#offset(long)}, {@link Pointer#next(long)}, {@link Pointer#next()}).<br>
     * The returned pointer is also an {@code Iterable<$primWrapper>} instance that can be safely iterated upon.
     * @return pointer to arrayLength zero-initialized $cPrimName consecutive values
     */
#end
#macro (docAllocateArray2D $cPrimName $primWrapper)
	/**
     * Allocate enough memory for dim1 * dim2 $cPrimName values in a packed multi-dimensional C array and return a pointer to that memory.<br>
     * The memory will be automatically be freed when the pointer is garbage-collected or upon manual calls to {@link Pointer#release()}.<br>
     * The pointer won't be garbage-collected until all its clones / views are garbage-collected themselves (see {@link Pointer#clone()}, {@link Pointer#offset(long)}, {@link Pointer#next(long)}, {@link Pointer#next()}).<br>
     * @return pointer to dim1 * dim2 zero-initialized $cPrimName consecutive values
     */
#end
#macro (docAllocateArray3D $cPrimName $primWrapper)
	/**
     * Allocate enough memory for dim1 * dim2 * dim3 $cPrimName values in a packed multi-dimensional C array and return a pointer to that memory.<br>
     * The memory will be automatically be freed when the pointer is garbage-collected or upon manual calls to {@link Pointer#release()}.<br>
     * The pointer won't be garbage-collected until all its clones / views are garbage-collected themselves (see {@link Pointer#clone()}, {@link Pointer#offset(long)}, {@link Pointer#next(long)}, {@link Pointer#next()}).<br>
     * @return pointer to dim1 * dim2 * dim3 zero-initialized $cPrimName consecutive values
     */
#end
#macro (docGet $cPrimName $primWrapper)
	/**
     * Read a $cPrimName value from the pointed memory location
     */
#end
#macro (docGetOffset $cPrimName $primWrapper $signatureWithoutOffset)
	/**
     * Read a $cPrimName value from the pointed memory location shifted by a byte offset
     * @deprecated Avoid using the byte offset methods variants unless you know what you're doing (may cause alignment issues). Please favour {@link $signatureWithoutOffset} over this method. 
	 */
#end
#macro (docGetArray $cPrimName $primWrapper)
	/**
     * Read an array of $cPrimName values of the specified length from the pointed memory location
     */
#end
#macro (docGetRemainingArray $cPrimName $primWrapper)
	/**
     * Read the array of remaining $cPrimName values from the pointed memory location
     */
#end
#macro (docGetArrayOffset $cPrimName $primWrapper $signatureWithoutOffset)
	/**
     * Read an array of $cPrimName values of the specified length from the pointed memory location shifted by a byte offset
     * @deprecated Avoid using the byte offset methods variants unless you know what you're doing (may cause alignment issues). Please favour {@link $signatureWithoutOffset} over this method. 
	 */
#end
#macro (docSet $cPrimName $primWrapper)
	/**
     * Write a $cPrimName value to the pointed memory location
     */
#end
#macro (docSetOffset $cPrimName $primWrapper $signatureWithoutOffset)
    /**
     * Write a $cPrimName value to the pointed memory location shifted by a byte offset
     * @deprecated Avoid using the byte offset methods variants unless you know what you're doing (may cause alignment issues). Please favour {@link $signatureWithoutOffset} over this method. 
	 */
#end
#macro (docSetArray $cPrimName $primWrapper)
	/**
     * Write an array of $cPrimName values to the pointed memory location
     */
#end
#macro (docSetArrayOffset $cPrimName $primWrapper $signatureWithoutOffset)
	/**
     * Write an array of $cPrimName values to the pointed memory location shifted by a byte offset
     * @deprecated Avoid using the byte offset methods variants unless you know what you're doing (may cause alignment issues). Please favour {@link $signatureWithoutOffset} over this method. 
	 */
#end
	
	/** The NULL pointer is <b>always</b> Java's null value */
    public static final Pointer NULL = null;
	
    /** 
     * Size of a pointer in bytes. <br>
     * This is 4 bytes in a 32 bits environment and 8 bytes in a 64 bits environment.<br>
     * Note that some 64 bits environments allow for 32 bits JVM execution (using the -d32 command line argument for Sun's JVM, for instance). In that case, Java programs will believe they're executed in a 32 bits environment. 
     */
    public static final int SIZE = JNI.POINTER_SIZE;
    
	static {
        JNI.initLibrary();
    }
    
    
	private static long UNKNOWN_VALIDITY = -1;
	private static long NO_PARENT = 0/*-1*/;
	
	private final PointerIO<T> io;
	private final long peer, offsetInParent;
	private final Pointer<?> parent;
	private Object sibling;
	private final long validStart, validEnd;
	private final boolean ordered;

	/**
	 * Object responsible for reclamation of some pointed memory when it's not used anymore.
	 */
	public interface Releaser {
		void release(Pointer<?> p);
	}
	
	Pointer(PointerIO<T> io, long peer) {
		this(io, peer, true, UNKNOWN_VALIDITY, UNKNOWN_VALIDITY, null, 0, null);
	}
	Pointer(PointerIO<T> io, long peer, boolean ordered, long validStart, long validEnd, Pointer<?> parent, long offsetInParent, Object sibling) {
		this.io = io;
		this.peer = peer;
		this.ordered = ordered;
		this.validStart = validStart;
		this.validEnd = validEnd;
		this.parent = parent;
		this.offsetInParent = offsetInParent;
		this.sibling = sibling;
	}
	
	/**
	 * Create a {@code Pointer<T>} type. <br>
	 * For Instance, {@code Pointer.pointerType(Integer.class) } returns a type that represents {@code Pointer<Integer> }  
	 */
	public static Type pointerType(Type targetType) {
		return org.bridj.util.DefaultParameterizedType.paramType(Pointer.class, targetType);	
	}
	/**
	 * Create a {@code IntValuedEnum<T>} type. <br>
	 * For Instance, {@code Pointer.intEnumType(SomeEnum.class) } returns a type that represents {@code IntValuedEnum<SomeEnum> }  
	 */
	public static <E extends Enum<E>> Type intEnumType(Class<? extends IntValuedEnum<E>> targetType) {
		return org.bridj.util.DefaultParameterizedType.paramType(IntValuedEnum.class, targetType);	
	}
	
	/**
	 * Manually release the memory pointed by this pointer if it was allocated on the Java side.<br>
	 * If the pointer is an offset version of another pointer (using {@link Pointer#offset(long)} or {@link Pointer#next(long)}, for instance), this method tries to release the original pointer.<br>
	 * If the memory was not allocated from the Java side, this method does nothing either.<br>
	 * If the memory was already successfully released, this throws a RuntimeException.
	 * @throws RuntimeException if the pointer was already released
	 */
	public void release() {
		if (sibling instanceof Pointer)
			((Pointer)sibling).release();
		sibling = null;
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
	* Compare the byteCount bytes at the memory location pointed by this pointer to the byteCount bytes at the memory location pointer by other using the C @see <a href="http://www.cplusplus.com/reference/clibrary/cstring/memcmp/">memcmp</a> function.<br>
	 * @return 0 if the two memory blocks are equal, -1 if this pointer's memory is "less" than the other and 1 otherwise.
	 */
	public int compareBytes(Pointer<?> other, long byteCount) {
		return compareBytes(0, other, 0, byteCount);	
	}
	
	/**
	 * Compare the byteCount bytes at the memory location pointed by this pointer shifted by byteOffset to the byteCount bytes at the memory location pointer by other shifted by otherByteOffset using the C @see <a href="http://www.cplusplus.com/reference/clibrary/cstring/memcmp/">memcmp</a> function.<br>
	 * @deprecated Avoid using the byte offset methods variants unless you know what you're doing (may cause alignment issues)
	 * @return 0 if the two memory blocks are equal, -1 if this pointer's memory is "less" than the other and 1 otherwise.
	 */
	@Deprecated
    public int compareBytes(long byteOffset, Pointer<?> other, long otherByteOffset, long byteCount) {
		return JNI.memcmp(getCheckedPeer(byteOffset, byteCount), other.getCheckedPeer(otherByteOffset, byteCount), byteCount);	
	}
	
    /**
	 * Compute a hash code based on pointed address.
	 */
	@Override
    public int hashCode() {
		int hc = new Long(getPeer()).hashCode();
		return hc;
    }
    
    private final long getCheckedPeer(long byteOffset, long validityCheckLength) {
		long offsetPeer = getPeer() + byteOffset;
		///*
		if (validStart != UNKNOWN_VALIDITY) {
			if (offsetPeer < validStart || (offsetPeer + validityCheckLength) > validEnd)
				throw new IndexOutOfBoundsException("Cannot access to memory data of length " + validityCheckLength + " at offset " + (offsetPeer - getPeer()) + " : valid memory start is " + validStart + ", valid memory size is " + (validEnd - validStart));
		}
		//*/
		return offsetPeer;
    }

    /**
	 * Returns a pointer which address value was obtained by this pointer's by adding a byte offset.<br>
	 * The returned pointer will prevent the memory associated to this pointer from being automatically reclaimed as long as it lives, unless Pointer.release() is called on the originally-allocated pointer.
	 * @param byteOffset offset in bytes of the new pointer vs. this pointer. The expression {@code p.offset(byteOffset).getPeer() - p.getPeer() == byteOffset} is always true.
	 */
    public Pointer<T> offset(long byteOffset) {
    	return offset(byteOffset, getIO());
    }

    <U> Pointer<U> offset(long byteOffset, PointerIO<U> pio) {
		if (byteOffset == 0)
			return pio == this.io ? (Pointer<U>)this : as(pio);
		
		long newPeer = getPeer() + byteOffset;
		
		Object newSibling = getSibling() != null ? getSibling() : this;
		if (validStart == UNKNOWN_VALIDITY)
			return newPointer(pio, newPeer, ordered, UNKNOWN_VALIDITY, UNKNOWN_VALIDITY, null, NO_PARENT, null, newSibling);	
		if (newPeer >= validEnd || newPeer < validStart)
			throw new IndexOutOfBoundsException("Invalid pointer offset !");
		
		return newPointer(pio, newPeer, ordered, validStart, validEnd, null, NO_PARENT, null, newSibling);	
	}
	
	/**
	 * Creates a pointer that has the given number of valid bytes ahead.<br>
	 * If the pointer was already bound, the valid bytes must be lower or equal to the current getValidBytes() value.
	 */
	public Pointer<T> validBytes(long byteCount) {
		long peer = getPeer();
		long newValidEnd = peer + byteCount;
		if (validStart == 0 && validEnd == newValidEnd)
			return this;
		
		if (validEnd != UNKNOWN_VALIDITY && newValidEnd > validEnd)
			throw new IndexOutOfBoundsException("Cannot extend validity of pointed memory from " + validEnd + " to " + newValidEnd);
		
		Object newSibling = getSibling() != null ? getSibling() : this;
		return newPointer(getIO(), peer, ordered, peer, newValidEnd, parent, offsetInParent, null, newSibling);    	
	}
	
	/**
	 * Creates a pointer that has the given number of valid elements ahead.<br>
	 * If the pointer was already bound, the valid bytes must be lower or equal to the current getValidElements() value.
	 */
	public Pointer<T> validElements(long elementCount) {
		return validBytes(elementCount * getIO("Cannot define elements validity").getTargetSize());
    }   
	
	/**
	 * Returns a pointer to this pointer.<br>
	 * It will only succeed if this pointer was dereferenced from another pointer.<br>
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
		return parent.offset(offsetInParent).as(io == null ? null : io.getReferenceIO());
	}
	
	/**
	 * Get the address of the memory pointed to by this pointer ("cast this pointer to long", in C jargon).<br>
	 * This is equivalent to the C code {@code (size_t)&pointer}
	 * @return Address of the memory pointed to by this pointer
	 */
	public final long getPeer() {
		return peer;
	}
    
    
    /**
     * Cast this pointer to another pointer type
     * @param newIO
     */
    public <U> Pointer<U> as(PointerIO<U> newIO) {
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
    		return newPointer(newIO, getPeer(), ordered, getValidStart(), getValidEnd(), getParent(), getOffsetInParent(), null, getSibling() != null ? getSibling() : this);
    }

    /**
     * Get the PointerIO instance used by this pointer to get and set pointed values.
     */
    public final PointerIO<T> getIO() {
		return io;
	}
    
	/**
     * Whether this pointer reads data in the system's native byte order or not.
     * See {@link Pointer#order()}, {@link Pointer#order(ByteOrder)}
     */
    final boolean isOrdered() {
    	return ordered;
    }
    
    final long getOffsetInParent() {
		return offsetInParent;
	}
    final Pointer<?> getParent() {
		return parent;
	}
    final Object getSibling() {
		return sibling;
	}
    
    final long getValidEnd() {
		return validEnd;
	}
    final long getValidStart() {
		return validStart;
	}

    /**
     * Cast this pointer to another pointer type<br>
     * Synonym of {@link Pointer#as(Class)}<br>
     * The following C code :<br>
     * <code>{@code 
     * T* pointerT = ...;
     * U* pointerU = (U*)pointerT;
     * }</code><br>
     * Can be translated to the following Java code :<br>
     * <code>{@code 
     * Pointer<T> pointerT = ...;
     * Pointer<U> pointerU = pointerT.as(U.class); // or pointerT.asPointerTo(U.class);
     * }</code><br>
     * @param <U> type of the elements pointed by the returned pointer
     * @param type type of the elements pointed by the returned pointer
     * @return pointer to type U elements at the same address as this pointer
     */
    public <U> Pointer<U> asPointerTo(Type type) {
    	PointerIO<U> pio = PointerIO.getInstance(type);
    	return as(pio);
    }

    /**
     * Cast this pointer to another pointer type.<br>
     * Synonym of {@link Pointer#asPointerTo(Type)}<br>
     * The following C code :<br>
     * <code>{@code 
     * T* pointerT = ...;
     * U* pointerU = (U*)pointerT;
     * }</code><br>
     * Can be translated to the following Java code :<br>
     * <code>{@code 
     * Pointer<T> pointerT = ...;
     * Pointer<U> pointerU = pointerT.as(U.class); // or pointerT.asPointerTo(U.class);
     * }</code><br>
     * @param <U> type of the elements pointed by the returned pointer
     * @param type type of the elements pointed by the returned pointer
     * @return pointer to type U elements at the same address as this pointer
     */
    public <U> Pointer<U> as(Class<U> type) {
    	return asPointerTo(type);
    }
    
    /**
     * Cast this pointer to an untyped pointer.<br>
     * Synonym of {@code ptr.as((Class<?>)null)}.<br>
     * See {@link Pointer#as(Class)}<br>
     * The following C code :<br>
     * <code>{@code 
     * T* pointerT = ...;
     * void* pointer = (void*)pointerT;
     * }</code><br>
     * Can be translated to the following Java code :<br>
     * <code>{@code 
     * Pointer<T> pointerT = ...;
     * Pointer<?> pointer = pointerT.asUntyped(); // or pointerT.as((Class<?>)null);
     * }</code><br>
     * @return untyped pointer pointing to the same address as this pointer
     */
    public Pointer<?> asUntyped() {
    	return as((Class<?>)null);
    }

    /**
     * Get the amount of memory known to be valid from this pointer, or -1 if it is unknown.<br>
     * Memory validity information is available when the pointer was created out of another pointer (with {@link Pointer#offset(long)}, {@link Pointer#next()}, {@link Pointer#next(long)}) or from a direct NIO buffer ({@link Pointer#pointerToBuffer(Buffer)}, {@link Pointer#pointerToInts(IntBuffer)}...)
     * @return amount of bytes that can be safely read or written from this pointer, or -1 if this amount is unknown
     */
    public long getValidBytes() {
    	long ve = getValidEnd();
    	return ve == UNKNOWN_VALIDITY ? -1 : ve - getPeer();
    }
    
    /**
    * Get the amount of memory known to be valid from this pointer (expressed in elements of the target type, see {@link Pointer#getTargetType()}) or -1 if it is unknown.<br>
     * Memory validity information is available when the pointer was created out of another pointer (with {@link Pointer#offset(long)}, {@link Pointer#next()}, {@link Pointer#next(long)}) or from a direct NIO buffer ({@link Pointer#pointerToBuffer(Buffer)}, {@link Pointer#pointerToInts(IntBuffer)}...)
     * @return amount of elements that can be safely read or written from this pointer, or -1 if this amount is unknown
     */
    public long getValidElements() {
    	long bytes = getValidBytes();
    	long elementSize = getTargetSize();
    	if (bytes < 0 || elementSize <= 0)
    		return -1;
    	return bytes / elementSize;
    }
    
    /**
     * Returns an iterator over the elements pointed by this pointer.<br>
     * If this pointer was allocated from Java with the allocateXXX, pointerToXXX methods (or is a view or a clone of such a pointer), the iteration is safely bounded.<br>
     * If this iterator is just a wrapper for a native-allocated pointer (or a view / clone of such a pointer), iteration will go forever (until illegal areas of memory are reached and cause a JVM crash).
     */
    public ListIterator<T> iterator() {
    	return new ListIterator<T>() {
    		Pointer<T> next = Pointer.this.getValidElements() != 0 ? Pointer.this : null;
    		Pointer<T> previous;
    		@Override
			public T next() {
				if (next == null)
					throw new NoSuchElementException();
                T value = next.get();
                previous = next;
                long valid = next.getValidElements();
				next = valid < 0 || valid > 1 ? next.next(1) : null;
				return value;
			}
			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			@Override
			public boolean hasNext() {
				long rem;
				return next != null && ((rem = next.getValidBytes()) < 0 || rem > 0);
			}
			@Override
			public void add(T o) {
				throw new UnsupportedOperationException();
			}
			@Override
			public boolean hasPrevious() {
				return previous != null;
			}
			@Override
			public int nextIndex() {
				throw new UnsupportedOperationException();
			}
			@Override
			public T previous() {
				//TODO return previous;
				throw new UnsupportedOperationException();
			}
			@Override
			public int previousIndex() {
				throw new UnsupportedOperationException();
			}
			@Override
			public void set(T o) {
				if (previous == null)
					throw new NoSuchElementException("You haven't called next() prior to calling ListIterator.set(E)");
				previous.set(o);
			} 
    	};
    }
    
    
    /**
     * Get a pointer to a native object (C++ or ObjectiveC class, struct, union, callback...) 
     */
    public static <N extends NativeObject> Pointer<N> pointerTo(N instance) {
    		return pointerTo(instance, null);
    }
    /**
     * Get a pointer to a native object, specifying the type of the pointer's target.<br>
     * In C++, the address of the pointer to an object as its canonical class is not always the same as the address of the pointer to the same object cast to one of its parent classes. 
     */
    public static <R extends NativeObject> Pointer<R> pointerTo(NativeObject instance, Class<R> targetType) {
		return instance == null ? null : (Pointer<R>)instance.peer;
    }
    /**
    * Get the address of a native object, specifying the type of the pointer's target (same as {@code pointerTo(instance, targetType).getPeer()}, see {@link Pointer#pointerTo(NativeObject, Class)}).<br>
     * In C++, the address of the pointer to an object as its canonical class is not always the same as the address of the pointer to the same object cast to one of its parent classes. 
     */
    public static long getAddress(NativeObject instance, Class targetType) {
		return getPeer(pointerTo(instance, targetType));
    }
    
#docGetOffset("native object", "O extends NativeObject", "Pointer#getNativeObject(Type)")
	@Deprecated
	 public <O extends NativeObject> O getNativeObject(long byteOffset, Type type) {
		return (O)BridJ.createNativeObjectFromPointer((Pointer<O>)this, type);
	}
#docGetOffset("native object", "O extends NativeObject", "Pointer#getNativeObject(Class)")
    @Deprecated
	 public <O extends NativeObject> O getNativeObject(long byteOffset, Class<O> type) {
		return (O)getNativeObject(byteOffset, (Type)type);
	}
#docGet("native object", "O extends NativeObject")
    public <O extends NativeObject> O getNativeObject(Class<O> type) {
		return getNativeObject(0, type);
	}
#docGet("native object", "O extends NativeObject")
    public <O extends NativeObject> O getNativeObject(Type type) {
		O o = (O)getNativeObject(0, type);
		return o;
	}
	
	/**
	 * Check that the pointer's peer is aligned to the target type alignment.
	 * @throws RuntimeException If the target type of this pointer is unknown
	 * @return getPeer() % alignment == 0
	 */
	public boolean isAligned() {
        return isAligned(getIO("Cannot check alignment").getTargetAlignment());
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
	
	/**
	 * Dereference this pointer (*ptr).<br>
     Take the following C++ code fragment :
     <pre>{@code
     int* array = new int[10];
     for (int index = 0; index < 10; index++, array++) 
     	printf("%i\n", *array);
     }</pre>
     Here is its equivalent in Java :
     <pre>{@code
     import static org.bridj.Pointer.*;
     ...
     Pointer<Integer> array = allocateInts(10);
     for (int index = 0; index < 10; index++) { 
     	System.out.println("%i\n".format(array.get()));
     	array = array.next();
	 }
     }</pre>
     Here is a simpler equivalent in Java :
     <pre>{@code
     import static org.bridj.Pointer.*;
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
     Gets the n-th element from this pointer.<br>
     This is equivalent to the C/C++ square bracket syntax.<br>
     Take the following C++ code fragment :
     <pre>{@code
	int* array = new int[10];
	int index = 5;
	int value = array[index];
     }</pre>
     Here is its equivalent in Java :
     <pre>{@code
	import static org.bridj.Pointer.*;
	...
	Pointer<Integer> array = allocateInts(10);
	int index = 5;
	int value = array.get(index);
     }</pre>
     @param index offset in pointed elements at which the value should be copied. Can be negative if the pointer was offset and the memory before it is valid.
     @throws RuntimeException if called on an untyped {@code Pointer<?>} instance ({@link  Pointer#getTargetType()}) 
	 */
	public T get(long index) {
        return getIO("Cannot get pointed value").get(this, index);
    }
    
    /**
	 Assign a value to the pointed memory location.<br>
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
	import static org.bridj.Pointer.*;
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
    public T set(T value) {
        return set(0, value);
    }
    
    static void throwBecauseUntyped(String message) {
    	throw new RuntimeException("Pointer is not typed (call Pointer.asPointerTo(Type) to create a typed pointer) : " + message);
    }
    static void throwUnexpected(Throwable ex) {
    	throw new RuntimeException("Unexpected error", ex);
    }
	/**
     Sets the n-th element from this pointer.<br>
     This is equivalent to the C/C++ square bracket assignment syntax.<br>
     Take the following C++ code fragment :
     <pre>{@code
     float* array = new float[10];
     int index = 5;
     float value = 12;
     array[index] = value;
     }</pre>
     Here is its equivalent in Java :
     <pre>{@code
     import static org.bridj.Pointer.*;
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
	public T set(long index, T value) {
        getIO("Cannot set pointed value").set(this, index, value);
        return value;
    }
	
    /**
     * Get a pointer's peer (see {@link Pointer#getPeer}), or zero if the pointer is null.
     */
	public static long getPeer(Pointer<?> pointer) {
        return pointer == null ? 0 : pointer.getPeer();
    }
	
    /**
     * Get the unitary size of the pointed elements in bytes.
     * @throws RuntimeException if the target type is unknown (see {@link Pointer#getTargetType()})
     */
	public long getTargetSize() {
        return getIO("Cannot compute target size").getTargetSize();
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
        return offset(getIO("Cannot get pointers to next or previous targets").getTargetSize() * delta);
	}
	
	/**
     * Release pointers, if they're not null (see {@link Pointer#release}).
     */
	public static void release(Pointer... pointers) {
    		for (Pointer pointer : pointers)
    			if (pointer != null)
    				pointer.release();
	}

    /**
	 * Test equality of the pointer using the address.<br>
	 * @return true if and only if obj is a Pointer instance and {@code obj.getPeer() == this.getPeer() }
	 */
	@Override
    public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Pointer))
			return false;
		
		Pointer p = (Pointer)obj;
		return getPeer() == p.getPeer();
	}
	
	/**
     * Create a pointer out of a native memory address
     * @param peer native memory address that is to be converted to a pointer
	 * @return a pointer with the provided address : {@code pointer.getPeer() == address }
     */
    @Deprecated
    public static Pointer<?> pointerToAddress(long peer) {
        return newPointer(null, peer, true, UNKNOWN_VALIDITY, UNKNOWN_VALIDITY, null, NO_PARENT, null, null);
    }

    /**
     * Create a pointer out of a native memory address
     * @param size number of bytes known to be readable at the pointed address 
	 * @param peer native memory address that is to be converted to a pointer
	 * @return a pointer with the provided address : {@code pointer.getPeer() == peer }
     */
    @Deprecated
    public static Pointer<?> pointerToAddress(long peer, long size) {
        return newPointer(null, peer, true, peer, peer + size, null, NO_PARENT, null, null);
    }
    
    /**
     * Create a pointer out of a native memory address
     * @param targetClass type of the elements pointed by the resulting pointer 
	 * @param releaser object responsible for reclaiming the native memory once whenever the returned pointer is garbage-collected 
	 * @param peer native memory address that is to be converted to a pointer
	 * @return a pointer with the provided address : {@code pointer.getPeer() == peer }
     */
    public static Pointer<?> pointerToAddress(long peer, Class<?> targetClass, final Releaser releaser) {
        return newPointer(PointerIO.getInstance(targetClass), peer, true, UNKNOWN_VALIDITY, UNKNOWN_VALIDITY, null, -1, null, null);
    }
    /**
     * Create a pointer out of a native memory address
     * @param io PointerIO instance that knows how to read the elements pointed by the resulting pointer 
	 * @param peer native memory address that is to be converted to a pointer
	 * @return a pointer with the provided address : {@code pointer.getPeer() == peer }
     */
    static <P> Pointer<P> pointerToAddress(long peer, PointerIO<P> io) {
    	return newPointer(io, peer, true, UNKNOWN_VALIDITY, UNKNOWN_VALIDITY, null, NO_PARENT, null, null);
	}
	/**
     * Create a pointer out of a native memory address
     * @param io PointerIO instance that knows how to read the elements pointed by the resulting pointer 
	 * @param releaser object responsible for reclaiming the native memory once whenever the returned pointer is garbage-collected 
	 * @param peer native memory address that is to be converted to a pointer
	 * @return a pointer with the provided address : {@code pointer.getPeer() == peer }
     */
    static <P> Pointer<P> pointerToAddress(long peer, PointerIO<P> io, Releaser releaser) {
    	return newPointer(io, peer, true, UNKNOWN_VALIDITY, UNKNOWN_VALIDITY, null, NO_PARENT, releaser, null);
	}
	
	/**
     * Create a pointer out of a native memory address
     * @param releaser object responsible for reclaiming the native memory once whenever the returned pointer is garbage-collected 
	 * @param peer native memory address that is to be converted to a pointer
	 * @return a pointer with the provided address : {@code pointer.getPeer() == peer }
     */
    @Deprecated
    public static Pointer<?> pointerToAddress(long peer, Releaser releaser) {
		return newPointer(null, peer, true, UNKNOWN_VALIDITY, UNKNOWN_VALIDITY, null, NO_PARENT, releaser, null);
	}
    
	/**
     * Create a pointer out of a native memory address
     * @param releaser object responsible for reclaiming the native memory once whenever the returned pointer is garbage-collected 
	 * @param size number of bytes known to be readable at the pointed address 
	 * @param peer native memory address that is to be converted to a pointer
	 * @return a pointer with the provided address : {@code pointer.getPeer() == peer }
     */
    public static Pointer<?> pointerToAddress(long peer, long size, Releaser releaser) {
        return newPointer(null, peer, true, peer, peer + size, null, NO_PARENT, releaser, null);
    }
	
	/**
     * Create a pointer out of a native memory address
     * @param targetClass type of the elements pointed by the resulting pointer 
	 * @param peer native memory address that is to be converted to a pointer
	 * @return a pointer with the provided address : {@code pointer.getPeer() == peer }
     */
    @Deprecated
    public static <P> Pointer<P> pointerToAddress(long peer, Class<P> targetClass) {
    	return newPointer((PointerIO<P>)PointerIO.getInstance(targetClass), peer, true, UNKNOWN_VALIDITY, UNKNOWN_VALIDITY, null, -1, null, null);
    }
    
	/**
     * Create a pointer out of a native memory address
     * @param size number of bytes known to be readable at the pointed address 
	 * @param io PointerIO instance that knows how to read the elements pointed by the resulting pointer 
	 * @param peer native memory address that is to be converted to a pointer
	 * @return a pointer with the provided address : {@code pointer.getPeer() == peer }
     */
    static <U> Pointer<U> pointerToAddress(long peer, long size, PointerIO<U> io) {
    	return newPointer(io, peer, true, peer, peer + size, null, NO_PARENT, null, null);
	}
	
	/**
     * Create a pointer out of a native memory address
     * @param releaser object responsible for reclaiming the native memory once whenever the returned pointer is garbage-collected 
	 * @param peer native memory address that is to be converted to a pointer
	 * @return a pointer with the provided address : {@code pointer.getPeer() == peer }
     */
    static <U> Pointer<U> newPointer(
		PointerIO<U> io, 
		long peer, 
		boolean ordered, 
		long validStart, 
		long validEnd, 
		Pointer<?> parent, 
		long offsetInParent, 
		final Releaser releaser,
		Object sibling)
	{
		if (peer == 0)
			return null;
		
		if (validEnd != UNKNOWN_VALIDITY) {
			long size = validEnd - validStart;
			if (size <= 0)
				return null;
		}
		
		if (releaser == null)
			return new Pointer<U>(io, peer, ordered, validStart, validEnd, parent, offsetInParent, sibling);
		else {
			assert sibling == null;
			return new Pointer<U>(io, peer, ordered, validStart, validEnd, parent, offsetInParent, sibling) {
				private Releaser rel = releaser;
				@Override
				public synchronized void release() {
					if (rel != null) {
						rel.release(this);
						rel = null;
					}
				}
				protected void finalize() {
					release();
				}
			};
		}
    }
	
#docAllocate("typed pointer", "P extends TypedPointer")
    public static <P extends TypedPointer> Pointer<P> allocateTypedPointer(Class<P> type) {
    	return (Pointer<P>)(Pointer)allocate(PointerIO.getInstance(type));
    }
#docAllocateArray("typed pointer", "P extends TypedPointer")
    public static <P extends TypedPointer> Pointer<P> allocateTypedPointers(Class<P> type, long arrayLength) {
    	return (Pointer<P>)(Pointer)allocateArray(PointerIO.getInstance(type), arrayLength);
    }
    /**
     * Create a memory area large enough to hold a pointer.
     * @param targetType target type of the pointer values to be stored in the allocated memory 
     * @return a pointer to a new memory area large enough to hold a single typed pointer
     */
    public static <P> Pointer<Pointer<P>> allocatePointer(Class<P> targetType) {
    	return (Pointer<Pointer<P>>)(Pointer)allocate(PointerIO.getPointerInstance(targetType)); 
    }
    /**
     * Create a memory area large enough to hold a pointer.
     * @param targetType target type of the pointer values to be stored in the allocated memory 
     * @return a pointer to a new memory area large enough to hold a single typed pointer
     */
    public static <P> Pointer<Pointer<P>> allocatePointer(Type targetType) {
    	return (Pointer<Pointer<P>>)(Pointer)allocate(PointerIO.getPointerInstance(targetType)); 
    }
    /**
     * Create a memory area large enough to hold a pointer to a pointer
     * @param targetType target type of the values pointed by the pointer values to be stored in the allocated memory 
     * @return a pointer to a new memory area large enough to hold a single typed pointer
     */
    public static <P> Pointer<Pointer<Pointer<P>>> allocatePointerPointer(Type targetType) {
    	return allocatePointer(pointerType(targetType)); 
    }/**
     * Create a memory area large enough to hold a pointer to a pointer
     * @param targetType target type of the values pointed by the pointer values to be stored in the allocated memory 
     * @return a pointer to a new memory area large enough to hold a single typed pointer
     */
    public static <P> Pointer<Pointer<Pointer<P>>> allocatePointerPointer(Class<P> targetType) {
    	return allocatePointerPointer(targetType); 
    }
#docAllocate("untyped pointer", "Pointer<?>")
    /**
     * Create a memory area large enough to hold an untyped pointer.
     * @return a pointer to a new memory area large enough to hold a single untyped pointer
     */
    public static <V> Pointer<Pointer<?>> allocatePointer() {
    	return (Pointer)allocate(PointerIO.getPointerInstance());
    }
#docAllocateArray("untyped pointer", "Pointer<?>")
    public static Pointer<Pointer<?>> allocatePointers(int arrayLength) {
		return (Pointer<Pointer<?>>)(Pointer)allocateArray(PointerIO.getPointerInstance(), arrayLength); 
	}
	
    /**
     * Create a memory area large enough to hold an array of arrayLength typed pointers.
     * @param targetType target type of element pointers in the resulting pointer array. 
     * @param arrayLength size of the allocated array, in elements
     * @return a pointer to a new memory area large enough to hold an array of arrayLength typed pointers
     */
    public static <P> Pointer<Pointer<P>> allocatePointers(Class<P> targetType, int arrayLength) {
		return (Pointer<Pointer<P>>)(Pointer)allocateArray(PointerIO.getPointerInstance(targetType), arrayLength); // TODO 
	}
	
    /**
     * Create a memory area large enough to hold an array of arrayLength typed pointers.
     * @param targetType target type of element pointers in the resulting pointer array. 
     * @param arrayLength size of the allocated array, in elements
     * @return a pointer to a new memory area large enough to hold an array of arrayLength typed pointers
     */
    public static <P> Pointer<Pointer<P>> allocatePointers(Type targetType, int arrayLength) {
		return (Pointer<Pointer<P>>)(Pointer)allocateArray(PointerIO.getPointerInstance(targetType), arrayLength); // TODO 
	}
	
    
    /**
     * Create a memory area large enough to a single items of type elementClass.
     * @param elementClass type of the array elements
     * @return a pointer to a new memory area large enough to hold a single item of type elementClass.
     */
    public static <V> Pointer<V> allocate(Class<V> elementClass) {
        return allocateArray(elementClass, 1);
    }
    
    /**
     * Create a memory area large enough to hold one item of the type associated to the provided PointerIO instance (see {@link PointerIO#getTargetType()})
     * @param io PointerIO instance able to store and retrieve the element
     * @return a pointer to a new memory area large enough to hold one item of the type associated to the provided PointerIO instance (see {@link PointerIO#getTargetType()})
     */
    public static <V> Pointer<V> allocate(PointerIO<V> io) {
    	long targetSize = io.getTargetSize();
    	if (targetSize < 0)
    		throwBecauseUntyped("Cannot allocate array ");
		return allocateBytes(io, targetSize, null);
    }
    /**
     * Create a memory area large enough to hold arrayLength items of the type associated to the provided PointerIO instance (see {@link PointerIO#getTargetType()})
     * @param io PointerIO instance able to store and retrieve elements of the array
     * @param arrayLength length of the array in elements
     * @return a pointer to a new memory area large enough to hold arrayLength items of the type associated to the provided PointerIO instance (see {@link PointerIO#getTargetType()})
     */
    public static <V> Pointer<V> allocateArray(PointerIO<V> io, long arrayLength) {
		long targetSize = io.getTargetSize();
    	if (targetSize < 0)
    		throwBecauseUntyped("Cannot allocate array ");
		return allocateBytes(io, targetSize * arrayLength, null);
    }
    /**
     * Create a memory area large enough to hold arrayLength items of the type associated to the provided PointerIO instance (see {@link PointerIO#getTargetType()})
     * @param io PointerIO instance able to store and retrieve elements of the array
     * @param arrayLength length of the array in elements
     * @param beforeDeallocation fake releaser that should be run just before the memory is actually released, for instance in order to call some object destructor
     * @return a pointer to a new memory area large enough to hold arrayLength items of the type associated to the provided PointerIO instance (see {@link PointerIO#getTargetType()})
     */
    public static <V> Pointer<V> allocateArray(PointerIO<V> io, long arrayLength, final Releaser beforeDeallocation) {
		long targetSize = io.getTargetSize();
    	if (targetSize < 0)
    		throwBecauseUntyped("Cannot allocate array ");
		return allocateBytes(io, targetSize * arrayLength, beforeDeallocation);
    }
    /**
     * Create a memory area large enough to hold byteSize consecutive bytes and return a pointer to elements of the type associated to the provided PointerIO instance (see {@link PointerIO#getTargetType()})
     * @param io PointerIO instance able to store and retrieve elements of the array
     * @param byteSize length of the array in bytes
     * @param beforeDeallocation fake releaser that should be run just before the memory is actually released, for instance in order to call some object destructor
     * @return a pointer to a new memory area large enough to hold byteSize consecutive bytes
     */
    public static <V> Pointer<V> allocateBytes(PointerIO<V> io, long byteSize, final Releaser beforeDeallocation) {
        if (byteSize == 0)
        	return null;
        if (byteSize < 0)
        	throw new IllegalArgumentException("Cannot allocate a negative amount of memory !");
        
        long address = JNI.mallocNulled(byteSize);
        if (address == 0)
        	throw new RuntimeException("Failed to allocate " + byteSize);

		return newPointer(io, address, true, address, address + byteSize, null, NO_PARENT, beforeDeallocation == null ? freeReleaser : new Releaser() {
        	@Override
        	public void release(Pointer<?> p) {
        		beforeDeallocation.release(p);
        		freeReleaser.release(p);
        	}
        }, null);
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
    public static <V> Pointer<V> allocateArray(Class<V> elementClass, long arrayLength) {
		if (arrayLength == 0)
			return null;
		
		PointerIO pio = PointerIO.getInstance(elementClass);
		if (pio == null)
			throw new UnsupportedOperationException("Cannot allocate memory for type " + elementClass.getName());
		return (Pointer<V>)allocateArray(pio, arrayLength);
		/*
        #foreach ($prim in $primitives)
        if (elementClass == ${prim.WrapperName}.TYPE || elementClass == ${prim.WrapperName}.class)
            return (Pointer<V>)allocateArray(PointerIO.get${prim.CapName}Instance(), arrayLength);
        #end
        if (Pointer.class.isAssignableFrom(elementClass))
            return (Pointer<V>)allocateArray(PointerIO.getPointerInstance(elementClass), arrayLength); // TODO
        if (SizeT.class.isAssignableFrom(elementClass))
            return (Pointer<V>)allocateArray(PointerIO.getSizeTInstance(), arrayLength); // TODO
        if (CLong.class.isAssignableFrom(elementClass))
            return (Pointer<V>)allocateArray(PointerIO.getCLongInstance(), arrayLength); // TODO
        if (StructObject.class.isAssignableFrom(elementClass)) {
        	CRuntime runtime = (CRuntime)BridJ.getRuntime(elementClass);
        	StructIO sio = StructIO.getInstance(elementClass, elementClass);
        	PointerIO pio = PointerIO.getInstance(sio);
        	return (Pointer<V>)allocateArray(pio, arrayLength); // TODO
        }
        //if (CLong.class.isAssignableFrom(elementClass))
        //    return (Pointer<V>)allocate(PointerIO.getPointerInstance(), Pointer.SIZE * arrayLength); // TODO
        throw new UnsupportedOperationException("Cannot allocate memory for type " + elementClass.getName());*/
    }

    /**
     * Create a pointer to the memory location used by a direct NIO buffer.<br>
     * If the NIO buffer is not direct, then it's backing Java array is copied to some native memory and will never be updated by changes to the native memory (calls {@link Pointer#pointerToArray(Object)}).<br>
     * The returned pointer (and its subsequent clones returned by {@link Pointer#clone()}, {@link Pointer#offset(long)} or {@link Pointer#next(long)}) retains a reference to the original NIO buffer, so its lifespan is at least that of the pointer.</br>
     * @throws UnsupportedOperationException if the buffer is not direct
     */
    public static Pointer<?> pointerToBuffer(Buffer buffer) {
        if (buffer == null)
			return null;
		
		#foreach ($prim in $primitivesNoBool)
		if (buffer instanceof ${prim.BufferName})
			return (Pointer)pointerTo${prim.CapName}s((${prim.BufferName})buffer);
		#end
        throw new UnsupportedOperationException("Unhandled buffer type : " + buffer.getClass().getName());
	}
	
	/**
	 * When a pointer was created with {@link Pointer#pointerToBuffer(Buffer)} on a non-direct buffer, a native copy of the buffer data was made.
	 * This method updates the original buffer with the native memory, and does nothing if the buffer is direct <b>and</b> points to the same memory location as this pointer.<br>
	 * @throws IllegalArgumentException if buffer is direct and does not point to the exact same location as this Pointer instance
     */
    public void updateBuffer(Buffer buffer) {
        if (buffer == null)
			throw new IllegalArgumentException("Cannot update a null Buffer !");
		
		if (buffer.isDirect()) {
			long address = JNI.getDirectBufferAddress(buffer);
			if (address != getPeer()) {
				throw new IllegalArgumentException("Direct buffer does not point to the same location as this Pointer instance, updating it makes no sense !");
			}
		} else {
			#foreach ($prim in $primitivesNoBool)
			if (buffer instanceof ${prim.BufferName}) {
				((${prim.BufferName})buffer).duplicate().put(get${prim.BufferName}());
				return;
			}
			#end
			throw new UnsupportedOperationException("Unhandled buffer type : " + buffer.getClass().getName());
		}
	}

#foreach ($prim in $primitives)
    #docAllocateCopy($prim.Name $prim.WrapperName)
    public static Pointer<${prim.WrapperName}> pointerTo${prim.CapName}(${prim.Name} value) {
        Pointer<${prim.WrapperName}> mem = allocate(PointerIO.get${prim.CapName}Instance());
        mem.set${prim.CapName}(0, value);
        return mem;
    }
	
#docAllocateArrayCopy($prim.Name $prim.WrapperName)
    public static Pointer<${prim.WrapperName}> pointerTo${prim.CapName}s(${prim.Name}... values) {
        if (values == null)
			return null;
		Pointer<${prim.WrapperName}> mem = allocateArray(PointerIO.get${prim.CapName}Instance(), values.length);
        mem.set${prim.CapName}s(0, values, 0, values.length);
        return mem;
    }
    
    #docAllocateArray2DCopy($prim.Name $prim.WrapperName)
    public static Pointer<Pointer<${prim.WrapperName}>> pointerTo${prim.CapName}s(${prim.Name}[][] values) {
        if (values == null)
			return null;
		int dim1 = values.length, dim2 = values[0].length;
		Pointer<Pointer<${prim.WrapperName}>> mem = allocate${prim.CapName}s(dim1, dim2);
		for (int i1 = 0; i1 < dim1; i1++)
        	mem.set${prim.CapName}s(i1 * dim2 * ${prim.Size}, values[i1], 0, dim2);
		return mem;
    }
    
    #docAllocateArray3DCopy($prim.Name $prim.WrapperName)
    public static Pointer<Pointer<Pointer<${prim.WrapperName}>>> pointerTo${prim.CapName}s(${prim.Name}[][][] values) {
        if (values == null)
			return null;
		int dim1 = values.length, dim2 = values[0].length, dim3 = values[0][0].length;
		Pointer<Pointer<Pointer<${prim.WrapperName}>>> mem = allocate${prim.CapName}s(dim1, dim2, dim3);
		for (int i1 = 0; i1 < dim1; i1++) {
        	int offset1 = i1 * dim2;
        	for (int i2 = 0; i2 < dim2; i2++) {
        		int offset2 = (offset1 + i2) * dim3;
				mem.set${prim.CapName}s(offset2 * ${prim.Size}, values[i1][i2], 0, dim3);
			}
		}
		return mem;
    }
	
    #docAllocate($prim.Name $prim.WrapperName)
    public static Pointer<${prim.WrapperName}> allocate${prim.CapName}() {
        return allocate(PointerIO.get${prim.CapName}Instance());
    }
    #docAllocateArray($prim.Name $prim.WrapperName)
    public static Pointer<${prim.WrapperName}> allocate${prim.CapName}s(long arrayLength) {
        return allocateArray(PointerIO.get${prim.CapName}Instance(), arrayLength);
    }
    
    #docAllocateArray2D($prim.Name $prim.WrapperName)
    public static Pointer<Pointer<${prim.WrapperName}>> allocate${prim.CapName}s(long dim1, long dim2) {
        return allocateArray(PointerIO.getArrayInstance(PointerIO.get${prim.CapName}Instance(), new long[] { dim1, dim2 }, 0), dim1);
        
    }
    #docAllocateArray3D($prim.Name $prim.WrapperName)
    public static Pointer<Pointer<Pointer<${prim.WrapperName}>>> allocate${prim.CapName}s(long dim1, long dim2, long dim3) {
        long[] dims = new long[] { dim1, dim2, dim3 };
		return
			allocateArray(
				PointerIO.getArrayInstance(
					PointerIO.getArrayInstance(
						PointerIO.get${prim.CapName}Instance(), 
						dims,
						1
					),
					dims,
					0
				),
				dim1
			)
		;
    }

#end
#foreach ($prim in $primitivesNoBool)

	/**
     * Create a pointer to the memory location used by a direct NIO ${prim.BufferName}}.<br>
     * If the NIO ${prim.BufferName}} is not direct, then it's backing Java array is copied to some native memory and will never be updated by changes to the native memory (calls {@link Pointer#pointerTo${prim.CapName}s(${prim.Name}[])}).<br>
     * The returned pointer (and its subsequent clones returned by {@link Pointer#clone()}, {@link Pointer#offset(long)} or {@link Pointer#next(long)}) retains a reference to the original NIO buffer, so its lifespan is at least that of the pointer.</br>
     * @throws UnsupportedOperationException if the buffer is not direct
     */
    public static Pointer<${prim.WrapperName}> pointerTo${prim.CapName}s(${prim.BufferName} buffer) {
        if (buffer == null)
			return null;
		
		if (!buffer.isDirect()) {
			return pointerTo${prim.CapName}s(buffer.array());
			//throw new UnsupportedOperationException("Cannot create pointers to indirect ${prim.BufferName} buffers");
		}
		
		long address = JNI.getDirectBufferAddress(buffer);
		long size = JNI.getDirectBufferCapacity(buffer);
		
		// HACK (TODO?) the JNI spec says size is in bytes, but in practice on mac os x it's in elements !!!
		size *= ${prim.Size};
		//System.out.println("Buffer capacity = " + size);
		
		if (address == 0 || size == 0)
			return null;
		
		PointerIO<${prim.WrapperName}> io = CommonPointerIOs.${prim.Name}IO;
		boolean ordered = buffer.order().equals(ByteOrder.nativeOrder());
		return newPointer(io, address, ordered, address, address + size, null, NO_PARENT, null, buffer);
    }
	
#end
    
    /**
     * Get the type of pointed elements.
     */
	public Type getTargetType() {
        PointerIO<T> io = getIO();
        return io == null ? null : io.getTargetType();
    }
    
    /**
	 * Read an untyped pointer value from the pointed memory location
	 * @deprecated Avoid using untyped pointers, if possible.
	 */
	@Deprecated
    public Pointer<?> getPointer() {
    	return getPointer(0, (PointerIO)null);	
    }
    
    /**
	 * Read a pointer value from the pointed memory location shifted by a byte offset
	 */
    @Deprecated
	public Pointer<?> getPointer(long byteOffset) {
        return getPointer(byteOffset, (PointerIO)null);
    }
    
    /**
	 * Read a pointer value from the pointed memory location.<br>
	 * @param c class of the elements pointed by the resulting pointer 
	 */
    public <U> Pointer<U> getPointer(Class<U> c) {
    	return getPointer(0, (PointerIO<U>)PointerIO.getInstance(c));	
    }
    
    /**
	 * Read a pointer value from the pointed memory location
	 * @param pio PointerIO instance that knows how to read the elements pointed by the resulting pointer 
	 */
    public <U> Pointer<U> getPointer(PointerIO<U> pio) {
    	return getPointer(0, pio);	
    }
    
    /**
	 * Read a pointer value from the pointed memory location shifted by a byte offset
	 * @param c class of the elements pointed by the resulting pointer 
	 */
    @Deprecated
	public <U> Pointer<U> getPointer(long byteOffset, Class<U> c) {
    	return getPointer(byteOffset, (PointerIO<U>)PointerIO.getInstance(c));	
    }
    
    /**
	 * Read a pointer value from the pointed memory location shifted by a byte offset
	 * @param t type of the elements pointed by the resulting pointer 
	 */
    @Deprecated
	public <U> Pointer<U> getPointer(long byteOffset, Type t) {
        return getPointer(byteOffset, t == null ? null : (PointerIO<U>)PointerIO.getInstance(t));
    }
    
    /**
	 * Read a pointer value from the pointed memory location shifted by a byte offset
	 * @param pio PointerIO instance that knows how to read the elements pointed by the resulting pointer 
	 */
    @Deprecated
	public <U> Pointer<U> getPointer(long byteOffset, PointerIO<U> pio) {
    	long value = getSizeT(byteOffset);
    	if (value == 0)
    		return null;
    	return newPointer(pio, value, isOrdered(), UNKNOWN_VALIDITY, UNKNOWN_VALIDITY, this, byteOffset, null, null);
    }

    /**
     * Write a pointer value to the pointed memory location
     */
    public Pointer<T> setPointer(Pointer<?> value) {
    	return setPointer(0, value);
    }
    
    /**
     * Write a pointer value to the pointed memory location shifted by a byte offset
     */
    @Deprecated
	public Pointer<T> setPointer(long byteOffset, Pointer<?> value) {
        setSizeT(byteOffset, value == null ? 0 : value.getPeer());
        return this;
    }
    
    /**
	 * Read an array of untyped pointer values from the pointed memory location shifted by a byte offset
	 * @deprecated Use a typed version instead : {@link Pointer#getPointers(long, int, Type)}, {@link Pointer#getPointers(long, int, Class)} or {@link Pointer#getPointers(long, int, PointerIO)}
	 */
    @Deprecated
	public Pointer<?>[] getPointers(long byteOffset, int arrayLength) {
        return getPointers(byteOffset, arrayLength, (PointerIO)null);
    }
    /**
	 * Read the array of remaining untyped pointer values from the pointed memory location
	 * @deprecated Use a typed version instead : {@link Pointer#getPointers(long, int, Type)}, {@link Pointer#getPointers(long, int, Class)} or {@link Pointer#getPointers(long, int, PointerIO)}
	 */
    @Deprecated
	public Pointer<?>[] getPointers() {
        long rem = getValidElements("Cannot create array if remaining length is not known. Please use getPointers(int length) instead.");
		return getPointers(0L, (int)rem);
    }
    /**
	 * Read an array of untyped pointer values from the pointed memory location
	 * @deprecated Use a typed version instead : {@link Pointer#getPointers(long, int, Type)}, {@link Pointer#getPointers(long, int, Class)} or {@link Pointer#getPointers(long, int, PointerIO)}
	 */
    @Deprecated
	public Pointer<?>[] getPointers(int arrayLength) {
        return getPointers(0, arrayLength);
    }
    /**
	 * Read an array of pointer values from the pointed memory location shifted by a byte offset
	 * @param t type of the elements pointed by the resulting pointer 
	 */
    @Deprecated
	public <U> Pointer<U>[] getPointers(long byteOffset, int arrayLength, Type t) {
        return getPointers(byteOffset, arrayLength, t == null ? null : (PointerIO<U>)PointerIO.getInstance(t));
    }
    /**
	 * Read an array of pointer values from the pointed memory location shifted by a byte offset
	 * @param t class of the elements pointed by the resulting pointer 
	 */
    @Deprecated
	public <U> Pointer<U>[] getPointers(long byteOffset, int arrayLength, Class<U> t) {
        return getPointers(byteOffset, arrayLength, t == null ? null : PointerIO.getInstance(t));
    }
    
    /**
	 * Read an array of pointer values from the pointed memory location shifted by a byte offset
	 * @param pio PointerIO instance that knows how to read the elements pointed by the resulting pointer 
	 */
    @Deprecated
	public <U> Pointer<U>[] getPointers(long byteOffset, int arrayLength, PointerIO pio) {
    	Pointer<U>[] values = (Pointer<U>[])new Pointer[arrayLength];
		int s = JNI.POINTER_SIZE;
		for (int i = 0; i < arrayLength; i++)
			values[i] = getPointer(byteOffset + i * s, pio);
		return values;
	}
	/**
	 * Write an array of pointer values to the pointed memory location shifted by a byte offset
	 */
    @Deprecated
	public Pointer<T> setPointers(long byteOffset, Pointer<?>[] values) {
    		return setPointers(byteOffset, values, 0, values.length);
	}
	
	/**
	 * Write length pointer values from the given array (starting at the given value offset) to the pointed memory location shifted by a byte offset
	 */
    @Deprecated
	public Pointer<T> setPointers(long byteOffset, Pointer<?>[] values, int valuesOffset, int length) {
		if (values == null)
			throw new IllegalArgumentException("Null values");
		int n = length, s = JNI.POINTER_SIZE;
		for (int i = 0; i < n; i++)
			setPointer(byteOffset + i * s, values[valuesOffset + i]);
		return this;
	}
	
	/**
	 * Write an array of pointer values to the pointed memory location
	 */
    public Pointer<T> setPointers(Pointer<?>[] values) {
    		return setPointers(0, values);
	}
	
	/**
	 * Read an array of elements from the pointed memory location shifted by a byte offset.<br>
	 * For pointers to primitive types (e.g. {@code Pointer<Integer> }), this method returns primitive arrays (e.g. {@code int[] }), unlike {@link Pointer#toArray } (which returns arrays of objects so primitives end up being boxed, e.g. {@code Integer[] })
	 * @return an array of values of the requested length. The array is an array of primitives if the pointer's target type is a primitive or a boxed primitive type
	 */
	@Deprecated
	public Object getArray(long byteOffset, int length) {
        return getIO("Cannot create sublist").getArray(this, byteOffset, length);	
	}
	
	/**
	 * Read an array of elements from the pointed memory location.<br>
	 * For pointers to primitive types (e.g. {@code Pointer<Integer> }), this method returns primitive arrays (e.g. {@code int[] }), unlike {@link Pointer#toArray } (which returns arrays of objects so primitives end up being boxed, e.g. {@code Integer[] })
	 * @return an array of values of the requested length. The array is an array of primitives if the pointer's target type is a primitive or a boxed primitive type
	 */
	public Object getArray(int length) {
		return getArray(0L, length);	
	}
	
	/**
	 * Read the array of remaining elements from the pointed memory location.<br>
	 * For pointers to primitive types (e.g. {@code Pointer<Integer> }), this method returns primitive arrays (e.g. {@code int[] }), unlike {@link Pointer#toArray } (which returns arrays of objects so primitives end up being boxed, e.g. {@code Integer[] })
	 * @return an array of values of the requested length. The array is an array of primitives if the pointer's target type is a primitive or a boxed primitive type
	 */
	public Object getArray() {
		return getArray((int)getValidElements());	
	}
	
	/**
	 * Read an NIO {@link Buffer} of elements from the pointed memory location shifted by a byte offset.<br>
	 * @return an NIO {@link Buffer} of values of the requested length.
	 * @throws UnsupportedOperationException if this pointer's target type is not a Java primitive type with a corresponding NIO {@link Buffer} class.
	 */
	@Deprecated
	public <B extends Buffer> B getBuffer(long byteOffset, int length) {
        return (B)getIO("Cannot create Buffer").getBuffer(this, byteOffset, length);	
	}
	
	/**
	 * Read an NIO {@link Buffer} of elements from the pointed memory location.<br>
	 * @return an NIO {@link Buffer} of values of the requested length.
	 * @throws UnsupportedOperationException if this pointer's target type is not a Java primitive type with a corresponding NIO {@link Buffer} class.
	 */
	public <B extends Buffer> B getBuffer(int length) {
		return (B)getBuffer(0L, length);	
	}
	
	/**
	 * Read the NIO {@link Buffer} of remaining elements from the pointed memory location.<br>
	 * @return an array of values of the requested length.
	 * @throws UnsupportedOperationException if this pointer's target type is not a Java primitive type with a corresponding NIO {@link Buffer} class.
	 */
	public <B extends Buffer> B getBuffer() {
		return (B)getBuffer((int)getValidElements());	
	}
	
	/**
	 * Write an array of elements to the pointed memory location shifted by a byte offset.<br>
	 * For pointers to primitive types (e.g. {@code Pointer<Integer> }), this method accepts primitive arrays (e.g. {@code int[] }) instead of arrays of boxed primitives (e.g. {@code Integer[] })
	 */
	@Deprecated
	public Pointer<T> setArray(long byteOffset, Object array) {
        getIO("Cannot create sublist").setArray(this, byteOffset, array);
        return this;
	}
	
	/**
     * Allocate enough memory for array.length values, copy the values of the array provided as argument into it and return a pointer to that memory.<br>
     * The memory will be automatically be freed when the pointer is garbage-collected or upon manual calls to {@link Pointer#release()}.<br>
     * The pointer won't be garbage-collected until all its clones / views are garbage-collected themselves (see {@link Pointer#clone()}, {@link Pointer#offset(long)}, {@link Pointer#next(long)}, {@link Pointer#next()}).<br>
     * For pointers to primitive types (e.g. {@code Pointer<Integer> }), this method accepts primitive arrays (e.g. {@code int[] }) instead of arrays of boxed primitives (e.g. {@code Integer[] })
	 * @param array primitive array containing the initial values for the created memory area
     * @return pointer to a new memory location that initially contains the consecutive values provided in argument
     */
	public static <T> Pointer<T> pointerToArray(Object array) {
		if (array == null)
			return null;
		
		PointerIO<T> io = PointerIO.getArrayIO(array);
		if (io == null)
            throwBecauseUntyped("Cannot create pointer to array");
        
        Pointer<T> ptr = allocateArray(io, java.lang.reflect.Array.getLength(array));
        io.setArray(ptr, 0, array);
        return ptr;
	}
	
	/**
	 * Write an array of elements to the pointed memory location.<br>
	 * For pointers to primitive types (e.g. {@code Pointer<Integer> }), this method accepts primitive arrays (e.g. {@code int[] }) instead of arrays of boxed primitives (e.g. {@code Integer[] })
	 */
	public Pointer<T> setArray(Object array) {
		return setArray(0L, array);
	}
	
	#foreach ($sizePrim in ["SizeT", "CLong"])
	
#docAllocateCopy($sizePrim $sizePrim)
    public static Pointer<${sizePrim}> pointerTo${sizePrim}(long value) {
		Pointer<${sizePrim}> p = allocate(PointerIO.get${sizePrim}Instance());
		p.set${sizePrim}(0, value);
		return p;
	}
#docAllocateCopy($sizePrim $sizePrim)
    public static Pointer<${sizePrim}> pointerTo${sizePrim}(${sizePrim} value) {
		Pointer<${sizePrim}> p = allocate(PointerIO.get${sizePrim}Instance());
		p.set${sizePrim}(0, value);
		return p;
	}
#docAllocateArrayCopy($sizePrim $sizePrim)
    public static Pointer<${sizePrim}> pointerTo${sizePrim}s(long... values) {
		if (values == null)
			return null;
		return allocateArray(PointerIO.get${sizePrim}Instance(), values.length).set${sizePrim}s(0, values);
	}
#docAllocateArrayCopy($sizePrim $sizePrim)
    public static Pointer<${sizePrim}> pointerTo${sizePrim}s(${sizePrim}... values) {
		if (values == null)
			return null;
		return allocateArray(PointerIO.get${sizePrim}Instance(), values.length).set${sizePrim}s(0, values);
	}
	
#docAllocateArrayCopy($sizePrim $sizePrim)
    public static Pointer<${sizePrim}> pointerTo${sizePrim}s(int[] values) {
		if (values == null)
			return null;
		return allocateArray(PointerIO.get${sizePrim}Instance(), values.length).set${sizePrim}s(0, values);
	}
	
#docAllocateArray($sizePrim $sizePrim)
    public static Pointer<${sizePrim}> allocate${sizePrim}s(long arrayLength) {
		return allocateArray(PointerIO.get${sizePrim}Instance(), arrayLength);
	}
#docAllocate($sizePrim $sizePrim)
    public static Pointer<${sizePrim}> allocate${sizePrim}() {
		return allocate(PointerIO.get${sizePrim}Instance());
	}
	
#docGet($sizePrim $sizePrim)
    public long get${sizePrim}() {
		return get${sizePrim}(0);
	}
#docGetOffset($sizePrim $sizePrim "Pointer#get${sizePrim}()")
    @Deprecated
	public long get${sizePrim}(long byteOffset) {
		return ${sizePrim}.SIZE == 8 ? 
			getLong(byteOffset) : 
			//0xffffffffL & 
			getInt(byteOffset);
	}
#docGetRemainingArray($sizePrim $sizePrim)
    public long[] get${sizePrim}s() {
    		long rem = getValidElements("Cannot create array if remaining length is not known. Please use get${sizePrim}s(int length) instead.");
		return get${sizePrim}s(0, (int)rem);
	}
#docGetArray($sizePrim $sizePrim)
    public long[] get${sizePrim}s(int arrayLength) {
		return get${sizePrim}s(0, arrayLength);
	}
#docGetArrayOffset($sizePrim $sizePrim "Pointer#get${sizePrim}s(int)")
    @Deprecated
	public long[] get${sizePrim}s(long byteOffset, int arrayLength) {
		if (${sizePrim}.SIZE == 8)  
			return getLongs(byteOffset, arrayLength);
		
		int[] values = getInts(byteOffset, arrayLength);
		long[] ret = new long[arrayLength];
		for (int i = 0; i < arrayLength; i++) {
			ret[i] = //0xffffffffL & 
				values[i];
		}
		return ret;
	}
	
#docSet($sizePrim $sizePrim)
    public Pointer<T> set${sizePrim}(long value) {
		return set${sizePrim}(0, value);
	}
#docSet($sizePrim $sizePrim)
    public Pointer<T> set${sizePrim}(${sizePrim} value) {
		return set${sizePrim}(0, value);
	}
#docSetOffset($sizePrim $sizePrim "Pointer#set${sizePrim}(long)")
    @Deprecated
	public Pointer<T> set${sizePrim}(long byteOffset, long value) {
		if (${sizePrim}.SIZE == 8)
			setLong(byteOffset, value);
		else {
			setInt(byteOffset, SizeT.safeIntCast(value));
		}
		return this;
	}
	
#docSetOffset($sizePrim $sizePrim "Pointer#set${sizePrim}(${sizePrim})")
    @Deprecated
	public Pointer<T> set${sizePrim}(long byteOffset, ${sizePrim} value) {
		return set${sizePrim}(byteOffset, value.longValue());
	}
#docSetArray($sizePrim $sizePrim)
    public Pointer<T> set${sizePrim}s(long[] values) {
		return set${sizePrim}s(0, values);
	}
#docSetArray($sizePrim $sizePrim)
    public Pointer<T> set${sizePrim}s(int[] values) {
		return set${sizePrim}s(0, values);
	}
#docSetArray($sizePrim $sizePrim)
    public Pointer<T> set${sizePrim}s(${sizePrim}[] values) {
		return set${sizePrim}s(0, values);
	}
#docSetArrayOffset($sizePrim $sizePrim "Pointer#set${sizePrim}s(long[])")
    @Deprecated
	public Pointer<T> set${sizePrim}s(long byteOffset, long[] values) {
    		return set${sizePrim}s(byteOffset, values, 0, values.length);
	}
#docSetArrayOffset($sizePrim $sizePrim)
    public Pointer<T> set${sizePrim}s(long byteOffset, long[] values, int valuesOffset, int length) {
		if (values == null)
			throw new IllegalArgumentException("Null values");
		if (${sizePrim}.SIZE == 8) {
			setLongs(byteOffset, values, valuesOffset, length);
		} else {
			int n = length, s = 4;
			for (int i = 0; i < n; i++)
				setInt(byteOffset + i * s, (int)values[valuesOffset + i]);
		}
		return this;
	}
#docSetArrayOffset($sizePrim $sizePrim "Pointer#set${sizePrim}s(${sizePrim}...)")
    @Deprecated
	public Pointer<T> set${sizePrim}s(long byteOffset, ${sizePrim}... values) {
		if (values == null)
			throw new IllegalArgumentException("Null values");
		int n = values.length, s = ${sizePrim}.SIZE;
		for (int i = 0; i < n; i++)
			set${sizePrim}(byteOffset + i * s, values[i].longValue());
		return this;
	}
#docSetArrayOffset($sizePrim $sizePrim "Pointer#set${sizePrim}s(int[])")
    @Deprecated
	public Pointer<T> set${sizePrim}s(long byteOffset, int[] values) {
		if (${sizePrim}.SIZE == 4) {
			setInts(byteOffset, values);
		} else {
			int n = values.length, s = 8;
			for (int i = 0; i < n; i++)
				setLong(i * s, values[i]);
		}
		return this;
	}
	
	#end
	
#docAllocateCopy("pointer", "Pointer")
    public static <T> Pointer<Pointer<T>> pointerToPointer(Pointer<T> value) {
		Pointer<Pointer<T>> p = (Pointer<Pointer<T>>)(Pointer)allocate(PointerIO.getPointerInstance());
		p.setPointer(0, value);
		return p;
	}
	
#docAllocateArrayCopy("pointer", "Pointer")
	public static <T> Pointer<Pointer<T>> pointerToPointers(Pointer<T>... values) {
		if (values == null)
			return null;
		int n = values.length, s = Pointer.SIZE;
		PointerIO<Pointer> pio = PointerIO.getPointerInstance(); // TODO get actual pointer instances PointerIO !!!
		Pointer<Pointer<T>> p = (Pointer<Pointer<T>>)(Pointer)allocateArray(pio, n);
		for (int i = 0; i < n; i++) {
			p.setPointer(i * s, values[i]);
		}
		return p;
	}
	
	/*
	static Class<?> getPrimitiveType(Buffer buffer) {

        #foreach ($prim in $primitivesNoBool)
		if (buffer instanceof ${prim.BufferName})
			return ${prim.WrapperName}.TYPE;
		#end
        throw new UnsupportedOperationException();
    }*/
    
    /**
     * Copy all values from an NIO buffer to the pointed memory location shifted by a byte offset
     */
    @Deprecated
	public Pointer<T> setValues(long byteOffset, Buffer values) {
        #foreach ($prim in $primitivesNoBool)
        if (values instanceof ${prim.BufferName}) {
            set${prim.CapName}s(byteOffset, (${prim.BufferName})values);
            return this;
        }
        #end
        throw new UnsupportedOperationException("Unhandled buffer type : " + values.getClass().getName());
    }
    
    /**
     * Copy length values from an NIO buffer (beginning at element at valuesOffset index) to the pointed memory location shifted by a byte offset
     */
    @Deprecated
	public Pointer<T> setValues(long byteOffset, Buffer values, int valuesOffset, int length) {
        #foreach ($prim in $primitivesNoBool)
        if (values instanceof ${prim.BufferName}) {
            set${prim.CapName}s(byteOffset, (${prim.BufferName})values, valuesOffset, length);
            return this;
        }
        #end
        throw new UnsupportedOperationException("Unhandled buffer type : " + values.getClass().getName());
    }
    
    /**
     * Copy values from an NIO buffer to the pointed memory location
     */
    public Pointer<T> setValues(Buffer values) {
    		#foreach ($prim in $primitivesNoBool)
        if (values instanceof ${prim.BufferName}) {
            set${prim.CapName}s((${prim.BufferName})values);
            return this;
        }
        #end
        throw new UnsupportedOperationException("Unhandled buffer type : " + values.getClass().getName());
    }

    /**
     * Copy bytes from the memory location indicated by this pointer to that of another pointer (with byte offsets for both the source and the destination), using the @see <a href="http://www.cplusplus.com/reference/clibrary/cstring/memcpy/">memcpy</a> C function.<br>
     * If the destination and source memory locations are likely to overlap, {@link Pointer#moveBytesTo(long, Pointer, long, long)} must be used instead.
     */
    @Deprecated
	public Pointer<T> copyBytesTo(long byteOffset, Pointer<?> destination, long byteOffsetInDestination, long byteCount) {
    		JNI.memcpy(destination.getCheckedPeer(byteOffsetInDestination, byteCount), getCheckedPeer(byteOffset, byteCount), byteCount);
    		return this;
    }
    
    /**
     * Copy bytes from the memory location indicated by this pointer to that of another pointer (with byte offsets for both the source and the destination), using the @see <a href="http://www.cplusplus.com/reference/clibrary/cstring/memmove/">memmove</a> C function.<br>
     * Works even if the destination and source memory locations are overlapping.
     */
    @Deprecated
	public Pointer<T> moveBytesTo(long byteOffset, Pointer<?> destination, long byteOffsetInDestination, long byteCount) {
    		JNI.memmove(destination.getCheckedPeer(byteOffsetInDestination, byteCount), getCheckedPeer(byteOffset, byteCount), byteCount);
    		return this;
    }
    
    private final long getValidBytes(String error) {
    		long rem = getValidBytes();
    		if (rem < 0)
    		//if (validEnd == UNKNOWN_VALIDITY)
    			throw new IndexOutOfBoundsException(error);

        return rem;
    }
    private final long getValidElements(String error) {
    		long rem = getValidElements();
    		if (rem < 0)
    			throw new IndexOutOfBoundsException(error);

        return rem;
    }
    private final PointerIO<T> getIO(String error) {
    		PointerIO<T> io = getIO();
        if (io == null)
            throwBecauseUntyped(error);
        return io;
    }
    
    /**
    * Copy remaining bytes from this pointer to a destination using the @see <a href="http://www.cplusplus.com/reference/clibrary/cstring/memcpy/">memcpy</a> C function (see {@link Pointer#copyBytesTo(long, Pointer, long, long)}, {@link Pointer#getValidBytes})
     */
    public void copyTo(Pointer<?> destination) {
    		copyBytesTo(0, destination, 0, getValidBytes("Cannot copy unbounded pointer without element count information. Please use copyTo(destination, elementCount) instead."));
    }
    
    /**
    * Copy remaining elements from this pointer to a destination using the @see <a href="http://www.cplusplus.com/reference/clibrary/cstring/memcpy/">memcpy</a> C function (see {@link Pointer#copyBytesTo(long, Pointer, long, long)}, {@link Pointer#getValidBytes})
     */
    public void copyTo(Pointer<?> destination, long elementCount) {
    		PointerIO<T> io = getIO("Cannot copy untyped pointer without byte count information. Please use copyTo(offset, destination, destinationOffset, byteCount) instead");
    		copyBytesTo(0, destination, 0, getValidElements() * io.getTargetSize());
    }


#foreach ($prim in $primitives)

	/**
	 * Write a ${prim.Name} value to the pointed memory location
	 */
    public Pointer<T> set${prim.CapName}(${prim.Name} value) {
		return set${prim.CapName}(0, value);
	}
	
	/**
	 * Read a ${prim.Name} value from the pointed memory location shifted by a byte offset
	 */
    @Deprecated
	public Pointer<T> set${prim.CapName}(long byteOffset, ${prim.Name} value) {
    	#if ($prim.Name != "byte" && $prim.Name != "boolean")
		if (!isOrdered()) {
			JNI.set_${prim.Name}_disordered(getCheckedPeer(byteOffset, ${prim.Size}), value);
			return this;
		}
		#end
		JNI.set_${prim.Name}(getCheckedPeer(byteOffset, ${prim.Size}), value);
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
    @Deprecated
	public Pointer<T> set${prim.CapName}s(long byteOffset, ${prim.Name}[] values) {
        return set${prim.CapName}s(byteOffset, values, 0, values.length);
    }
    
    /**
	 * Write an array of ${prim.Name} values of the specified length to the pointed memory location shifted by a byte offset, reading values at the given array offset and for the given length from the provided array.
	 */
    @Deprecated
	public Pointer<T> set${prim.CapName}s(long byteOffset, ${prim.Name}[] values, int valuesOffset, int length) {
        #if ($prim.Name != "byte" && $prim.Name != "boolean")
        if (!isOrdered()) {
        	JNI.set_${prim.Name}_array_disordered(getCheckedPeer(byteOffset, ${prim.Size} * length), values, valuesOffset, length);
        	return this;
    	}
        #end
		JNI.set_${prim.Name}_array(getCheckedPeer(byteOffset, ${prim.Size} * length), values, valuesOffset, length);
        return this;
	}
	
#docGet(${prim.Name} ${prim.WrapperName})
    public ${prim.Name} get${prim.CapName}() {
		return get${prim.CapName}(0);
    }
    
#docGetOffset(${prim.Name} ${prim.WrapperName} "Pointer#get${prim.CapName}()")
    @Deprecated
	public ${prim.Name} get${prim.CapName}(long byteOffset) {
        #if ($prim.Name != "byte" && $prim.Name != "boolean")
        if (!isOrdered())
        	return JNI.get_${prim.Name}_disordered(getCheckedPeer(byteOffset, ${prim.Size}));
        #end
        return JNI.get_${prim.Name}(getCheckedPeer(byteOffset, ${prim.Size}));
    }
    
#docGetArray(${prim.Name} ${prim.WrapperName})
	public ${prim.Name}[] get${prim.CapName}s(int length) {
    		return get${prim.CapName}s(0, length);
    }
    
  
#docGetRemainingArray(${prim.Name} ${prim.WrapperName})
    public ${prim.Name}[] get${prim.CapName}s() {
    		long rem = getValidElements("Cannot create array if remaining length is not known. Please use get${prim.CapName}s(int length) instead.");
		return get${prim.CapName}s(0, (int)rem);
    }

#docGetArrayOffset(${prim.Name} ${prim.WrapperName} "Pointer#get${prim.CapName}s(int)")
    @Deprecated
	public ${prim.Name}[] get${prim.CapName}s(long byteOffset, int length) {
        #if ($prim.Name != "byte" && $prim.Name != "boolean")
        if (!isOrdered())
        	return JNI.get_${prim.Name}_array_disordered(getCheckedPeer(byteOffset, ${prim.Size} * length), length);
        #end
        return JNI.get_${prim.Name}_array(getCheckedPeer(byteOffset, ${prim.Size} * length), length);
    }
    
#end
#foreach ($prim in $primitivesNoBool)

    /**
	 * Read ${prim.Name} values into the specified destination array from the pointed memory location
	 */
	public void get${prim.CapName}s(${prim.Name}[] dest) {
    		get${prim.BufferName}().get(dest);
    }
    
    /**
	 * Read length ${prim.Name} values into the specified destination array from the pointed memory location shifted by a byte offset, storing values after the provided destination offset.
	 */
    @Deprecated
	public void get${prim.CapName}s(long byteOffset, ${prim.Name}[] dest, int destOffset, int length) {
    		get${prim.BufferName}(byteOffset).get(dest, destOffset, length);
    }
    
	/**
	 * Write a buffer of ${prim.Name} values of the specified length to the pointed memory location
	 */
    public Pointer<T> set${prim.CapName}s(${prim.BufferName} values) {
		return set${prim.CapName}s(0, values, 0, values.capacity());
	}

    /**
	 * Write a buffer of ${prim.Name} values of the specified length to the pointed memory location shifted by a byte offset
	 */
    @Deprecated
	public Pointer<T> set${prim.CapName}s(long byteOffset, ${prim.BufferName} values) {
		return set${prim.CapName}s(byteOffset, values, 0, values.capacity());
	}

    /**
	 * Write a buffer of ${prim.Name} values of the specified length to the pointed memory location shifted by a byte offset, reading values at the given buffer offset and for the given length from the provided buffer.
	 */
    @Deprecated
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
    		long rem = getValidElements("Cannot create buffer if remaining length is not known. Please use get${prim.BufferName}(long length) instead.");
		return get${prim.BufferName}(0, rem);
	}
	
	/**
	 * Read a buffer of ${prim.Name} values of the specified length from the pointed memory location shifted by a byte offset
	 */
    @Deprecated
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
    
#end

	/**
	 * Type of a native character string.<br>
	 * In the native world, there are several ways to represent a string.<br>
	 * See {@link Pointer#getString(long, StringType, Charset)} and {@link Pointer#setString(long, String, StringType, Charset)}
	 */
    public enum StringType {
        /**
		 * C strings (a.k.a "NULL-terminated strings") have no size limit and are the most used strings in the C world.
		 * They are stored with the bytes of the string (using either a single-byte encoding such as ASCII, ISO-8859 or windows-1252 or a C-string compatible multi-byte encoding, such as UTF-8), followed with a zero byte that indicates the end of the string.<br>
		 * Corresponding C types : {@code char* }, {@code const char* }, {@code LPCSTR }<br>
		 * Corresponding Pascal type : {@code PChar }<br>
		 * See {@link Pointer#pointerToCString(String)}, {@link Pointer#getCString()} and {@link Pointer#setCString(String)}
		 */
		C(false, true),
		/**
		 * Wide C strings are stored as C strings (see {@link StringType#C}) except they are composed of shorts instead of bytes (and are ended by one zero short value = two zero byte values). 
		 * This allows the use of two-bytes encodings, which is why this kind of strings is often found in modern Unicode-aware system APIs.<br>
		 * Corresponding C types : {@code wchar_t* }, {@code const wchar_t* }, {@code LPCWSTR }<br>
		 * See {@link Pointer#pointerToWideCString(String)}, {@link Pointer#getWideCString()} and {@link Pointer#setWideCString(String)}
		 */
        WideC(true, true),
    		/**
		 * Pascal strings can be up to 255 characters long.<br>
		 * They are stored with a first byte that indicates the length of the string, followed by the ascii or extended ascii chars of the string (no support for multibyte encoding).<br>
		 * They are often used in very old Mac OS programs and / or Pascal programs.<br>
		 * Usual corresponding C types : {@code unsigned char* } and {@code const unsigned char* }<br>
		 * Corresponding Pascal type : {@code ShortString } (see @see <a href="http://www.codexterity.com/delphistrings.htm">http://www.codexterity.com/delphistrings.htm</a>)<br>
		 * See {@link Pointer#pointerToString(String, StringType, Charset)}, {@link Pointer#getString(StringType)}, {@link Pointer#setString(String, StringType)}, 
		 */
        PascalShort(false, true),
		/**
		 * Wide Pascal strings are ref-counted unicode strings that look like WideC strings but are prepended with a ref count and length (both 32 bits ints).<br>
		 * They are the current default in Delphi (2010).<br>
		 * Corresponding Pascal type : {@code WideString } (see @see <a href="http://www.codexterity.com/delphistrings.htm">http://www.codexterity.com/delphistrings.htm</a>)<br>
		 * See {@link Pointer#pointerToString(String, StringType, Charset)}, {@link Pointer#getString(StringType)}, {@link Pointer#setString(String, StringType)}, 
		 */
        PascalWide(true, true),
        /**
		 * Pascal ANSI strings are ref-counted single-byte strings that look like C strings but are prepended with a ref count and length (both 32 bits ints).<br>
		 * Corresponding Pascal type : {@code AnsiString } (see @see <a href="http://www.codexterity.com/delphistrings.htm">http://www.codexterity.com/delphistrings.htm</a>)<br>
		 * See {@link Pointer#pointerToString(String, StringType, Charset)}, {@link Pointer#getString(StringType)}, {@link Pointer#setString(String, StringType)}, 
		 */
        PascalAnsi(false, true),
        /**
         * Microsoft's BSTR strings, used in COM, OLE, MS.NET Interop and MS.NET Automation functions.<br>
         * See @see <a href="http://msdn.microsoft.com/en-us/library/ms221069.aspx">http://msdn.microsoft.com/en-us/library/ms221069.aspx</a> for more details.<br>
         * See {@link Pointer#pointerToString(String, StringType, Charset)}, {@link Pointer#getString(StringType)}, {@link Pointer#setString(String, StringType)}, 
		 */
        BSTR(true, true),
        /**
         * STL strings have compiler- and STL library-specific implementations and memory layouts.<br>
         * BridJ support reading and writing to / from pointers to most implementation's STL strings, though.
         * See {@link Pointer#pointerToString(String, StringType, Charset)}, {@link Pointer#getString(StringType)}, {@link Pointer#setString(String, StringType)}, 
		 */
		STL(false, false),
        /**
         * STL wide strings have compiler- and STL library-specific implementations and memory layouts.<br>
         * BridJ supports reading and writing to / from pointers to most implementation's STL strings, though.
         * See {@link Pointer#pointerToString(String, StringType, Charset)}, {@link Pointer#getString(StringType)}, {@link Pointer#setString(String, StringType)}, 
		 */
		WideSTL(true, false);
        //MFCCString,
        //CComBSTR,
        //_bstr_t
        
        final boolean isWide, canCreate;
        StringType(boolean isWide, boolean canCreate) {
			this.isWide = isWide;
			this.canCreate = canCreate;
        }
        
    }
	
    private static void notAString(StringType type, String reason) {
    		throw new RuntimeException("There is no " + type + " String here ! (" + reason + ")");
    }
    
    private void checkIntRefCount(StringType type, long byteOffset) {
    		int refCount = getInt(byteOffset);
		if (refCount <= 0)
			notAString(type, "invalid refcount: " + refCount);
    }
    
	/**
	 * Read a native string from the pointed memory location using the default charset.<br>
	 * See {@link Pointer#getString(long, StringType, Charset)} for more options.
	 * @param type Type of the native String to read. See {@link StringType} for details on the supported types.
	 * @return string read from native memory
	 */
	public String getString(StringType type) {
		return getString(0, type, null);
	}
	
	/**
	 * Read a native string from the pointed memory location, using the provided charset or the system's default if not provided.
	 * See {@link Pointer#getString(long, StringType, Charset)} for more options.
	 * @param type Type of the native String to read. See {@link StringType} for details on the supported types.
	 * @param charset Character set used to convert String characters to bytes. If null, {@link Charset#defaultCharset()} will be used
	 * @return string read from native memory
	 */
	public String getString(StringType type, Charset charset) {
		return getString(0, type, charset);
	}
	 
	
	String getSTLString(long byteOffset, StringType type, Charset charset) {
		// Assume the following layout :
		// - fixed buff of 16 chars
		// - ptr to dynamic array if the string is bigger
		// - size of the string (size_t)
		// - max allowed size of the string without the need for reallocation
		boolean wide = type == StringType.WideSTL;
		
		int fixedBuffLength = 16;
		int fixedBuffSize = wide ? fixedBuffLength * 2 : fixedBuffLength;
		long length = getSizeT(byteOffset + fixedBuffSize + Pointer.SIZE);
		long pOff;
		Pointer<?> p;
		if (length < fixedBuffLength - 1) {
			pOff = byteOffset;
			p = this;
		} else {
			pOff = 0;
			p = getPointer(byteOffset + fixedBuffSize + Pointer.SIZE);
		}
		int endChar = wide ? p.getChar(pOff + length * 2) : p.getByte(pOff + length);
		if (endChar != 0)
			notAString(type, "STL string format is not recognized : did not find a NULL char at the expected end of string of expected length " + length);
		return p.getString(pOff, wide ? StringType.WideC : StringType.C, charset);
	}
	
	static <U> Pointer<U> setSTLString(Pointer<U> pointer, long byteOffset, String s, StringType type, Charset charset) {
		boolean wide = type == StringType.WideSTL;
		
		int fixedBuffLength = 16;
		int fixedBuffSize = wide ? fixedBuffLength * 2 : fixedBuffLength;
		long lengthOffset = byteOffset + fixedBuffSize + Pointer.SIZE;
		long capacityOffset = lengthOffset + Pointer.SIZE;
		
		long length = s.length();
		if (pointer == null)// { && length > fixedBuffLength - 1)
			throw new UnsupportedOperationException("Cannot create STL strings (yet)");
		
		long currentLength = pointer.getSizeT(lengthOffset);
		long currentCapacity = pointer.getSizeT(capacityOffset);
		
		if (currentLength < 0 || currentCapacity < 0 || currentLength > currentCapacity)
			notAString(type, "STL string format not recognized : currentLength = " + currentLength + ", currentCapacity = " + currentCapacity);
		
		if (length > currentCapacity)
			throw new RuntimeException("The target STL string is not large enough to write a string of length " + length + " (current capacity = " + currentCapacity + ")");
		
		pointer.setSizeT(lengthOffset, length);
		
		long pOff;
		Pointer<?> p;
		if (length < fixedBuffLength - 1) {
			pOff = byteOffset;
			p = pointer;
		} else {
			pOff = 0;
			p = pointer.getPointer(byteOffset + fixedBuffSize + SizeT.SIZE);
		}
		
		int endChar = wide ? p.getChar(pOff + currentLength * 2) : p.getByte(pOff + currentLength);
		if (endChar != 0)
			notAString(type, "STL string format is not recognized : did not find a NULL char at the expected end of string of expected length " + currentLength);
		
		p.setString(pOff, s, wide ? StringType.WideC : StringType.C, charset);
		return pointer;
	}
    
	
	/**
	 * Read a native string from the pointed memory location shifted by a byte offset, using the provided charset or the system's default if not provided.
	 * @param byteOffset
	 * @param charset Character set used to convert String characters to bytes. If null, {@link Charset#defaultCharset()} will be used
	 * @param type Type of the native String to read. See {@link StringType} for details on the supported types.
	 * @return string read from native memory
	 */
	@Deprecated
	public String getString(long byteOffset, StringType type, Charset charset) {
        try {
			long len;
			
			switch (type) {
			case PascalShort:
				len = getByte(byteOffset) & 0xff;
				return new String(getBytes(byteOffset + 1, safeIntCast(len)), charset(charset));
			case PascalWide:
				checkIntRefCount(type, byteOffset - 8);
			case BSTR:
				len = getInt(byteOffset - 4);
				if (len < 0 || ((len & 1) == 1))
					notAString(type, "invalid byte length: " + len);
				//len = wcslen(byteOffset);
				if (getChar(byteOffset + len) != 0)
					notAString(type, "no null short after the " + len + " declared bytes");
				return new String(getChars(byteOffset, safeIntCast(len / 2)));
			case PascalAnsi:
				checkIntRefCount(type, byteOffset - 8);
				len = getInt(byteOffset - 4);
				if (len < 0)
					notAString(type, "invalid byte length: " + len);
				if (getByte(byteOffset + len) != 0)
					notAString(type, "no null short after the " + len + " declared bytes");
				return new String(getBytes(byteOffset, safeIntCast(len)), charset(charset));
			case C:
				len = strlen(byteOffset);
				return new String(getBytes(byteOffset, safeIntCast(len)), charset(charset));
			case WideC:
				len = wcslen(byteOffset);
				return new String(getChars(byteOffset, safeIntCast(len)));
			case STL:
			case WideSTL:
				return getSTLString(byteOffset, type, charset);
			default:
				throw new RuntimeException("Unhandled string type : " + type);
			}
		} catch (UnsupportedEncodingException ex) {
            throwUnexpected(ex);
            return null;
        }
	}

	/**
	 * Write a native string to the pointed memory location using the default charset.<br>
	 * See {@link Pointer#setString(long, String, StringType, Charset)} for more options.
	 * @param s string to write
	 * @param type Type of the native String to write. See {@link StringType} for details on the supported types.
	 * @return this
	 */
	public Pointer<T> setString(String s, StringType type) {
		return setString(this, 0, s, type, null);
	}
	
	
    /**
	 * Write a native string to the pointed memory location shifted by a byte offset, using the provided charset or the system's default if not provided.
	 * @param byteOffset
	 * @param s string to write
	 * @param charset Character set used to convert String characters to bytes. If null, {@link Charset#defaultCharset()} will be used
	 * @param type Type of the native String to write. See {@link StringType} for details on the supported types.
	 * @return this
	 */
	@Deprecated
	public Pointer<T> setString(long byteOffset, String s, StringType type, Charset charset) {
		return setString(this, byteOffset, s, type, charset);
	}
	
	private static String charset(Charset charset) {
		return (charset == null ? Charset.defaultCharset() : charset).name();
	}
			
	static <U> Pointer<U> setString(Pointer<U> pointer, long byteOffset, String s, StringType type, Charset charset) {
        try {
			if (s == null)
				return null;
			
			byte[] bytes;
			char[] chars;
			int bytesCount, headerBytes;
			int headerShift;
			
			switch (type) {
			case PascalShort:
				bytes = s.getBytes(charset(charset));
				bytesCount = bytes.length;
				if (pointer == null)
					pointer = (Pointer<U>)allocateBytes(bytesCount + 1);
				if (bytesCount > 255)
					throw new IllegalArgumentException("Pascal strings cannot be more than 255 chars long (tried to write string of byte length " + bytesCount + ")");
				pointer.setByte(byteOffset, (byte)bytesCount);
				pointer.setBytes(byteOffset + 1, bytes, 0, bytesCount);
				break;
			case C:
				bytes = s.getBytes(charset(charset));
				bytesCount = bytes.length;
				if (pointer == null)
					pointer = (Pointer<U>)allocateBytes(bytesCount + 1);
				pointer.setBytes(byteOffset, bytes, 0, bytesCount);
				pointer.setByte(byteOffset + bytesCount, (byte)0);
				break;
			case WideC:
				chars = s.toCharArray();
				bytesCount = chars.length * 2;
				if (pointer == null)
					pointer = (Pointer<U>)allocateChars(bytesCount + 2);
				pointer.setChars(byteOffset, chars);
				pointer.setChar(byteOffset + bytesCount, (char)0);
				break;
			case PascalWide:
				headerBytes = 8;
				chars = s.toCharArray();
				bytesCount = chars.length * 2;
				if (pointer == null) {
					pointer = (Pointer<U>)allocateChars(headerBytes + bytesCount + 2);
					byteOffset = headerShift = headerBytes;
				} else
					headerShift = 0;
				pointer.setInt(byteOffset - 8, 1); // refcount
				pointer.setInt(byteOffset - 4, bytesCount); // length header
				pointer.setChars(byteOffset, chars);
				pointer.setChar(byteOffset + bytesCount, (char)0);
				// Return a pointer to the WideC string-compatible part of the Pascal WideString
				return (Pointer<U>)pointer.offset(headerShift);
			case PascalAnsi:
				headerBytes = 8;
				bytes = s.getBytes(charset(charset));
				bytesCount = bytes.length;
				if (pointer == null) {
					pointer = (Pointer<U>)allocateBytes(headerBytes + bytesCount + 1);
					byteOffset = headerShift = headerBytes;
				} else
					headerShift = 0;
				pointer.setInt(byteOffset - 8, 1); // refcount
				pointer.setInt(byteOffset - 4, bytesCount); // length header
				pointer.setBytes(byteOffset, bytes);
				pointer.setByte(byteOffset + bytesCount, (byte)0);
				// Return a pointer to the WideC string-compatible part of the Pascal WideString
				return (Pointer<U>)pointer.offset(headerShift);
			case BSTR:
				headerBytes = 4;
				chars = s.toCharArray();
				bytesCount = chars.length * 2;
				if (pointer == null) {
					pointer = (Pointer<U>)allocateChars(headerBytes + bytesCount + 2);
					byteOffset = headerShift = headerBytes;
				} else
					headerShift = 0;
				pointer.setInt(byteOffset - 4, bytesCount); // length header IN BYTES
				pointer.setChars(byteOffset, chars);
				pointer.setChar(byteOffset + bytesCount, (char)0);
				// Return a pointer to the WideC string-compatible part of the Pascal WideString
				return (Pointer<U>)pointer.offset(headerShift);
			case STL:
			case WideSTL:
				return setSTLString(pointer, byteOffset, s, type, charset);
			default:
				throw new RuntimeException("Unhandled string type : " + type);
			}
	
			return (Pointer<U>)pointer;
		} catch (UnsupportedEncodingException ex) {
            throwUnexpected(ex);
            return null;
        }
    }
	
    /**
     * Allocate memory and write a string to it, using the system's default charset to convert the string (See {@link StringType} for details on the supported types).<br>
	 * See {@link Pointer#setString(String, StringType)}, {@link Pointer#getString(StringType)}.
	 * @param charset Character set used to convert String characters to bytes. If null, {@link Charset#defaultCharset()} will be used
	 * @param type Type of the native String to create.
	 */
	public static Pointer<?> pointerToString(String string, StringType type, Charset charset) {
		return setString(null, 0, string, type, charset);
	}
	
#macro (defPointerToString $string $eltWrapper)
    /**
     * Allocate memory and write a ${string} string to it, using the system's default charset to convert the string.  (see {@link StringType#${string}}).<br>
	 * See {@link Pointer#set${string}String(String)}, {@link Pointer#get${string}String()}.<br>
	 * See {@link Pointer#pointerToString(String, StringType, Charset)} for choice of the String type or Charset.
	 */
	 public static Pointer<$eltWrapper> pointerTo${string}String(String string) {
		return setString(null, 0, string, StringType.${string}, null);
	}
	
	/**
	 * The update will take place inside the release() call
	 */
    public static Pointer<Pointer<$eltWrapper>> pointerTo${string}Strings(final String... strings) {
    	if (strings == null)
    		return null;
        final int len = strings.length;
        final Pointer<$eltWrapper>[] pointers = (Pointer<$eltWrapper>[])new Pointer[len];
        Pointer<Pointer<$eltWrapper>> mem = allocateArray((PointerIO<Pointer<$eltWrapper>>)(PointerIO)PointerIO.getPointerInstance(${eltWrapper}.class), len, new Releaser() {
        	@Override
        	public void release(Pointer<?> p) {
        		Pointer<Pointer<$eltWrapper>> mem = (Pointer<Pointer<$eltWrapper>>)p;
        		for (int i = 0; i < len; i++) {
        			Pointer<$eltWrapper> pp = mem.get(i);
        			if (pp != null)
        				strings[i] = pp.get${string}String();
        			pp = pointers[i];
        			if (pp != null)
        				pp.release();
                }
        	}
        });
        for (int i = 0; i < len; i++)
            mem.set(i, pointers[i] = pointerTo${string}String(strings[i]));

		return mem;
    }
    
#end

#defPointerToString("C" "Byte")
#defPointerToString("WideC" "Character")

	
#foreach ($string in ["C", "WideC"])
	
	/**
	 * Read a ${string} string using the default charset from the pointed memory location (see {@link StringType#${string}}).<br>
	 * See {@link Pointer#get${string}String(long)}, {@link Pointer#getString(StringType)} and {@link Pointer#getString(long, StringType, Charset)} for more options
	 */
	public String get${string}String() {
		return get${string}String(0);
	}
	
	/**
	 * Read a ${string} string using the default charset from the pointed memory location shifted by a byte offset (see {@link StringType#${string}}).<br>
	 * See {@link Pointer#getString(long, StringType, Charset)} for more options
	 */
	@Deprecated
	public String get${string}String(long byteOffset) {
		return getString(byteOffset, StringType.${string}, null);
	}
	
	/**
	 * Write a ${string} string using the default charset to the pointed memory location (see {@link StringType#${string}}).<br>
	 * See {@link Pointer#set${string}String(long, String)} and {@link Pointer#setString(long, String, StringType, Charset)} for more options
	 */
	public Pointer<T> set${string}String(String s) {
        return set${string}String(0, s);
    }
    /**
	 * Write a ${string} string using the default charset to the pointed memory location shifted by a byte offset (see {@link StringType#${string}}).<br>
	 * See {@link Pointer#setString(long, String, StringType, Charset)} for more options
	 */
	@Deprecated
	public Pointer<T> set${string}String(long byteOffset, String s) {
        return setString(byteOffset, s, StringType.${string}, null);
    }
	
#end

	/**
	 * Get the length of the C string at the pointed memory location shifted by a byte offset (see {@link StringType#C}).
	 */
	protected long strlen(long byteOffset) {
		return JNI.strlen(getCheckedPeer(byteOffset, 1));
	}
	
	/**
	 * Get the length of the wide C string at the pointed memory location shifted by a byte offset (see {@link StringType#WideC}).
	 */
	protected long wcslen(long byteOffset) {
		long len = 0;
		while (getShort(byteOffset + len * 2) != 0)
			len++;
		return len; //BUGGY: JNI.wcslen(getCheckedPeer(byteOffset, 1));
	}
	
	/**
	 * Write zero bytes to the first length bytes pointed by this pointer
	 */
	public void clearBytes(long length) {
		clearBytes(0, length, (byte)0);	
	}
	/**
	 * Write a byte {@code value} to each of the {@code length} bytes at the address pointed to by this pointer shifted by a {@code byteOffset}
	 */
	public void clearBytes(long byteOffset, long length, byte value) {
		JNI.memset(getCheckedPeer(byteOffset, length), value, length);
	}
	
	/**
	 * Find the first occurrence of a value in the memory block of length searchLength bytes pointed by this pointer shifted by a byteOffset 
	 */
	public Pointer<T> findByte(long byteOffset, byte value, long searchLength) {
		long ptr = getCheckedPeer(byteOffset, searchLength);
		long found = JNI.memchr(ptr, value, searchLength);	
		return found == 0 ? null : offset(found - ptr);
	}
	
	/**
	 * Implementation of {@link List#add(Object)} that throws UnsupportedOperationException
	 * @throws UnsupportedOperationException
	 */
    @Deprecated
	public boolean add(T item) {
		throw new UnsupportedOperationException();
	}
	
    /**
	 * Implementation of {@link List#add(int, Object)} that throws UnsupportedOperationException
	 * @throws UnsupportedOperationException
	 */
    @Deprecated
	public void add(int index, T element) {
		throw new UnsupportedOperationException();
	}
	
    /**
	 * Implementation of {@link List#addAll(Collection)} that throws UnsupportedOperationException
	 * @throws UnsupportedOperationException
	 */
    @Deprecated
	public boolean addAll(Collection<? extends T> c) {
		throw new UnsupportedOperationException();
	}
	
    /**
	 * Implementation of {@link List#addAll(int, Collection)} that throws UnsupportedOperationException
	 * @throws UnsupportedOperationException
	 */
    @Deprecated
	public boolean addAll(int index, Collection<? extends T> c) {
		throw new UnsupportedOperationException();
	}
	
    /**
	 * Implementation of {@link List#clear()} that throws UnsupportedOperationException
	 * @throws UnsupportedOperationException
	 */
    @Deprecated
	public void clear() {
		throw new UnsupportedOperationException();
	}
	
    /**
	 * Implementation of {@link List#contains(Object)} that throws UnsupportedOperationException
	 * @throws UnsupportedOperationException
	 */
    @Deprecated
	public boolean contains(Object o) {
		throw new UnsupportedOperationException();
	}
	
    /**
	 * Implementation of {@link List#containsAll(Collection)} that throws UnsupportedOperationException
	 * @throws UnsupportedOperationException
	 */
    @Deprecated
	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}
	
    /**
	 * Implementation of {@link List#get(int)}
	 */
	public final T get(int index) {
		return get((long)index);
	}
	
	/**
	 * Alias for {@link Pointer#get(long)} defined for more natural use from the Scala language.
	 */
    public final T apply(long index) {
		return get(index);
	}
	
    /**
	 * Implementation of {@link List#indexOf(Object)} that throws UnsupportedOperationException
	 * @throws UnsupportedOperationException
	 */
    @Deprecated
	public int indexOf(Object o) {
		throw new UnsupportedOperationException();
	}
	
    /**
	 * Implementation of {@link List#isEmpty()}
	 */
	public boolean isEmpty() {
		return getValidElements() == 0;
	}
	
    /**
     * Implementation of {@link List#lastIndexOf(Object)} that throws UnsupportedOperationException
	 * @throws UnsupportedOperationException
	 */
    @Deprecated
	public int lastIndexOf(Object o) {
		throw new UnsupportedOperationException();
	}
	
    /**
	 * Implementation of {@link List#listIterator()}
	 */
	public ListIterator<T> listIterator() {
		return iterator();
	}
	
    /**
	 * Implementation of {@link List#listIterator(int)}
	 */
	public ListIterator<T> listIterator(int index) {
		return next(index).listIterator();
	}
	
    /**
	 * Implementation of {@link List#remove(int)} that throws UnsupportedOperationException
	 * @throws UnsupportedOperationException
	 */
    @Deprecated
	public T remove(int index) {
		throw new UnsupportedOperationException();
	}
	
    /**
	 * Implementation of {@link List#remove(Object)} that throws UnsupportedOperationException
	 * @throws UnsupportedOperationException
	 */
    @Deprecated
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}
	
    /**
	 * Implementation of {@link List#removeAll(Collection)} that throws UnsupportedOperationException
	 * @throws UnsupportedOperationException
	 */
    @Deprecated
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Implementation of {@link List#retainAll(Collection)} that throws UnsupportedOperationException
	 * @throws UnsupportedOperationException
	 */
    @Deprecated
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}
	
    /**
	 * Implementation of {@link List\#set(int, Object)}
	 */
	public final T set(int index, T element) {
		set((long)index, element);
		return element;
	}
	
	/**
	 * Alias for {@link Pointer\#set(long, Object)} defined for more natural use from the Scala language.
	 */
	public final void update(long index, T element) {
		set(index, element);
	}
	
    /**
	 * Implementation of {@link List#size()}
	 * @deprecated Casts the result of getValidElements() to int, so sizes greater that 2^31 will be invalid
	 * @return {@link Pointer#getValidElements()}
	 */
	public int size() {
		long size = getValidElements();
		if (size > Integer.MAX_VALUE)
			throw new RuntimeException("Size is greater than Integer.MAX_VALUE, cannot convert to int in Pointer.size()");
		return (int)size;
	}
	
    /**
	 * Implementation of {@link List#subList(int, int)}
	 */
	public List<T> subList(int fromIndex, int toIndex) {
		getIO("Cannot create sublist");
        return next(fromIndex).validElements(toIndex - fromIndex);
	}
	
    /**
	 * Implementation of {@link List#toArray()}
	 */
	public T[] toArray() {
		getIO("Cannot create array");
        return toArray((int)getValidElements("Length of pointed memory is unknown, cannot create array out of this pointer"));
	}
	
	T[] toArray(int length) {
        Class<?> c = Utils.getClass(getIO("Cannot create array").getTargetType());
		if (c == null)
			throw new RuntimeException("Unable to get the target type's class (target type = " + io.getTargetType() + ")");
        return (T[])toArray((Object[])Array.newInstance(c, length));
	}
	
    /**
	 * Implementation of {@link List#toArray(Object[])}
	 */
	public <U> U[] toArray(U[] array) {
		int n = (int)getValidElements();
		if (n < 0)
            throwBecauseUntyped("Cannot create array");
        
        if (array.length != n)
        	return (U[])toArray();
        
        for (int i = 0; i < n; i++)
        	array[i] = (U)get(i);
        return array;
	}
}
