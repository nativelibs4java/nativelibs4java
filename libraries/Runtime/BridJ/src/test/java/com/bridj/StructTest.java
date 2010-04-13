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
}

