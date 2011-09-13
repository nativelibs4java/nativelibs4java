package org.bridj;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import org.bridj.objc.*;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
import org.bridj.ann.Runtime;
import org.bridj.ann.Array;
import org.bridj.ann.Ptr;
import org.bridj.cpp.com.*;
import static org.bridj.Pointer.*;
import static org.bridj.BridJ.*;


@Library("Foundation")
public class ObjectiveCTest {
	static boolean mac = Platform.isMacOSX();
	static {
		try {
			BridJ.register();
		} catch (Throwable th) {
			if (mac)
				throw new RuntimeException(th);
		}
	}
	
    protected Pointer<NSAutoreleasePool> pool;
    
    @Before
    public void init() {
    	if (!mac) return;
        pool = NSAutoreleasePool.new_();
		assertNotNull(pool);
    }

    @After
    public void cleanup() {
        if (!mac) return;
        pool.get().drain();
    }
    
	@Test 
	public void testNSNumber() {
		if (!mac) return;
        
		long n = 13;
		Pointer<NSNumber> pnn = NSNumber.numberWithLong(n);
		//System.out.println("pnn = " + pnn);
		NSNumber nn = pnn.get();
		//System.out.println("nn = " + nn);
		assertEquals(n + "", nn.toString());
		assertEquals(n, nn.shortValue());   
		assertEquals(n, nn.intValue());   
		assertEquals(n, nn.longValue());   
		assertEquals(n, nn.floatValue(), 0);    
		assertEquals(n, nn.doubleValue(), 0);       
	}
}
