/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj;

import com.bridj.Demangler.DemanglingException;
import com.bridj.Demangler.MemberRef;
import com.bridj.cpp.VC9Demangler;
import static com.bridj.Demangler.MemberRef.*;
import com.bridj.Demangler.SpecialName;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;

import static org.junit.Assert.*;
public class VC9DemanglingTest {

    @Test

	public void simpleConstructor() {
		demangleVC9("??0Ctest@@QEAA@XZ", CPPTest.Ctest.class, SpecialName.Constructor, null);
	}

    @Test
	public void simpleFunctions() {
		demangleVC9("?sinInt@@YANH@Z", null, "sinInt", double.class, int.class);
		demangleVC9("?forwardCall@@YAHP6AHHH@ZHH@Z", null, "forwardCall", int.class, Pointer.class, int.class, int.class);
	}

    private void demangleVC9(String string, Class enclosingType, Object memberName, Class returnType, Class... paramTypes) {
        try {
            checkSymbol(new VC9Demangler(null, string).parseSymbol(), enclosingType, memberName, returnType, paramTypes);
        } catch (DemanglingException ex) {
            Logger.getLogger(VC9DemanglingTest.class.getName()).log(Level.SEVERE, null, ex);
            throw new AssertionError(ex.toString());
        }
    }

    private void checkSymbol(MemberRef symbol, Class enclosingType, Object memberName, Class returnType, Class[] paramTypes) {
        if (memberName != null)
            assertEquals("Bad name", memberName, symbol.getMemberName());
        if (enclosingType != null)
            assertTrue("Bad enclosing type (got " + symbol.getEnclosingType() + ", expected " + enclosingType.getName() + ")", symbol.getEnclosingType().matches(enclosingType));
        if (returnType != null)
            assertTrue("Bad return type", symbol.getValueType().matches(returnType));

        int nArgs = symbol.paramTypes.length;
        assertEquals("Bad number of parameters", paramTypes.length, nArgs);

        for (int iArg = 0; iArg < nArgs; iArg++) {
            assertTrue("Bad type for " + (iArg + 1) + "th param", symbol.paramTypes[iArg].matches(paramTypes[iArg]));
        }
    }


}