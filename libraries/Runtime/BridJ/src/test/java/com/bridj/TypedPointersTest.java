package com.bridj;

import java.io.FileNotFoundException;

import java.util.Collection;

import org.junit.Test;
import static org.junit.Assert.*;

import com.bridj.ann.*;
import com.bridj.*;

///http://www.codesourcery.com/public/cxx-abi/cxx-vtable-ex.html
public class TypedPointersTest {
	
	public static class MyPtr extends TypedPointer {
		public MyPtr(long peer) {
			super(peer);
		}
		public MyPtr(Pointer peer) {
			super(peer);
		}
	}
	
	@Library("test")
	public static class MyStruct extends StructObject {
		@Field(0)
		//public native Pointer<Integer> a();
		//public native MyStruct a(MyPtr a);
        public native MyPtr a();
        public native void a(MyPtr a);
        //public native void a(MyPtr a);
	}
	
	@Test
	public void test_Ctest_testAdd() {
		Pointer<MyPtr> ptrs = Pointer.allocateTypedPointers(MyPtr.class, 10);
		ptrs.setSizeT(0, 10);
		MyPtr ptr = ptrs.get();
		assertTrue(ptr instanceof MyPtr);
		assertEquals(10, ptr.getPeer());
	}
	
	@Test
	public void testStructTypedPtrField() {
		MyStruct s = new MyStruct();
		Pointer<MyStruct> ps = Pointer.getPointer(s);
		ps.setSizeT(0, 10);
		MyPtr ptr = s.a();
		assertTrue(ptr instanceof MyPtr);
		assertEquals(10, ptr.getPeer());
	}
	
	@Test
	public void testStringPointer() {
		assertNull(Pointer.pointerToCString(null));
		Pointer<Byte> p = Pointer.pointerToCString("test");
		assertEquals("test", p.getCString(0));
	}
	
	@Test
	public void testStringsPointer() {
		assertNull(Pointer.pointerToCStrings((String[])null));
		
		Pointer<Pointer<Byte>> p = Pointer.pointerToCStrings(null, null);
		assertNull(p.get(0));
		assertNull(p.get(1));
		
		p = Pointer.pointerToCStrings("test1", "test2");
		assertEquals("test1", p.get(0).getCString(0));
		assertEquals("test2", p.get(1).getCString(0));
	}
	
	@Test
	public void testEquals() {
		Pointer m1 = Pointer.allocateBytes(2), m2 = Pointer.allocateBytes(2);
		assertNotNull(m1);
		assertNotNull(m2);
		assertTrue(!m1.equals(m2));
		
		long addr1 = m1.getPeer(), addr2 = m2.getPeer();
		Pointer[] ps1 = new Pointer[] {
			m1,
			new MyPtr(addr1),
			Pointer.pointerToAddress(addr1)
		};
		Pointer[] ps2 = new Pointer[] {
			m2,
			new MyPtr(addr2),
			Pointer.pointerToAddress(addr2)
		};
		for (Pointer p1 : ps1) {
			assertNotNull(p1);
			assertEquals(m1, p1);
			assertEquals(p1, m1);	
		}
		for (Pointer p2 : ps2) {
			assertNotNull(p2);
			assertEquals(m2, p2);
			assertEquals(p2, m2);	
		}
		
		for (Pointer p1 : ps1) {
			for (Pointer p2 : ps2) {
				assertTrue(!p1.equals(p2));
			}	
		}
	}
}

