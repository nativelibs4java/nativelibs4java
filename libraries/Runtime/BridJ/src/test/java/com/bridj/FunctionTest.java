/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj;

import com.bridj.JNI;
import com.bridj.BridJ;
import com.bridj.ann.Library;
//import com.sun.jna.Native;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author Olivier
 */
@Library("test")
@com.bridj.ann.Runtime(CRuntime.class)
public class FunctionTest {
	
	@Before
    public void register() {
		BridJ.register(getClass());
	}
    public native int testAddDyncall(int a, int b);
    
    @Test
    public void add() {
    		int res = testAddDyncall(10, 4);
    		assertEquals(14, res);
    }
}
