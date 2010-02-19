package com.bridj;

import java.io.FileNotFoundException;
import java.util.Collection;

import org.junit.Test;
import static org.junit.Assert.*;

import com.bridj.ann.Constructor;
import com.bridj.ann.Library;
import com.bridj.ann.Mangling;
import com.bridj.ann.Ptr;
import com.bridj.ann.This;
import com.bridj.ann.Virtual;

///http://www.codesourcery.com/public/cxx-abi/cxx-vtable-ex.html
public class CPPTest {
	
	@Test
	public void test_Ctest_testAdd() {
		testAdd(new Ctest(), 1, 2, 3);
	}
	@Test
	public void test_Ctest2_testAdd() {
		testAdd(new Ctest2(), 1, 2, 5);
	}
	
	void testAdd(Ctest instance, int a, int b, int res) {
		//long peer = Pointer.getAddress(test, Ctest.class);
		int c = instance.testAdd(a, b);
		assertEquals(res, c);
	}
	
	@Library("test")
	static class Ctest extends CPPObject {
		static {
			BridJ.register(Ctest.class);
		}
		//@Mangling("_Z10createTestv")
		static native @Ptr long createTest();
		
		
		@Constructor
		//@Mangling("__ZN5CtestC1Ev")
		private static native void Ctest(@This long thisPtr);
		

//		public Ctest() {
//			super(Pointer.pointerToAddress(createTest(), Ctest.class));
//		}
//		public Ctest(Pointer<? extends Ctest> peer) {
//			super(peer);
//		}
		
		@Virtual
		public native int testAdd(int a, int b);
//		protected static native int testAdd(@This long thisPtr, int a, int b);
//		public int testAdd(int a, int b) {
//			//print("this", $this.getPeer(), 10, 10);
//			//print("*this", $this.getPointer(0).getPeer(), 10, 10);
//			return testAdd(Pointer.getAddress(this, getClass()), a, b);
//		}
	}
	static class Ctest2 extends Ctest {
		@Constructor
		private static native void Ctest2(@This long thisPtr);
	}
	
}

