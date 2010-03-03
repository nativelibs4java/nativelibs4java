package com.bridj;

import org.junit.Test;
import static org.junit.Assert.*;

import com.bridj.ann.Field;

///http://www.codesourcery.com/public/cxx-abi/cxx-vtable-ex.html
public class StructTest {
	
	public static class MyStruct extends StructObject {
		@Field(0)
		public native int a();
		@Field(0)
		public native void a(int a);
	}
	@Test
	public void trivial() {
		MyStruct s = new MyStruct();
		s.a(10);
		int a = s.a();
		assertEquals(10, a);
	}	
}

