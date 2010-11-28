package org.bridj;
import org.bridj.cpp.*;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.*;
import java.util.Iterator;
import org.bridj.ann.Ptr;
import org.bridj.ann.Library;
import org.bridj.ann.Runtime;
import static org.junit.Assert.*;
import static org.bridj.Pointer.*;

@Library("test")
@Runtime(CPPRuntime.class)
public class PointerTest {
	static {
		BridJ.register();
	}
	int n = 3;
	static final ByteOrder[] orders = new ByteOrder[] { ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN };
    
	@Test
	public void testIdentities() {
		Pointer<Integer> p = allocateInt();
		assertTrue(p == (Pointer)p.offset(0));
		assertTrue(p == (Pointer)p.next(0));
		assertTrue(p == (Pointer)p.withIO(p.getIO()));
		assertTrue(p == (Pointer)p.asPointerTo(p.getIO().getTargetType()));
		assertTrue(p == (Pointer)p.order(p.order()));
	}
	/*
	@Test
	public void testFloatEndian() {
		ByteBuffer b = ByteBuffer.allocateDirect(20);
		b.order(ByteOrder.BIG_ENDIAN).asFloatBuffer().put(0, 10.0f);
		assertEquals(10.0f, b.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(0), 0);	
	}*/
	@Test
	public void testFloatEndian() {
		Pointer<Double> b = allocateDouble();
		for (ByteOrder bo : new ByteOrder[] { ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN }) {
			b.order(bo).setDouble(10.0);
			assertEquals(10.0, b.order(bo).getDoubles(1)[0], 0);
			assertEquals(10.0, b.order(bo).getDoubleBuffer().get(0), 0);
		}
	}
	@Test(expected=UnsupportedOperationException.class)
	public void noRef() {
		allocateBytes(10).getReference();
	}
	@Test(expected=UnsupportedOperationException.class)
	public void noRemoveIt() {
		Iterator<Byte> it = allocateBytes(10).iterator();
		assertTrue(it.hasNext());
		it.next();
		it.remove();
	}
	@Test(expected=RuntimeException.class)
	public void untypedGet() {
		allocateBytes(10).as(null).get(0);
	}
	
	@Test
	public void findByte() {
		Pointer<Byte> p = pointerToBytes((byte)1, (byte)2, (byte)3, (byte)4);
		assertNotNull(p.findByte(0, (byte)2, 4));
		assertNull(p.findByte(0, (byte)5, 4));
		assertNull(p.findByte(0, (byte)2, 1));
	}
	
	@Test
	public void alignment() {
		Pointer<Integer> p = allocateInts(2);
		assertTrue(p.isAligned());
		assertTrue(!p.offset(1).isAligned());
		assertTrue(!p.offset(2).isAligned());
		assertTrue(p.offset(2).isAligned(2));
		assertTrue(!p.offset(3).isAligned());
		assertTrue(p.offset(4).isAligned());
	}
	
	@Test
	public void iterate() {
		int i = 0;
		for (int v : pointerToInts(0, 1, 2, 3, 4)) {
			assertEquals(i, v);
			i++;
		}
	}
	
	@Test
	public void basicTest() {
		Pointer<Byte> p = allocateBytes(10);
		assertTrue(p == p.offset(0));
		assertEquals(p, p);
		
		assertTrue(!p.equals(p.offset(1)));
		assertEquals(p, p.offset(1).offset(-1));
		
		assertEquals(new Long(p.getPeer()).hashCode(), p.hashCode());
		
		assertEquals(1, p.compareTo(null));
		assertEquals(-1, p.compareTo(p.offset(1)));
		assertEquals(0, p.compareTo(p.offset(1).offset(-1)));
		assertEquals(1, p.offset(1).compareTo(p.offset(1).offset(-1)));
		
		assertTrue(!allocateBytes(10).equals(allocateBytes(10)));
	}
	
	@Test
	public void refTest() {
		Pointer<Pointer<?>> pp = allocatePointers(10);
		Pointer<?> pa = allocateBytes(5);
		pp.set(2, pa);
		Pointer<?> p = pp.get(2);
		assertEquals(p, pa);
		Pointer ref = p.getReference();
		assertNotNull(ref);
		assertEquals(pp.offset(2 * Pointer.SIZE), ref);
	}
	
#macro (testString $string $eltWrapper)
	@Test
    public void test${string}String() {
    		String s = "Hello, World !";
    		Pointer<$eltWrapper> p = pointerTo${string}String(s);
    		assertEquals(s, p.get${string}String());
	}
#end
#testString("C", "Byte")
#testString("WideC", "Character")

