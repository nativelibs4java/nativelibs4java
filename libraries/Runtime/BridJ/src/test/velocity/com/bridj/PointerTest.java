package com.bridj;

import org.junit.Test;
import static org.junit.Assert.*;

public class PointerTest {

#foreach ($prim in $primitivesNoBool)
	@Test 
    public void testPointerTo_${prim.Name}_Values() {
		Pointer<${prim.WrapperName}> p = Pointer.pointerTo(new ${prim.Name}[]{ (${prim.Name})1, (${prim.Name})2, (${prim.Name})3 });
		assertEquals((${prim.Name})1, (${prim.Name})p.get(0), 0);
		assertEquals((${prim.Name})2, (${prim.Name})p.get(1), 0);
		assertEquals((${prim.Name})3, (${prim.Name})p.get(2), 0);
	}
	
	@Test 
    public void testAllocateBounds_${prim.Name}_ok() {
		assertEquals((double)(${prim.Name})0, (double)Pointer.allocate${prim.CapName}().get(0), 0);
		assertEquals((double)(${prim.Name})0, (double)Pointer.allocate${prim.CapName}s(1).get(0), 0);
		assertEquals((double)(${prim.Name})0, (double)Pointer.allocate${prim.CapName}s(2).offset(${prim.Size}).get(-1), 0);
		
		//TODO slide, slideBytes
	}
	
	@Test(expected=IndexOutOfBoundsException.class)
    public void testAllocateBounds_${prim.Name}_failAfter() {
		Pointer.allocate${prim.CapName}().get(1);
	}
	@Test(expected=IndexOutOfBoundsException.class)
    public void testAllocateBounds_${prim.Name}_failBefore() throws IndexOutOfBoundsException {
		Pointer.allocate${prim.CapName}().get(-1);
	}
	
#end
}
