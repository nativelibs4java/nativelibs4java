package org.bridj;

import java.util.ArrayList;
import java.lang.reflect.Method;
import org.bridj.Dyncall.CallingConvention;
import java.io.FileNotFoundException;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;

import org.bridj.ann.Constructor;
import org.bridj.ann.Convention;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
import org.bridj.ann.Symbol;
import org.bridj.ann.Ptr;
import org.bridj.ann.Virtual;
import org.bridj.cpp.CPPObject;


import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
import org.bridj.ann.Name;
import org.bridj.ann.Runtime;
import org.bridj.ann.Virtual;
import org.bridj.cpp.CPPRuntime;

import org.junit.After;
import org.junit.Before;
import static org.bridj.Pointer.*;

///http://www.codesourcery.com/public/cxx-abi/cxx-vtable-ex.html
@Library("test")
@Runtime(CPPRuntime.class)
public class CPPTest {
    static {
		BridJ.register();
	}
    ///*
	@Test
	public void testSize() {
		assertEquals("Invalid size for class Ctest", sizeOfCtest(), BridJ.sizeOf(Ctest.class));
		assertEquals("Invalid size for class Ctest2", sizeOfCtest2(), BridJ.sizeOf(Ctest2.class));
		assertTrue("sizeOfCtest() = " + sizeOfCtest(), sizeOfCtest() >= 12 && sizeOfCtest() <= 20);
		assertTrue("sizeOfCtest2() = " + sizeOfCtest2(), sizeOfCtest2() >= 16 && sizeOfCtest() <= 30);
	}

	@Test
	public void test_Ctest_constructors() {
        Ctest ct =  new Ctest();
        Pointer<Ctest> p = pointerTo(ct);
        assertEquals(-123456, ct.firstField());
		assertEquals(-33, new Ctest(-33).firstField());
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
    //*/
	///*
	void testAdd(Ctest instance, int a, int b, int res, int baseRes) {
		//long peer = Pointer.getAddress(test, Ctest.class);
		
		//TestCPP.print(instance.getClass().getSimpleName(), pointerTo(instance).getPeer(), 10, 2);
		//TestCPP.print(instance.getClass().getSimpleName() + "'s vtable", pointerTo(instance).getSizeT(), 10, 2);
                
		int c = instance.testVirtualAdd(a, b);
		assertEquals(res, c);

        c = Pointer.pointerTo(instance).getNativeObject(Ctest.class).testAdd(a, b);
        assertEquals(baseRes, c);

        if (Platform.isWindows()) {
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
    //*/
    public static native int testIndirectVirtualAdd(Pointer<Ctest> pTest, int a, int b);
    
    @Test
    public void testJavaVirtualOverride() {
        Ctest test = new Ctest();
        assertEquals(3, testIndirectVirtualAdd(pointerTo(test), 1, 2));

        test = new Ctest(10) {
            @Override
            public int testVirtualAdd(int a, int b) {
                return a * 10 + b * 100;//super.testVirtualAdd(a, b) * 2;
            }
        };
        /*
        List<Method> virtualMethods = new ArrayList<Method>();
        CPPRuntime.getInstance().listVirtualMethods(test.getClass(), virtualMethods);
        System.out.println("virtualMethods = " + virtualMethods);
        //*/
        int a = 1, b = 2;
        int ind = testIndirectVirtualAdd(pointerTo(test), a, b);
        assertEquals(a * 10 + b * 100, ind);
    }
	
	@Ptr public static native long sizeOfCtest();
	@Ptr public static native long sizeOfCtest2();
	
	//@Library("test")
	public static class Ctest extends CPPObject {
        @Constructor(0)
		public Ctest() {
			super((Void)null, 0);
		}
		@Constructor(1)
		public Ctest(int firstField) {
			super((Void)null, 1, firstField);
		}
		public Ctest(Pointer pointer) {
			super(pointer);
		}
		public Ctest(Void voidArg, int constructorId, Object... args) {
			super(voidArg, constructorId, args);
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
        @Constructor(0)
		public Ctest2() {
			super((Void)null, 0);
		}
		public Ctest2(Pointer pointer) {
			super(pointer);
		}
		
		/// C type : int*
		@Field(0) 
		public Pointer<java.lang.Integer > fState() {
			return this.io.getPointerField(this, 0);
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

    @After
    public void GC() throws InterruptedException {
        System.gc();
        Thread.sleep(200);
    }
}

