package com.bridj;

import java.io.FileNotFoundException;
import java.util.Collection;

import org.junit.Test;
import static org.junit.Assert.*;

import com.bridj.ann.Library;
import com.bridj.ann.Mangling;
import com.bridj.ann.PointerSized;
import com.bridj.ann.This;
import com.bridj.ann.Virtual;

///http://www.codesourcery.com/public/cxx-abi/cxx-vtable-ex.html
public class CPPTest {
	
	@Test
	public void testAdd() {
		
		Ctest test = new Ctest();
		long peer = BridJ.getPeer(test, getClass());
		int res = test.testAdd(1, 2);
		assertEquals(3, res);
	}
	@Library("test")
	static class Ctest extends CPPObject {
		static {
			BridJ.register(Ctest.class);
		}
		@Mangling("_Z10createTestv")
		static native @PointerSized long createTest();
		

		public Ctest() {
			super(Pointer.pointerToAddress(createTest()));
		}
		public Ctest(Pointer<?> peer) {
			super(peer);
		}
		@Virtual
		protected static native int testAdd(@This long thisPtr, int a, int b);
		public int testAdd(int a, int b) {
			//print("this", $this.getPeer(), 10, 10);
			//print("*this", $this.getPointer(0).getPeer(), 10, 10);
			return testAdd($this.getPeer(), a, b);
		}
	}
	
}

