package com.bridj;

import com.bridj.ann.*;
import static com.bridj.Functional.*;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

@Library("test")
@com.bridj.ann.Runtime(CRuntime.class)
public class CallTest {

	public CallTest() {
		BridJ.register(CallTest.class);
	}
	
#foreach ($prim in $primitivesNoBool)

	/// Returns value + 1
	public static native ${prim.Name} test_incr_${prim.Name}(${prim.Name} value);

	@Test
	public void testIncrement_${prim.Name}() {
		for (${prim.Name} value : new ${prim.Name}[] { (${prim.Name})0, (${prim.Name})1, (${prim.Name})-1 }) {
			${prim.Name} ret = test_incr_${prim.Name}(value);
			${prim.Name} incr = (${prim.Name})(value + 1);
			assertEquals(incr, ret#if($prim.Name == "float" || $prim.Name == "double"), 0#end);
		}
	}
	
	/*
	/// Returns cb.apply(value)
	public static native ${prim.Name} test_callback_${prim.Name}_${prim.Name}(Func1<${prim.WrapperName}, ${prim.WrapperName}> cb, ${prim.Name} value);
	
	@Test
	public void testCallback_${prim.Name}() {
		Func1<${prim.WrapperName}, ${prim.WrapperName}> cb = new Func1<${prim.WrapperName}, ${prim.WrapperName}>() {
			public ${prim.WrapperName} apply(${prim.WrapperName} value) {
				return (${prim.Name})(value + 1);
			}
		};
		for (${prim.Name} value : new ${prim.Name}[] { (${prim.Name})0, (${prim.Name})1, (${prim.Name})-1 }) {
			${prim.Name} ret = test_callback_${prim.Name}_${prim.Name}(cb, value);
			${prim.Name} incr = (${prim.Name})(value + 1);
			assertEquals(incr, ret#if($prim.Name == "float" || $prim.Name == "double"), 0#end);
		}
	}
	*/

#end
}
