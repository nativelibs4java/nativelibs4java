package com.bridj;

import org.junit.Test;
import static org.junit.Assert.*;

import com.bridj.ann.Library;

@Library("test")
@com.bridj.ann.Runtime(CRuntime.class)
public class CallbackTest {
	static {
		BridJ.register();
	}
	
	@Test
	public void testJavaTargetCallbacks() {
		assertEquals(3, forwardCall(new MyCallback() {
			@Override
			public int doSomething(int a, int b) {
				return a + b;
			}
		}.toPointer(), 1, 2));
		
		assertEquals(21, forwardCall(new MyCallback() {
			@Override
			public int doSomething(int a, int b) {
				return a + b * 10;
			}
		}.toPointer(), 1, 2));
	}
	
	@Test
	public void testNativeTargetCallbacks() {
		MyCallback adder = getAdder().toNativeObject(MyCallback.class);
		assertEquals(3, adder.doSomething(1, 2));
	}
	
	static native int forwardCall(Pointer<MyCallback> cb, int a, int b);
	static native Pointer<MyCallback> getAdder();
	
	public static abstract class MyCallback extends Callback {
		public abstract int doSomething(int a, int b); 
	}
	
	
}
