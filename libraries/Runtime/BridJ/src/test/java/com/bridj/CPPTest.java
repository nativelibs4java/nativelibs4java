package com.bridj;

import com.bridj.Dyncall.CallingConvention;
import java.io.FileNotFoundException;

import java.util.Collection;

import org.junit.Test;
import static org.junit.Assert.*;

import com.bridj.ann.Constructor;
import com.bridj.ann.Convention;
import com.bridj.ann.Field;
import com.bridj.ann.Library;
import com.bridj.ann.Symbol;
import com.bridj.ann.Ptr;
import com.bridj.ann.Virtual;
import com.bridj.cpp.CPPObject;


import com.bridj.BridJ;
import com.bridj.Pointer;
import com.bridj.ann.Field;
import com.bridj.ann.Library;
import com.bridj.ann.Name;
import com.bridj.ann.Runtime;
import com.bridj.ann.Virtual;
import com.bridj.cpp.CPPRuntime;

import org.junit.After;
import org.junit.Before;
import static com.bridj.Pointer.*;

///http://www.codesourcery.com/public/cxx-abi/cxx-vtable-ex.html
@Library("test")
@Runtime(CPPRuntime.class)
public class CPPTest {
	
	@Test
	public void testSize() {
		assertEquals("Invalid size for class Ctest", sizeOfCtest(), BridJ.sizeOf(new Ctest()));
		assertEquals("Invalid size for class Ctest2", sizeOfCtest2(), BridJ.sizeOf(new Ctest2()));
		assertTrue("sizeOfCtest() = " + sizeOfCtest(), sizeOfCtest() >= 12 && sizeOfCtest() <= 20);
		assertTrue("sizeOfCtest2() = " + sizeOfCtest2(), sizeOfCtest2() >= 16 && sizeOfCtest() <= 30);
	}
	
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
		
		//TestCPP.print(instance.getClass().getSimpleName(), pointerTo(instance).getPeer(), 10, 2);
		//TestCPP.print(instance.getClass().getSimpleName() + "'s vtable", pointerTo(instance).getSizeT(0), 10, 2);
                
		int c = instance.testVirtualAdd(a, b);
		assertEquals(res, c);

        c = Pointer.getPointer(instance).toNativeObject(Ctest.class).testAdd(a, b);
        assertEquals(baseRes, c);

        if (JNI.isWindows()) {
	        c = instance.testVirtualAddStdCall(null, a, b);
			assertEquals("testVirtualAddStdCall", baseRes, c);
	
	        c = instance.testVirtualAddStdCall(Pointer.allocateInt(), a, b);
			assertEquals("testVirtualAddStdCall", 0, c);
	
	        c = instance.testAddStdCall(null, a, b);
			assertEquals("testAddStdCall", baseRes, c);
	
	        c = instance.testAddStdCall(Pointer.allocateInt(), a, b);
			assertEquals("testAddStdCall", 0, c);
        }
	}
	
	static {
		BridJ.register();
	}
	@Ptr public static native long sizeOfCtest();
	@Ptr public static native long sizeOfCtest2();
	
	//@Library("test")
	public static class Ctest extends CPPObject {
		public Ctest() {
			super();
		}
		public Ctest(Pointer pointer) {
			super(pointer);
		}
		
		@Field(0) 
		public int firstField() {
			return this.io.getIntField(this, 0);
		}
		@Field(0) 
		public Ctest firstField(int firstField) {
			this.io.setIntField(this, 0, firstField);
			return this;
		}
		public final int firstField_$eq(int firstField) {
			firstField(firstField);
			return firstField;
		}
		@Field(1) 
		public int secondField() {
			return this.io.getIntField(this, 1);
		}
		@Field(1) 
		public Ctest secondField(int secondField) {
			this.io.setIntField(this, 1, secondField);
			return this;
		}
		public final int secondField_$eq(int secondField) {
			secondField(secondField);
			return secondField;
		}
		@Name("~Ctest") 
		public native void CtestDestructor();
		@Virtual(0) 
		public native int testVirtualAdd(int a, int b);
		public native int testAdd(int a, int b);
		@Virtual(1) 
		@Convention(Convention.Style.StdCall)
		public native int testVirtualAddStdCall(Pointer<? > ptr, int a, int b);
		@Convention(Convention.Style.StdCall)
		public native int testAddStdCall(Pointer<? > ptr, int a, int b);
		public native static void static_void();
	};
	/// <i>native declaration : line 18</i>
	public static class Ctest2 extends Ctest {
		public Ctest2() {
			super();
		}
		public Ctest2(Pointer pointer) {
			super(pointer);
		}
		
		/// C type : int*
		@Field(0) 
		public Pointer<java.lang.Integer > fState() {
			return this.io.getPointerField(this, 0, java.lang.Integer.class);
		}
		/// C type : int*
		@Field(0) 
		public Ctest2 fState(Pointer<java.lang.Integer > fState) {
			this.io.setPointerField(this, 0, fState);
			return this;
		}
		/// C type : int*
		public final Pointer<java.lang.Integer > fState_$eq(Pointer<java.lang.Integer > fState) {
			fState(fState);
			return fState;
		}
		@Field(1) 
		public int fDestructedState() {
			return this.io.getIntField(this, 1);
		}
		@Field(1) 
		public Ctest2 fDestructedState(int fDestructedState) {
			this.io.setIntField(this, 1, fDestructedState);
			return this;
		}
		public final int fDestructedState_$eq(int fDestructedState) {
			fDestructedState(fDestructedState);
			return fDestructedState;
		}
		public native void setState(Pointer<java.lang.Integer > pState);
		public native void setDestructedState(int destructedState);
		@Virtual(0) 
		public native int testVirtualAdd(int a, int b);
		public native int testAdd(int a, int b);
	};
	
    @Test
    public void testDestruction() throws InterruptedException {
        Pointer<Integer> pState = allocateInt();
        Ctest2 t = new Ctest2();
        t.setState(pState);
        int destructedState = 10;
        t.setDestructedState(destructedState);
        t = null;
        GC();
        assertEquals(destructedState, pState.getInt());
    }
    
    @After
    public void GC() throws InterruptedException {
        System.gc();
        Thread.sleep(200);
    }
}