	@Test
    public void testStrings() {
		String s = "Hello, World !";
		String s2 = "Hello you !";
		Charset charset = null;
		for (int offset : new int[] { 0, 1, 4, 10 }) {
			for (Pointer.StringType type : Pointer.StringType.values()) {
				if (!type.canCreate)
					continue;
				
				Pointer<?> p = pointerToString(s, charset, type);
				assertEquals("Failed alloc / set of string type " + type, s, p.getString(type));
				
				p.setString(s2, type);
				assertEquals("Failed set / get of string type " + type, s2, p.getString(type));
			}
		}
	}
	
	/*
	public static native Pointer<?> newString();
	public static native Pointer<?> newWString();

	public static native void deleteWString(Pointer<?> s);
	public static native void appendToWString(Pointer<?> s, Pointer<Character> a);
	public static native void resizeWString(Pointer<?> s, @Ptr long newSize);
	public static native void reserveWString(Pointer<?> s, @Ptr long newCapacity);
	public static native Pointer<Character> wstringCStr(Pointer<?> s);
	
	public static native void deleteString(Pointer<?> s);
	public static native void appendToString(Pointer<?> s, Pointer<Byte> a);
	public static native void resizeString(Pointer<?> s, @Ptr long newSize);
	public static native void reserveString(Pointer<?> s, @Ptr long newCapacity);
	public static native Pointer<Byte> stringCStr(Pointer<?> s);
	
	@Test
	public void stlTestTest() {
		//if (true) return;
		String s1 = "Test !";
		String s2 = "Test, yeah man ! Test, yeah man 2 ! Test, yeah man 3 !";
		Pointer<?> p = newString();
		System.err.println("Created new string : " + p);
		appendToString(p, pointerToCString(s1));
		assertEquals(s1, stringCStr(p).getCString());
		System.out.println("Created string just fine !");
		
		resizeString(p, 0);
		appendToString(p, pointerToCString(s2));
		assertEquals(s2, stringCStr(p).getCString());
	}
	*/

	@Test
	public void testAllocateArrayPrim() {
#foreach ($prim in $bridJPrimitives)
		{
			Pointer[] ptrs = new Pointer[] { 
				Pointer.allocate${prim.CapName}(), 
				Pointer.allocate${prim.CapName}s(2), 
				Pointer.allocateArray(${prim.Name}.class, 2), 
				Pointer.allocateArray(${prim.WrapperName}.class, 2) 
			};
			for (Pointer<${prim.WrapperName}> ptr : ptrs) {
				assertTrue(ptr.getIO() == (PointerIO)PointerIO.get${prim.CapName}Instance());
				assertTrue(ptr.getIO() == (PointerIO)PointerIO.getInstance(${prim.Name}.class));
			}
		}
#end
	}
	
	@Test(expected=RuntimeException.class)
	public void testUntypedSize() {
		Pointer<Integer> p = allocateInt();
		Pointer<?> up = pointerToAddress(p.getPeer());
		up.getTargetSize();
	}
	@Test(expected=RuntimeException.class)
	public void testUntypedNext() {
		Pointer<Integer> p = allocateInts(2);
		Pointer<?> up = pointerToAddress(p.getPeer());
		up.next();
	}
	
	@Test
	public void testPointerToNulls() {
		assertEquals(0, getPeer(null));
		assertEquals(null, pointerToAddress(0));
		assertEquals(null, pointerToBuffer(null));
		
#foreach ($prim in $bridJPrimitives)
		{
			assertTrue(pointerTo${prim.CapName}s((${prim.Name}[])null) == null);
		}
#end
#foreach ($prim in $primitives)
		{
			// TODO implement 2D and 3D arrays for CLong, SizeT !
			assertTrue(pointerTo${prim.CapName}s((${prim.Name}[][])null) == null);
			assertTrue(pointerTo${prim.CapName}s((${prim.Name}[][][])null) == null);
		}
#end
#foreach ($prim in $primitivesNoBool)
		{
			assertTrue(pointerTo${prim.CapName}s((${prim.BufferName})null) == null);
		}
#end
	}

#foreach ($prim in $bridJPrimitives)

#if ($prim.Name == "double" || $prim.Name == "float")
#set ($precisionArg = ", 0")
#else
#set ($precisionArg = "")
#end

