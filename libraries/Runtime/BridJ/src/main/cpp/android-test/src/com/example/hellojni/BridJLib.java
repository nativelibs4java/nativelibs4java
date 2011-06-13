package com.example.hellojni;

import org.bridj.*;
import org.bridj.ann.*;

@Library("hello-jni")
public class BridJLib {
	static {
		BridJ.register();
	}
	public static native int addTwoInts(int a, int b);
}
