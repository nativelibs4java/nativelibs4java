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
//		testAdd(Ctest.createTest().get(), 1, 2, 3);
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
		static { BridJ.register(); }
		
		static native Pointer<Ctest> createTest();
		
		@Virtual
		public native int testAdd(int a, int b);
	}
	static class Ctest2 extends Ctest {
	}
	
}

