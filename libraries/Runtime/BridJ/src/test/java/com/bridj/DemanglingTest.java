/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj;

import com.bridj.Demangler.DemanglingException;
import com.bridj.Demangler.MemberRef;
import com.bridj.cpp.GCC4Demangler;
import com.bridj.cpp.VC9Demangler;
import static com.bridj.Demangler.MemberRef.*;
import com.bridj.Demangler.SpecialName;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;

import static org.junit.Assert.*;
public class DemanglingTest {

	@Test
	public void gcc() {
		demangle(
			null, 
			"__Z17testInPlaceSquarePdj", 
			null, "testInPlaceSquare", 
			void.class, Pointer.class, int.class
		);
	}
    @Test
	public void simpleConstructor() {
		demangle(
			"??0Ctest@@QEAA@XZ", 
			null, 
			CPPTest.Ctest.class, SpecialName.Constructor, 
			null
		);
	}
	@Test
    public void methods() {
    	demangle(
			null, 
			"__ZN5Ctest7testAddEii", 
			CPPTest.Ctest.class, "testAdd", 
			int.class, int.class, int.class
		);
    	
    }

    @Test
	public void simpleFunctions() {
		demangle("?sinInt@@YANH@Z", null, null, 
				"sinInt", 
				double.class, int.class);
		demangle("?forwardCall@@YAHP6AHHH@ZHH@Z", null, null, "forwardCall", int.class, Pointer.class, int.class, int.class);
	}

    private void demangle(String vc9, String gcc4, Class enclosingType, Object memberName, Class returnType, Class... paramTypes) {
        try {
        	if (vc9 != null)
        		checkSymbol(new VC9Demangler(null, vc9).parseSymbol(), enclosingType, memberName, returnType, paramTypes);
        	if (gcc4 != null)
        		checkSymbol(new GCC4Demangler(null, gcc4).parseSymbol(), enclosingType, memberName, returnType, paramTypes);
        } catch (DemanglingException ex) {
            Logger.getLogger(DemanglingTest.class.getName()).log(Level.SEVERE, null, ex);
            throw new AssertionError(ex.toString());
        }
    }

    private void checkSymbol(MemberRef symbol, Class enclosingType, Object memberName, Class returnType, Class[] paramTypes) {
        if (memberName != null)
            assertEquals("Bad name", memberName, symbol.getMemberName());
        if (enclosingType != null) {
        	assertNotNull("Null enclosing type : " + symbol, symbol.getEnclosingType());
            assertTrue("Bad enclosing type (got " + symbol.getEnclosingType() + ", expected " + enclosingType.getName() + ")", symbol.getEnclosingType().matches(enclosingType));
        }
        if (returnType != null && symbol.getValueType() != null)
            assertTrue("Bad return type", symbol.getValueType().matches(returnType));

        int nArgs = symbol.paramTypes.length;
        assertEquals("Bad number of parameters", paramTypes.length, nArgs);

        for (int iArg = 0; iArg < nArgs; iArg++) {
            assertTrue("Bad type for " + (iArg + 1) + "th param", symbol.paramTypes[iArg].matches(paramTypes[iArg]));
        }
    }


}