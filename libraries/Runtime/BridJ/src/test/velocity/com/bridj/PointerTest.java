package com.bridj;

import org.junit.Test;

import com.bridj.DefaultDisorderedPointer;

import java.nio.*;
import java.util.Iterator;
import static org.junit.Assert.*;

public class PointerTest {
	int n = 10;

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
    public void testAllocateBounds_${prim.Name}_ok() {
		assertEquals((double)(${prim.Name})0, (double)Pointer.allocate${prim.CapName}().get(0), 0);
		assertEquals((double)(${prim.Name})0, (double)Pointer.allocate${prim.CapName}s(1).get(0), 0);
		assertEquals((double)(${prim.Name})0, (double)Pointer.allocate${prim.CapName}s(2).offset(${prim.Size}).get(-1), 0);
		
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
    public void test${prim.CapName}DisorderClass() {
    	Class<?> c = Pointer.allocate${prim.CapName}().order(ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN).getClass();
    	assertTrue(DefaultDisorderedPointer.class.isAssignableFrom(c));
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
