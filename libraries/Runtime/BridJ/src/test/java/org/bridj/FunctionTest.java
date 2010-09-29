/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bridj;

import org.bridj.JNI;
import org.bridj.BridJ;
import org.bridj.ann.Library;
//import com.sun.jna.Native;
import java.util.Collections;
import java.util.Iterator;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author Olivier
 */
@Library("test")
@org.bridj.ann.Runtime(CRuntime.class)
public class FunctionTest {
	
	@Before
    public void register() {
		BridJ.register(getClass());
	}
    @After
    public void unregister() {
	//	BridJ.releaseAll();
		System.gc();
		try {
			Thread.sleep(200);
		} catch (Exception ex) {}
		System.gc();
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
		public Iterator<ETest> iterator() {
			return Collections.singleton(this).iterator();
		}
		public static ValuedEnum<ETest> fromValue(long value) {
			return FlagSet.fromValue(value, values());
		}
    }
    public static native ValuedEnum<ETest> testEnum(ValuedEnum<ETest> e);
    
    @Test
    public void add() {
		int res = testAddDyncall(10, 4);
		assertEquals(14, res);
    }

    @Test
    public void testEnumCalls() {
        for (ETest e : ETest.values()) {
            ValuedEnum<ETest> r = testEnum(e);
            assertEquals(e.value(), r.value());
            //assertEquals(e, r);
        }
    }
    @Test
    public void enu() {
    	for (ETest v : ETest.values())
    	{
	    	FlagSet<ETest> e = FlagSet.fromValues(v);
	    	assertNotNull(e);
	    	assertEquals(v.value(), e.value());
	    	ETest[] values = e.getEnumClassValues();
	    	assertEquals(1, values.length);
	    	assertEquals(v, values[0]);
    	}
    }
}