	static ${prim.Name}[] createExpected${prim.CapName}s(int n) {
		${prim.Name}[] expected = new ${prim.Name}[n];
		expected[0] = ${prim.v1};
		expected[1] = ${prim.v2};
		expected[2] = ${prim.v3};
		//for (int i = 0; i < n; i++)
		//	expected[i] = (${prim.Name})(i + 1);
		return expected;
	}
	
	@Test 
    public void test${prim.CapName}sIterator() {
		${prim.Name}[] expected = createExpected${prim.CapName}s(n);
		Pointer<${prim.WrapperName}> p = Pointer.pointerTo${prim.CapName}s(expected);
		long peer = p.getPeer();
		
		Iterator<${prim.WrapperName}> it = p.iterator();
		for (int i = 0; i < n; i++) {
			assertTrue(it.hasNext());
			${prim.WrapperName} obVal = it.next();
			assertNotNull(obVal);
			${prim.Name} val = obVal;
			assertEquals("at position i = " + i, (Object)expected[i], (Object)val);
		}
		assertTrue(!it.hasNext());
	}
	
	
	@Test 
    public void testPointerTo_${prim.Name}_Values() {
		// Test pointerToInts(int...)
		Pointer<${prim.WrapperName}> p = Pointer.pointerTo${prim.CapName}s(${prim.v1}, ${prim.v2}, ${prim.v3});
		assertEquals(${prim.v1}, (${prim.Name})p.get(0)$precisionArg);
		assertEquals(${prim.v2}, (${prim.Name})p.get(1)$precisionArg);
		assertEquals(${prim.v3}, (${prim.Name})p.get(2)$precisionArg);
	}
	@Test 
    public void testPointerTo_${prim.Name}_Value() {
		Pointer<${prim.WrapperName}> p = Pointer.pointerTo${prim.CapName}(${prim.v1});
		assertEquals(${prim.v1}, (${prim.Name})p.get(0)$precisionArg);
	}

	
	
#end

#foreach ($prim in $primitivesNoBool)	

#if ($prim.Name == "double" || $prim.Name == "float")
#set ($precisionArg = ", 0")
#else
#set ($precisionArg = "")
#end
	
	@Test 
    public void simpleSetGet${prim.CapName}s_ENDIAN() {
    	for (ByteOrder order : new ByteOrder[] { ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN }) {
			Pointer<${prim.WrapperName}> p = allocate${prim.CapName}s(3).order(order);
			p.set${prim.CapName}(${prim.v1});
			assertEquals(${prim.v1}, (${prim.Name})p.get${prim.CapName}()$precisionArg);
			
			p.set${prim.CapName}(${prim.Size}, (${prim.Name})-2);
			assertEquals((${prim.Name})-2, (${prim.Name})p.get${prim.CapName}(${prim.Size})$precisionArg);
			
			p.set(2, ${prim.v3});
			assertEquals(${prim.v3}, (${prim.Name})p.get(2)$precisionArg);
			
			p.set${prim.CapName}s(${prim.Size}, new ${prim.Name}[] { (${prim.Name})5, (${prim.Name})6 });
			assertEquals((${prim.Name})5, (${prim.Name})p.get(1)$precisionArg);
			assertEquals((${prim.Name})6, (${prim.Name})p.get(2)$precisionArg);
			${prim.Name}[] a = p.get${prim.CapName}s(${prim.Size}, 2);
			assertEquals(2, a.length);
			assertEquals((${prim.Name})5, a[0]$precisionArg);
			assertEquals((${prim.Name})6, a[1]$precisionArg);
		}
	}
	
