package org.bridj;
import static org.bridj.Pointer.*;
import org.bridj.ann.*;
//import static org.bridj.Functional.*;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

@Library("test")
@org.bridj.ann.Runtime(CRuntime.class)
public class CallTest {

	public CallTest() {
		BridJ.register(CallTest.class);
	}
	
#foreach ($prim in $primitivesNoBool)

	/// Returns value + 1
	public static native ${prim.Name} test_incr_${prim.Name}(${prim.Name} value);

	/// Returns value + 1 in pointed value
	public static native void test_incr_${prim.Name}_out(${prim.Name} value, Pointer<${prim.WrapperName}> out);

	@Test
	public void testIncrement${prim.CapName}() {
		Pointer<${prim.WrapperName}> out = allocate${prim.CapName}();
		for (${prim.Name} value : new ${prim.Name}[] { (${prim.Name})0, (${prim.Name})1, (${prim.Name})-1 }) {
			${prim.Name} ret = test_incr_${prim.Name}(value);
			${prim.Name} incr = (${prim.Name})(value + 1);
			assertEquals(incr, ret#if($prim.Name == "float" || $prim.Name == "double"), 0#end);
			
			test_incr_${prim.Name}_out(value, out);
			ret = out.get();
			assertEquals(incr, ret#if($prim.Name == "float" || $prim.Name == "double"), 0#end);
		}
	}
	
	public static abstract class MyCallback_${prim.Name} extends Callback {
		public abstract ${prim.Name} apply(${prim.Name} value);
	}
	///*
	/// Returns cb.apply(value)
	//public static native ${prim.Name} test_callback_${prim.Name}_${prim.Name}(Func1<${prim.WrapperName}, ${prim.WrapperName}> cb, ${prim.Name} value);
	public static native ${prim.Name} test_callback_${prim.Name}_${prim.Name}(Pointer<MyCallback_${prim.Name}> cb, ${prim.Name} value);
	
	@Test
	public void testCallback_${prim.Name}() {
		MyCallback_${prim.Name} cb = new MyCallback_${prim.Name}() {
			public ${prim.Name} apply(${prim.Name} value) {
				return (${prim.Name})(value + 1);
			}
		};
		/*
		Func1<${prim.WrapperName}, ${prim.WrapperName}> cb = new Func1<${prim.WrapperName}, ${prim.WrapperName}>() {
			public ${prim.Name} apply(${prim.Name} value) {
				return (${prim.Name})(value + 1);
			}
		};*/
		for (${prim.Name} value : new ${prim.Name}[] { (${prim.Name})0, (${prim.Name})1, (${prim.Name})-1 }) {
			${prim.Name} ret = test_callback_${prim.Name}_${prim.Name}(cb.toPointer(), value);
			${prim.Name} incr = (${prim.Name})(value + 1);
			assertEquals(incr, ret#if($prim.Name == "float" || $prim.Name == "double"), 0#end);
		}
	}
	//*/

	
#foreach ($n in [9..9])
	public static native ${prim.Name} test_add${n}_${prim.Name}(#foreach ($i in [1..$n])#if($i > 1), #end${prim.Name} arg$i#end);
	public static native void test_add${n}_${prim.Name}_out(#foreach ($i in [1..$n])#if($i > 1), #end${prim.Name} arg$i#end, Pointer<${prim.WrapperName}> out);
	
	@Test
	public void testAdd${n}${prim.CapName}() {
		Pointer<${prim.WrapperName}> out = allocate${prim.CapName}();
		
		${prim.Name} expectedTot = (${prim.Name})0;
		${prim.Name} fact = (${prim.Name})1;
#foreach ($i in [1..$n])
		${prim.Name} arg$i = (${prim.Name})(fact * ($i + 1));
		fact *= (${prim.Name})2;
		expectedTot += arg$i;
#end
		${prim.Name} tot = test_add${n}_${prim.Name}(#foreach ($i in [1..$n])#if($i > 1),#end arg$i#end);
		assertEquals(expectedTot, tot#if($prim.Name == "float" || $prim.Name == "double"), 0#end);
		
		test_add${n}_${prim.Name}_out(#foreach ($i in [1..$n])#if($i > 1),#end arg$i#end, out);
		tot = out.get();
		assertEquals(expectedTot, tot#if($prim.Name == "float" || $prim.Name == "double"), 0#end);
	}
#end


#end
}
