package com.bridj;

import org.junit.Test;
import static org.junit.Assert.*;

public class PointerTest {

#foreach ($prim in $primitivesNoBool)
	@Test 
    public static void testPointerTo_${prim.Name}_Values() {
		Pointer<${prim.WrapperName}> p = Pointer.pointerTo(new ${prim.Name}[]{ (${prim.Name})1, (${prim.Name})2, (${prim.Name})3 });
		assertEqual((${prim.Name})1, p.get(0));
		assertEqual((${prim.Name})2, p.get(1));
		assertEqual((${prim.Name})3, p.get(2));
	}
	
	@Test 
    public static void testAllocateBounds_${prim.Name}_ok() {
		assertEqual((double)(${prim.Name})0, (double)Pointer.allocate${prim.CapName}().get(0), 0);
		assertEqual((double)(${prim.Name})0, (double)Pointer.allocate${prim.CapName}s(1).get(0), 0);
		assertEqual((double)(${prim.Name})0, (double)Pointer.allocate${prim.CapName}s(2).slide(1).get(-1), 0);
		
		//TODO slide, slideBytes
	}
	
	@Test 
    public static void testAllocateBounds_${prim.Name}_failAfter() throws ArrayIndexOutOfBoundsException {
		Pointer.allocate${prim.CapName}().get(1);
	}
	@Test 
    public static void testAllocateBounds_${prim.Name}_failBefore() throws ArrayIndexOutOfBoundsException {
		Pointer.allocate${prim.CapName}().get(-1);
	}
	
#end
}
