package com.bridj;

import org.junit.Test;
import static org.junit.Assert.*;

import com.bridj.ann.Field;
import static com.bridj.Pointer.*;

///http://www.codesourcery.com/public/cxx-abi/cxx-vtable-ex.html
public class StructTest {
	
	public static class MyStruct extends StructObject {
		@Field(0)
		public native int a();
		public native void a(int a);

        @Field(1)
		public native double b();
		public native void b(double a);
	}
	
	public static class MyJNAStruct extends com.sun.jna.Structure {
		public int a;
		public double b;
	}
	
	@Test
	public void trivial() {
		MyStruct s = new MyStruct();
		s.a(10);
        s.b(100.0);
		int a = s.a();
		double b = s.b();
		assertEquals(10, a);
		assertEquals(100.0, b, 0);
	}

	public static class CastStruct extends StructObject {
        public CastStruct(Pointer p) {
            super(p);
        }
		@Field(0)
		public native int a();
        public native void a(int a);
		//public native CastStruct a(int a);

	}

    @Test
    public void testCast() {
        Pointer<?> m = Pointer.allocateBytes(100);
        CastStruct s = new CastStruct(m);
        s.a(10);
        //assertTrue(s == s.a(10));
        int a = s.a();
        assertEquals(10, a);
    }

    public static class ArrStruct extends StructObject {

		@Field(0)
		public native int a();
		public native ArrStruct a(int a);

	}
    @Test
    public void arrayCast() {
        Pointer<ArrStruct> formats = allocateArray(ArrStruct.class, 10);
        assertEquals(10, formats.getRemainingElements());
        for (ArrStruct s : formats) {
            assertNotNull(s);
            assertEquals(0, s.a());
            assertTrue(s == s.a(10));
        }
    }


    public static class ThisStruct extends StructObject {

		@Field(0)
		public native int a();
		public native ThisStruct a(int a);

	}
    @Test
    public void testThisStruct() {
        ThisStruct s = new ThisStruct();
        ThisStruct o = s.a(10);
        assertTrue(s == o);
        int a = s.a();
        assertEquals(10, a);
    }
    
    @Test
	public void testBridJStructsCreationVsJNAs() {
		System.err.println("#");
		System.err.println("# Warming structs up...");
		System.err.println("#");
		long n = 40000;
		long warmup = 2000;
		for (int i = 0; i < warmup; i++)
			Pointer.pointerTo(new MyStruct());
		
		for (int i = 0; i < warmup; i++)
			new MyJNAStruct().getPointer();
		
		long timeJNA, timeBridJ;
		
		System.err.println("#");
		System.err.println("# Testings BridJ structs...");
		System.err.println("#");
		{
			long start = System.currentTimeMillis();
			for (int i = 0; i < n; i++)
				Pointer.pointerTo(new MyStruct());
			
			timeBridJ = System.currentTimeMillis() - start;
		}
		System.err.println("#");
		System.err.println("# Testings JNA structs...");
		System.err.println("#");
		{
			long start = System.currentTimeMillis();
			for (int i = 0; i < n; i++)
				new MyJNAStruct().getPointer();
			
			timeJNA = System.currentTimeMillis() - start;
		}
		System.err.println("#");
		System.err.println("# BridJ took " + timeBridJ + " ms to create " + n + " simple structs (" + (1000d * timeBridJ / (double)n) + " micro second per struct)");
		System.err.println("# JNA took " + timeJNA + " ms to create " + n + " simple structs (" + (1000d * timeJNA / (double)n) + " micro second per struct)");
		
		double bridJFaster = timeJNA / (double)timeBridJ;
		System.err.println("# Creation of BridJ's structs is " + bridJFaster + " times faster than JNA's");
		System.err.println("#");
		
		assertTrue(bridJFaster > 5);
	}
}

