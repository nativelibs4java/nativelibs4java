package org.bridj;

import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.*;
import java.util.Iterator;
import static org.junit.Assert.*;
import static org.bridj.Pointer.*;

public class PointerTest {
	int n = 10;
	static final ByteOrder[] orders = new ByteOrder[] { ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN };
    
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
				Pointer<?> p = pointerToString(s, charset, type);
				assertEquals("Failed alloc / set of string type " + type, s, p.getString(type));
				
				p.setString(s2, type);
				assertEquals("Failed set / get of string type " + type, s2, p.getString(type));
			}
		}
	}

#foreach ($prim in $primitivesNoBool)
	

	static ${prim.Name}[] createExpected${prim.CapName}s(int n) {
		${prim.Name}[] expected = new ${prim.Name}[n];
		for (int i = 0; i < n; i++)
			expected[i] = (${prim.Name})(i + 1);
		return expected;
	}
	
	
	@Test 
    public void testSetGet${prim.BufferName}() {
		${prim.Name}[] expected = createExpected${prim.CapName}s(n);
		Pointer<${prim.WrapperName}> p = Pointer.pointerTo${prim.CapName}s(expected);
		long peer = p.getPeer();
		
		Iterator<${prim.WrapperName}> it = p.iterator();
		for (int i = 0; i < n; i++) {
			assertTrue(it.hasNext());
			${prim.WrapperName} obVal = it.next();
			assertNotNull(obVal);
			${prim.Name} val = obVal;
			assertEquals("at position i = " + i, expected[i], val, 0);
		}
		assertTrue(!it.hasNext());
	}
	
	@Test 
    public void simpleSetGet${prim.BufferName}() {
    	for (ByteOrder order : new ByteOrder[] { ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN }) {
			Pointer<${prim.WrapperName}> p = allocate${prim.CapName}s(3).order(order);
			p.set${prim.CapName}((${prim.Name})1);
			assertEquals((${prim.Name})1, p.get${prim.CapName}(), 0);
			
			p.set${prim.CapName}(${prim.Size}, (${prim.Name})-2);
			assertEquals((${prim.Name})-2, p.get${prim.CapName}(${prim.Size}), 0);
			
			p.set(2, (${prim.Name})3);
			assertEquals((${prim.Name})3, p.get(2), 0);
			
			p.set${prim.CapName}s(${prim.Size}, new ${prim.Name}[] { (${prim.Name})5, (${prim.Name})6 });
			assertEquals((${prim.Name})5, p.get(1), 0);
			assertEquals((${prim.Name})6, p.get(2), 0);
			${prim.Name}[] a = p.get${prim.CapName}s(${prim.Size}, 2);
			assertEquals(2, a.length);
			assertEquals((${prim.Name})5, a[0], 0);
			assertEquals((${prim.Name})6, a[1], 0);
		}
	}
	
	@Test 
    public void testGet${prim.BufferName}() {
		
		Pointer<${prim.WrapperName}> p = Pointer.allocate${prim.CapName}s(n);
		${prim.Name}[] expected = createExpected${prim.CapName}s(n);
		${prim.BufferName} buf = ${prim.BufferName}.wrap(expected);
		
		p.set${prim.CapName}s(buf);
		
		${prim.Name}[] values = p.get${prim.CapName}s(0, n);
		${prim.BufferName} valuesBuffer = p.get${prim.CapName}Buffer(0, n);
		
		for (int i = 0; i < n; i++) {
			assertEquals(expected[i], values[i], 0);
			assertEquals(expected[i], valuesBuffer.get(i), 0);
			assertEquals(expected[i], p.get${prim.CapName}(i * ${prim.Size}), 0);
		}
		
	}
	@Test 
    public void testSetGet${prim.CapName}s() {
		
		Pointer<${prim.WrapperName}> p = Pointer.allocate${prim.CapName}s(n);
		${prim.Name}[] expected = createExpected${prim.CapName}s(n);
		
		p.set${prim.CapName}s(0, expected);
		${prim.Name}[] values = p.get${prim.CapName}s(0, n);
		${prim.BufferName} valuesBuffer = p.get${prim.CapName}Buffer(0, n);
		
		for (int i = 0; i < n; i++) {
			assertEquals(expected[i], values[i], 0);
			assertEquals(expected[i], valuesBuffer.get(i), 0);
			assertEquals(expected[i], p.get${prim.CapName}(i * ${prim.Size}), 0);
		}
		
		for (int i = 0; i < n; i++) {
			expected[i] = (${prim.Name})((i + 1) * 10);
			p.set${prim.CapName}(i * ${prim.Size}, expected[i]);
		}
		
		for (int i = 0; i < n; i++)
			assertEquals(expected[i], p.get${prim.CapName}(i * ${prim.Size}), 0);
	}

	@Test 
    public void testPointerTo_${prim.Name}_Values() {
		Pointer<${prim.WrapperName}> p = Pointer.pointerTo${prim.CapName}s((${prim.Name})1, (${prim.Name})2, (${prim.Name})3);
		assertEquals((${prim.Name})1, (${prim.Name})p.get(0), 0);
		assertEquals((${prim.Name})2, (${prim.Name})p.get(1), 0);
		assertEquals((${prim.Name})3, (${prim.Name})p.get(2), 0);
	}
	
	@Test 
    public void testPointerTo_${prim.Name}_Values2D() {
		${prim.Name}[][] values = new ${prim.Name}[][] {
				{(${prim.Name})1, (${prim.Name})2},
				{(${prim.Name})10, (${prim.Name})20},
				{(${prim.Name})100, (${prim.Name})200}
		};
		Pointer<Pointer<${prim.WrapperName}>> p = Pointer.pointerTo${prim.CapName}s(values);
		int dim2 = values[0].length;
		for (int i = 0; i < values.length; i++)
			for (int j = 0; j < dim2; j++)
				assertEquals(values[i][j], p.get(i).get(j), 0);
	}
	
	@Test 
    public void testAllocateBounds_${prim.Name}_ok() {
		assertEquals((double)(${prim.Name})0, (double)Pointer.allocate${prim.CapName}().get(0), 0);
		assertEquals((double)(${prim.Name})0, (double)Pointer.allocate${prim.CapName}s(1).get(0), 0);
		assertEquals((double)(${prim.Name})0, (double)Pointer.allocate${prim.CapName}s(2).offset(${prim.Size}).get(-1), 0);
		
		//TODO slide, slideBytes
	}
	@Test 
    public void testAllocateRemaining_${prim.Name}_ok() {
    	Pointer<${prim.WrapperName}> p = Pointer.allocate${prim.CapName}s(2);
    	assertEquals(2, p.getRemainingElements());
		assertEquals(2 * ${prim.Size}, p.getRemainingBytes());
		
		Pointer<${prim.WrapperName}> n = p.next();
		Pointer<${prim.WrapperName}> o = p.offset(${prim.Size});
		assertEquals(n, o);
		
		assertEquals(1, n.getRemainingElements());
		assertEquals(${prim.Size}, n.getRemainingBytes());
		assertEquals(1, o.getRemainingElements());
		assertEquals(${prim.Size}, o.getRemainingBytes());
		
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
	#if (($prim.Name == "short") || ($prim.Name == "int") || ($prim.Name == "long"))
	@Test
	public void test${prim.CapName}Endianness() {
		for (${prim.Name} value : new ${prim.Name}[] { (${prim.Name})0, (${prim.Name})1, (${prim.Name})-1 }) {
			test${prim.CapName}Endianness(ByteOrder.LITTLE_ENDIAN, value);
			test${prim.CapName}Endianness(ByteOrder.BIG_ENDIAN, value);
		}
	}
	void test${prim.CapName}Endianness(ByteOrder order, ${prim.Name} value) {
		Pointer<${prim.WrapperName}> p = Pointer.allocate${prim.CapName}().order(order);
		p.set(value);
        assertEquals(order, p.order());
        assertEquals(order, p.get${prim.BufferName}(0, 1).order());
		assertEquals(value, p.get${prim.BufferName}(0, 1).get(), 0); // check that the NIO buffer was created with the correct order by default
		assertEquals(value, p.getByteBuffer(0, ${prim.Size}).order(order).as${prim.BufferName}().get(), 0);
	}
	#end

#end
}
