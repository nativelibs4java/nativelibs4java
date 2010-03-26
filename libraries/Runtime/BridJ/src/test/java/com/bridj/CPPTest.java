package com.bridj;

import com.bridj.Dyncall.CallingConvention;
import java.io.FileNotFoundException;

import java.util.Collection;

import org.junit.Test;
import static org.junit.Assert.*;

import com.bridj.ann.Constructor;
import com.bridj.ann.Convention;
import com.bridj.ann.Library;
import com.bridj.ann.Symbol;
import com.bridj.ann.Ptr;
import com.bridj.ann.Virtual;
import com.bridj.cpp.CPPObject;

///http://www.codesourcery.com/public/cxx-abi/cxx-vtable-ex.html
public class CPPTest {
	
	@Test
	public void test_Ctest_testAdd() {
		testAdd(new Ctest(), 1, 2, 3, 3);
//		testAdd(Ctest.createTest().get(), 1, 2, 3);
	}
	@Test
	public void test_Ctest_testAddStdCall() {
        Ctest instance = new Ctest();
		testAdd(instance, 1, 2, 3, 3);
//		testAdd(Ctest.createTest().get(), 1, 2, 3);
	}
	@Test
	public void test_Ctest2_testAdd() {
		testAdd(new Ctest2(), 1, 2, 5, 3);
	}
	
	void testAdd(Ctest instance, int a, int b, int res, int baseRes) {
		//long peer = Pointer.getAddress(test, Ctest.class);
		int c = instance.testVirtualAdd(a, b);
		assertEquals(res, c);

        c = Pointer.getPeer(instance).toNativeObject(Ctest.class).testAdd(a, b);
        assertEquals(baseRes, c);

        c = instance.testVirtualAddStdCall(null, a, b);
		assertEquals("testVirtualAddStdCall", baseRes, c);

        c = instance.testVirtualAddStdCall(Pointer.allocateInt(), a, b);
		assertEquals("testVirtualAddStdCall", 0, c);

        c = instance.testAddStdCall(null, a, b);
		assertEquals("testAddStdCall", baseRes, c);

        c = instance.testAddStdCall(Pointer.allocateInt(), a, b);
		assertEquals("testAddStdCall", 0, c);
	}
	
	@Library("test")
	static class Ctest extends CPPObject {
		static { BridJ.register(); }
		
		static native Pointer<Ctest> createTest();
		
		@Virtual
		public native int testVirtualAdd(int a, int b);

		public native int testAdd(int a, int b);

        @Virtual
        @Convention(Convention.Style.StdCall)
		public native int testVirtualAddStdCall(Pointer<?> ptr, int a, int b);

		@Convention(Convention.Style.StdCall)
		public native int testAddStdCall(Pointer<?> ptr, int a, int b);
	}
	static class Ctest2 extends Ctest {

        public native int testAdd(int a, int b);
	}
	
}

