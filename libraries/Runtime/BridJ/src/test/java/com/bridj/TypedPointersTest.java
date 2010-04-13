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
		Pointer<MyStruct> ps = Pointer.getPeer(s);
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
}

