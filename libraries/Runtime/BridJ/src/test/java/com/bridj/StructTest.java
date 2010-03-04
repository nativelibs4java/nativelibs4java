package com.bridj;

import org.junit.Test;
import static org.junit.Assert.*;

import com.bridj.ann.Field;

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
}