	@Test 
    public void testAllocateBounds_${prim.Name}_ok() {
		assertEquals((${prim.Name})0, (${prim.Name})Pointer.allocate${prim.CapName}().get(0)$precisionArg);
		assertEquals((${prim.Name})0, (${prim.Name})Pointer.allocate${prim.CapName}s(1).get(0)$precisionArg);
		assertEquals((${prim.Name})0, (${prim.Name})Pointer.allocate${prim.CapName}s(2).offset(${prim.Size}).get(-1)$precisionArg);
		
		//TODO slide, slideBytes
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testPointerTo_${prim.Name}_IndirectBuffer() {
    		pointerTo${prim.CapName}s(${prim.BufferName}.wrap(new ${prim.Name}[3]));
	}
	
	@Test 
    public void testGet${prim.BufferName}() {
	
    		for (int type = 0; type < 3; type++) {
			Pointer<${prim.WrapperName}> p = Pointer.allocate${prim.CapName}s(n);
			${prim.Name}[] expected = createExpected${prim.CapName}s(n);
			${prim.BufferName} buf = ${prim.BufferName}.wrap(expected);
			
			switch (type) {
			case 0:
				p.setValues(buf);
				break;
			case 1:
				p.setValues(0, buf, 0, expected.length);
				break;
			case 2:
				p.set${prim.CapName}s(buf);
				break;
			}
			
			${prim.Name}[] values = p.get${prim.CapName}s(0, n);
			${prim.BufferName} valuesBuffer = p.get${prim.CapName}Buffer(0, n);
			
			for (int i = 0; i < n; i++) {
				assertEquals(expected[i], values[i]$precisionArg);
				assertEquals(expected[i], valuesBuffer.get(i)$precisionArg);
				assertEquals(expected[i], (${prim.Name})p.get${prim.CapName}(i * ${prim.Size})$precisionArg);
			}
		}
	}
	@Test 
    public void testSetGet${prim.CapName}s() {
		Pointer<${prim.WrapperName}> p = null;
		${prim.Name}[] expected = createExpected${prim.CapName}s(n);
			
		for (boolean autoSize : new boolean[] { false, true }) {
			${prim.Name}[] values;
			
			p = Pointer.allocate${prim.CapName}s(n);
			if (autoSize) {
				p.set${prim.CapName}s(expected);
				values = p.get${prim.CapName}s();
			} else { 
				p.set${prim.CapName}s(0, expected);
				values = p.get${prim.CapName}s(0, n);
			}
			
			${prim.BufferName} valuesBuffer = p.get${prim.CapName}Buffer(0, n);
			
			for (int i = 0; i < n; i++) {
				assertEquals(expected[i], values[i]$precisionArg);
				assertEquals(expected[i], valuesBuffer.get(i)$precisionArg);
				assertEquals(expected[i], (${prim.Name})p.get${prim.CapName}(i * ${prim.Size})$precisionArg);
			}
		}
		
		for (int i = 0; i < n; i++) {
			expected[i] = (${prim.Name})((i + 1) * 10);
			p.set${prim.CapName}(i * ${prim.Size}, expected[i]);
		}
		
		for (int i = 0; i < n; i++)
			assertEquals(expected[i], (${prim.Name})p.get${prim.CapName}(i * ${prim.Size})$precisionArg);
	}

	@Test 
    public void testPointerTo_${prim.Name}_DirectBuffer() {
    		Pointer<${prim.WrapperName}> p = Pointer.allocate${prim.CapName}s(3);
    		assertEquals(3 * ${prim.Size}, p.getValidBytes());
		p.set(0, ${prim.v1});
		p.set(1, ${prim.v2});
		p.set(2, ${prim.v3});
		${prim.BufferName} b = p.get${prim.BufferName}();
		assertEquals(3, b.capacity());
		
		for (boolean generic : new boolean[] { false, true }) {
			if (generic)
				p = (Pointer<${prim.WrapperName}>)Pointer.pointerToBuffer(b);
			else
				p = Pointer.pointerTo${prim.CapName}s(b);
			
			assertEquals(3 * ${prim.Size}, p.getValidBytes());
			assertEquals(${prim.v1}, (${prim.Name})p.get(0)$precisionArg);
			assertEquals(${prim.v2}, (${prim.Name})p.get(1)$precisionArg);
			assertEquals(${prim.v3}, (${prim.Name})p.get(2)$precisionArg);
		}
	}
	
	@Test 
    public void testPointerTo_${prim.Name}_Values2D() {
		${prim.Name}[][] values = new ${prim.Name}[][] {
				{${prim.v1}, ${prim.v2}},
				{${prim.v1}, ${prim.v2}},
				{${prim.v1}, ${prim.v2}}
		};
		Pointer<Pointer<${prim.WrapperName}>> p = Pointer.pointerTo${prim.CapName}s(values);
		int dim2 = values[0].length;
		for (int i = 0; i < values.length; i++)
			for (int j = 0; j < dim2; j++)
				assertEquals(values[i][j], (${prim.Name})p.get(i).get(j)$precisionArg);
	}
	
	/*
	@Test 
    public void testPointerTo_${prim.Name}_Values3D() {
		${prim.Name}[][][] values = new ${prim.Name}[][][] {
			{
				{${prim.v1}, ${prim.v2}},
				{${prim.v1}, ${prim.v2}},
				{${prim.v1}, ${prim.v2}}
			},
			{
				{${prim.v1}, ${prim.v2}},
				{${prim.v1}, ${prim.v2}},
				{${prim.v1}, ${prim.v2}}
			}
		};
		Pointer<Pointer<Pointer<${prim.WrapperName}>>> p = Pointer.pointerTo${prim.CapName}s(values);
		int dim2 = values[0].length;
		int dim3 = values[0][0].length;
		for (int i = 0; i < values.length; i++) {
			for (int j = 0; j < dim2; j++) {
				for (int k = 0; k < dim3; k++) {
					Object o = values[i][j][k];
					System.out.println(o);
					System.out.println("p.get(i) = " + p.get(i));
					System.out.println("p.get(i).get(j) = " + p.get(i).get(j));
					System.out.println("p.get(i).get(j).get(k) = " + p.get(i).get(j).get(k));
					// TODO 
					assertEquals(values[i][j][k], (${prim.Name})p.get(i).get(j).get(k)$precisionArg);
				}
			}
		}
				
	}
	*/
	
	@Test 
    public void testAllocateRemaining_${prim.Name}_ok() {
    	Pointer<${prim.WrapperName}> p = Pointer.allocate${prim.CapName}s(2);
    	assertEquals(2, p.getValidElements());
		assertEquals(2 * ${prim.Size}, p.getValidBytes());
		
		Pointer<${prim.WrapperName}> n = p.next();
		Pointer<${prim.WrapperName}> o = p.offset(${prim.Size});
		assertEquals(n, o);
		
		assertEquals(1, n.getValidElements());
		assertEquals(${prim.Size}, n.getValidBytes());
		assertEquals(1, o.getValidElements());
		assertEquals(${prim.Size}, o.getValidBytes());
		
		//TODO slide, slideBytes
	}
	
	@Test(expected=IndexOutOfBoundsException.class)
    public void testAllocateBounds_${prim.Name}_failAfter() {
		Pointer.allocate${prim.CapName}().get(1);
	}
	@Test(expected=IndexOutOfBoundsException.class)
    public void testAllocateBounds_${prim.Name}_failBefore() throws IndexOutOfBoundsException {
		Pointer.allocate${prim.CapName}().get(-1);
	}

	@Test
    public void test${prim.CapName}Order() {
    	for (ByteOrder order : orders) {
    		boolean isOrdered = order.equals(ByteOrder.nativeOrder());
    		Pointer<${prim.WrapperName}> p = Pointer.allocate${prim.CapName}().order(order);
    		assertEquals(order, p.order());
    		assertEquals(isOrdered, p.isOrdered());
    	}
    }
    #if (($prim.Name == "short") || ($prim.Name == "int") || ($prim.Name == "long") || ($prim.Name == "double") || ($prim.Name == "float"))
	@Test
	public void test${prim.CapName}Endianness() {
		for (${prim.Name} value : new ${prim.Name}[] { (${prim.Name})0, ${prim.v1}, (${prim.Name})-1 }) {
			test${prim.CapName}Endianness(ByteOrder.LITTLE_ENDIAN, value);
			test${prim.CapName}Endianness(ByteOrder.BIG_ENDIAN, value);
		}
	}
	void test${prim.CapName}Endianness(ByteOrder order, ${prim.Name} value) {
		Pointer<${prim.WrapperName}> p = Pointer.allocate${prim.CapName}().order(order);
		p.set(value);
        assertEquals(order, p.order());
        assertEquals(order, p.get${prim.BufferName}(0, 1).order());
		assertEquals(value, (${prim.Name})p.get${prim.BufferName}(0, 1).get()$precisionArg); // check that the NIO buffer was created with the correct order by default
		assertEquals(value, p.getByteBuffer(0, ${prim.Size}).order(order).as${prim.BufferName}().get()$precisionArg);
	}
	#end

#end
}
