/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bridj;

import java.io.FileNotFoundException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.bridj.Pointer.*;

/**
 *
 * @author ochafik
 */
public class DynamicCallbackTest {

    public DynamicCallbackTest() {
    }


    @Test
    public void testSimpleDynamicAdd() throws FileNotFoundException {
        NativeLibrary lib = BridJ.getNativeLibrary("test");
        DynamicFunction i = BridJ.getCRuntime().newFunction(lib.getSymbolPointer("testAddDyncall"), null, int.class, int.class, int.class);
        int res = (Integer)i.apply(1, 2);
        assertEquals(3, res);
    }
}