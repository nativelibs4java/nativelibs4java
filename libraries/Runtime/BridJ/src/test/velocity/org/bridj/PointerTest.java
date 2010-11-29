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

#foreach ($prim in $bridJPrimitives)

	@Test
	public void testAllocateArrayPrim_${prim.Name}() {
		for (int type = 0; type < 4; type++) {
			Pointer ptr = null;
			switch (type) {
			case 0:
				ptr = Pointer.allocate${prim.CapName}();
				break;
			case 1:
				ptr = Pointer.allocate${prim.CapName}s(2);
				break;
			case 2: 
				ptr = Pointer.allocateArray(${prim.Name}.class, 2);
				break;
			case 3: 
				ptr = Pointer.allocateArray(${prim.WrapperName}.class, 2);
				break;
			};
			assertTrue("approach " + type + " failed", ptr.getIO() == (PointerIO)PointerIO.get${prim.CapName}Instance());
			assertTrue("approach " + type + " failed", ptr.getIO() == (PointerIO)PointerIO.getInstance(${prim.Name}.class));
		}
	}
#end
	
	Pointer<?> someUntypedPtr() {
		Pointer<Integer> p = allocateInt();
		return pointerToAddress(p.getPeer());
	}
	@Test(expected=RuntimeException.class)
	public void testUntypedSize() {
		someUntypedPtr().getTargetSize();
	}
	@Test(expected=RuntimeException.class)
	public void testUntypedNext() {
		someUntypedPtr().next();
	}
	@Test(expected=RuntimeException.class)
	public void testUntypedGetArray() {
		someUntypedPtr().getArray();
	}
	@Test(expected=RuntimeException.class)
	public void testUntypedGetBuffer() {
		someUntypedPtr().getBuffer();
	}
	
	@Test
	public void testPointerToNulls() {
		assertEquals(0, getPeer(null));
		assertEquals(null, allocateBytes(null, 0, null));
		assertEquals(null, allocateArray(int.class, 0));
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

#if ($prim.Name == "SizeT" || $prim.Name == "CLong")
#set ($rawType = "long")
#set ($rawCapName = "Long")
#else
#set ($rawType = $prim.Name)
#set ($rawCapName = $prim.CapName)
#end

	static ${prim.Name}[] createExpected${prim.CapName}s(int n) {
		${prim.Name}[] expected = new ${prim.Name}[n];
		expected[0] = ${prim.value("1")};
		expected[1] = ${prim.value("2")};
		expected[2] = ${prim.value("3")};
		//for (int i = 0; i < n; i++)
		//	expected[i] = (${prim.Name})(i + 1);
		return expected;
	}
	
	@Test 
    public void test${prim.CapName}sIterator() {
		${prim.Name}[] expected = createExpected${prim.CapName}s(n);
		Pointer<${prim.typeRef}> p = Pointer.pointerTo${prim.CapName}s(expected);
		long peer = p.getPeer();
		
		Iterator<${prim.typeRef}> it = p.iterator();
		for (int i = 0; i < n; i++) {
			assertTrue(it.hasNext());
			${prim.typeRef} obVal = it.next();
			assertNotNull(obVal);
			${prim.Name} val = obVal;
			assertEquals("at position i = " + i, (Object)expected[i], (Object)val);
		}
		assertTrue(!it.hasNext());
	}
	
	
	@Test 
    public void testPointerTo_${prim.Name}_Values() {
		// Test pointerToInts(int...)
		Pointer<${prim.typeRef}> p = Pointer.pointerTo${prim.CapName}s(${prim.value("1")}, ${prim.value("2")}, ${prim.value("3")});
		assertEquals(${prim.value("1")}, (${prim.Name})p.get(0)$precisionArg);
		assertEquals(${prim.value("2")}, (${prim.Name})p.get(1)$precisionArg);
		assertEquals(${prim.value("3")}, (${prim.Name})p.get(2)$precisionArg);
		
		p = Pointer.pointerTo${prim.CapName}s(${prim.rawValue("1")}, ${prim.rawValue("2")}, ${prim.rawValue("3")});
		assertEquals(${prim.rawValue("1")}, p.get${prim.CapName}(0)$precisionArg);
		assertEquals(${prim.rawValue("2")}, p.get${prim.CapName}(1)$precisionArg);
		assertEquals(${prim.rawValue("3")}, p.get${prim.CapName}(2)$precisionArg);
	}
	@Test 
    public void testPointerTo_${prim.Name}_Value() {
		Pointer<${prim.typeRef}> p = Pointer.pointerTo${prim.CapName}(${prim.value("1")});
		assertEquals(${prim.value("1")}, (${prim.Name})p.get(0)$precisionArg);
		
		p = Pointer.pointerTo${prim.CapName}(${prim.rawValue("1")});
		assertEquals(${prim.rawValue("1")}, p.get${prim.CapName}(0)$precisionArg);
	}

	
	@Test 
    public void testGet${prim.CapName}s() {
	
    		for (int type = 0; type < 5; type++) {
			Pointer<${prim.typeRef}> p = Pointer.allocate${prim.CapName}s(n);
			$rawType[] expected = createExpected${rawCapName}s(n);
			$rawType[] values = null;
			
			switch (type) {
			case 0:
				p.set${prim.CapName}s(expected);
				values = p.get${prim.CapName}s();
				break;
			case 1:
				p.set${prim.CapName}s(0, expected);
				values = p.get${prim.CapName}s();
				break;
			case 2:
				p.set${prim.CapName}s(0, expected, 0, expected.length);
				values = p.get${prim.CapName}s(0, expected.length);
				break;
			case 3:
				values = new $rawType[n];
				for (int i = 0; i < n; i++) {
#if ($prim.Name == "SizeT" || $prim.Name == "CLong")
					p.set(i, new ${prim.Name}(expected[i]));
					values[i] = p.get(i).longValue(); 
#else
					p.set(i, expected[i]);
					values[i] = p.get(i); 
#end
				}
				break;
			case 4:
				values = new $rawType[n];
				for (int i = 0; i < n; i++) {
					p.set${prim.CapName}(i * ${prim.Size}, expected[i]);
					values[i] = p.get${prim.CapName}(i * ${prim.Size}); 
				}
				break;
			}
			assertNotNull(values);
			assertEquals("approach " + type + " failed", expected.length, values.length);
			for (int i = 0; i < n; i++) {
				assertEquals("approach " + type + " failed", expected[i], values[i]$precisionArg);
			}
		}
	}
	
	@Test 
    public void simpleSetGet${prim.CapName}s_ENDIAN() {
    	for (ByteOrder order : new ByteOrder[] { ByteOrder.LITTLE_ENDIAN, ByteOrder.BIG_ENDIAN }) {
			Pointer<${prim.typeRef}> p = allocate${prim.CapName}s(3).order(order);
			p.set${prim.CapName}(${prim.value("1")});
			assertEquals(${prim.rawValue("1")}, ($rawType)p.get${prim.CapName}()$precisionArg);
			
			p.set${prim.CapName}(${prim.Size}, ${prim.value("-2")});
			assertEquals(${prim.rawValue("-2")}, ($rawType)p.get${prim.CapName}(${prim.Size})$precisionArg);
			
			p.set(2, ${prim.value("3")});
			assertEquals(${prim.value("3")}, (${prim.Name})p.get(2)$precisionArg);
			
			p.set${prim.CapName}s(${prim.Size}, new ${prim.Name}[] { ${prim.value("5")}, ${prim.value("6")} });
			assertEquals(${prim.value("5")}, (${prim.Name})p.get(1)$precisionArg);
			assertEquals(${prim.value("6")}, (${prim.Name})p.get(2)$precisionArg);
			$rawType[] a = p.get${prim.CapName}s(${prim.Size}, 2);
			assertEquals(2, a.length);
			assertEquals(${prim.rawValue("5")}, a[0]$precisionArg);
			assertEquals(${prim.rawValue("6")}, a[1]$precisionArg);
		}
	}
	
	@Test
	public void testPointerToArray_${prim.Name}() {
		${prim.Name}[] original = new ${prim.Name}[] { ${prim.value("1")}, ${prim.value("2")}, ${prim.value("3")} };
		Pointer<${prim.typeRef}> p = pointerToArray(original);
		assertEquals(3, p.getValidElements());
		assertEquals(${prim.value("1")}, (${prim.Name})p.get(0)$precisionArg);
		assertEquals(${prim.value("2")}, (${prim.Name})p.get(1)$precisionArg);
		assertEquals(${prim.value("3")}, (${prim.Name})p.get(2)$precisionArg);
		
		${prim.Name}[] values = (${prim.Name}[])p.getArray();
		assertEquals(original.length, values.length);
		for (int i = 0; i < original.length; i++)
			assertEquals(original[i], values[i]$precisionArg);
	}
	
#end

#foreach ($prim in $primitivesNoBool)	

#if ($prim.Name == "double" || $prim.Name == "float")
#set ($precisionArg = ", 0")
#else
#set ($precisionArg = "")
#end
	
	@Test 
    public void testAllocateBounds_${prim.Name}_ok() {
		assertEquals(${prim.value("0")}, (${prim.Name})Pointer.allocate${prim.CapName}().get(0)$precisionArg);
		assertEquals(${prim.value("0")}, (${prim.Name})Pointer.allocate${prim.CapName}s(1).get(0)$precisionArg);
		assertEquals(${prim.value("0")}, (${prim.Name})Pointer.allocate${prim.CapName}s(2).offset(${prim.Size}).get(-1)$precisionArg);
		
		//TODO slide, slideBytes
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testPointerTo_${prim.Name}_IndirectBuffer() {
    		pointerTo${prim.CapName}s(${prim.BufferName}.wrap(new ${prim.Name}[3]));
	}
	
	@Test 
    public void testGet${prim.BufferName}s() {
	
    		for (int type = 0; type < 6; type++) {
			Pointer<${prim.typeRef}> p = Pointer.allocate${prim.CapName}s(n);
			${prim.Name}[] expected = createExpected${prim.CapName}s(n);
			${prim.BufferName} buf = ${prim.BufferName}.wrap(expected);
			${prim.BufferName} values = null;
			
			switch (type) {
			case 0:
				p.setValues(buf);
				values = (${prim.BufferName})p.getBuffer();
				break;
			case 1:
				p.setValues(0, buf);
				values = (${prim.BufferName})p.getBuffer();
				break;
			case 2:
				p.setValues(0, buf, 0, n);
				values = (${prim.BufferName})p.getBuffer(0, n);
				break;
			case 3:
				p.set${prim.CapName}s(buf);
				values = p.get${prim.BufferName}();
				break;
			case 4:
				p.set${prim.CapName}s(0, buf);
				values = p.get${prim.BufferName}();
				break;
			case 5:
				p.set${prim.CapName}s(0, buf, 0, n);
				values = p.get${prim.BufferName}(0, n);
				break;
			}
			assertEquals("approach " + type + " failed", n, values.capacity());
			
			for (int i = 0; i < n; i++) {
				assertEquals("approach " + type + " failed", expected[i], values.get(i)$precisionArg);
			}
		}
	}

	@Test 
    public void testPointerTo_${prim.Name}_DirectBuffer() {
    		Pointer<${prim.typeRef}> p = Pointer.allocate${prim.CapName}s(3);
    		assertEquals(3 * ${prim.Size}, p.getValidBytes());
		p.set(0, ${prim.value("1")});
		p.set(1, ${prim.value("2")});
		p.set(2, ${prim.value("3")});
		${prim.BufferName} b = p.get${prim.BufferName}();
		assertEquals(3, b.capacity());
		
		for (boolean generic : new boolean[] { false, true }) {
			if (generic)
				p = (Pointer<${prim.typeRef}>)Pointer.pointerToBuffer(b);
			else
				p = Pointer.pointerTo${prim.CapName}s(b);
			
			assertEquals(3 * ${prim.Size}, p.getValidBytes());
			assertEquals(${prim.value("1")}, (${prim.Name})p.get(0)$precisionArg);
			assertEquals(${prim.value("2")}, (${prim.Name})p.get(1)$precisionArg);
			assertEquals(${prim.value("3")}, (${prim.Name})p.get(2)$precisionArg);
		}
	}
	
	@Test 
    public void testPointerTo_${prim.Name}_Values2D() {
		${prim.Name}[][] values = new ${prim.Name}[][] {
				{${prim.value("1")}, ${prim.value("2")}},
				{${prim.value("1")}, ${prim.value("2")}},
				{${prim.value("1")}, ${prim.value("2")}}
		};
		Pointer<Pointer<${prim.typeRef}>> p = Pointer.pointerTo${prim.CapName}s(values);
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
				{${prim.value("1")}, ${prim.value("2")}},
				{${prim.value("1")}, ${prim.value("2")}},
				{${prim.value("1")}, ${prim.value("2")}}
			},
			{
				{${prim.value("1")}, ${prim.value("2")}},
				{${prim.value("1")}, ${prim.value("2")}},
				{${prim.value("1")}, ${prim.value("2")}}
			}
		};
		Pointer<Pointer<Pointer<${prim.typeRef}>>> p = Pointer.pointerTo${prim.CapName}s(values);
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
					// TODO fix 3D allocation !
					//assertEquals(values[i][j][k], (${prim.Name})p.get(i).get(j).get(k)$precisionArg);
				}
			}
		}
				
	}*/
	
	@Test 
    public void testAllocateRemaining_${prim.Name}_ok() {
    	Pointer<${prim.typeRef}> p = Pointer.allocate${prim.CapName}s(2);
    	assertEquals(2, p.getValidElements());
		assertEquals(2 * ${prim.Size}, p.getValidBytes());
		
		Pointer<${prim.typeRef}> n = p.next();
		Pointer<${prim.typeRef}> o = p.offset(${prim.Size});
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
    		Pointer<${prim.typeRef}> p = Pointer.allocate${prim.CapName}().order(order);
    		assertEquals(order, p.order());
    		assertEquals(isOrdered, p.isOrdered());
    	}
    }
    #if (($prim.Name == "short") || ($prim.Name == "int") || ($prim.Name == "long") || ($prim.Name == "double") || ($prim.Name == "float"))
	@Test
	public void test${prim.CapName}Endianness() {
		for (${prim.Name} value : new ${prim.Name}[] { ${prim.value("0")}, ${prim.value("1")}, ${prim.value("-1")} }) {
			test${prim.CapName}Endianness(ByteOrder.LITTLE_ENDIAN, value);
			test${prim.CapName}Endianness(ByteOrder.BIG_ENDIAN, value);
		}
	}
	void test${prim.CapName}Endianness(ByteOrder order, ${prim.Name} value) {
		Pointer<${prim.typeRef}> p = Pointer.allocate${prim.CapName}().order(order);
		p.set(value);
        assertEquals(order, p.order());
        assertEquals(order, p.get${prim.BufferName}(0, 1).order());
		assertEquals(value, (${prim.Name})p.get${prim.BufferName}(0, 1).get()$precisionArg); // check that the NIO buffer was created with the correct order by default
		assertEquals(value, p.getByteBuffer(0, ${prim.Size}).order(order).as${prim.BufferName}().get()$precisionArg);
	}
	#end

#end
}
