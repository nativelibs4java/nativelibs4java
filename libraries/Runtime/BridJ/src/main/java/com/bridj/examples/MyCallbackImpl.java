package com.bridj.examples;

import com.bridj.Pointer;
import com.bridj.CRuntime;

@com.bridj.ann.Runtime(CRuntime.class)
public class MyCallbackImpl extends MyCallback {
	public native long doSomething(int a, int b);
}
