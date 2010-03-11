/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj;

import com.bridj.JNI;
import com.bridj.BridJ;
import com.bridj.ann.Library;
//import com.sun.jna.Native;
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
    
    public enum ETest implements ValuedEnum<ETest> {
    	eFirst(0),
    	eSecond(1),
    	eThird(2);
    	
    	ETest(int value) {
    		this.value = value;
    	}
    	final int value;
    	public long value() {
    		return value;
    	}
    }
    public static native FlagSet<ETest> testEnum(FlagSet<ETest> e);
    
    @Test
    public void add() {
    		int res = testAddDyncall(10, 4);
    		assertEquals(14, res);
    }
    
    @Test
    public void enu() {
    	FlagSet<ETest> e = FlagSet.fromValues(ETest.eFirst);
    }
}
