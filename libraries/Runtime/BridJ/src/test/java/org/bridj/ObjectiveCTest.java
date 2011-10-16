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
import static org.bridj.objc.FoundationLibrary.*;
import java.util.*;

@Library("Foundation")
@Runtime(ObjectiveCRuntime.class)
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
	
	@Library("Foundation")
	public static class NSWorkspace extends NSObject
	{
		public static native Pointer<NSWorkspace> sharedWorkspace();
		
		public native Pointer<?> runningApplications();
	}
	@Test
	public void testNSWorkspace() {
		if (!mac) return;
		
		BridJ.register(NSWorkspace.class);
		Pointer<NSWorkspace> pWorkspace = NSWorkspace.sharedWorkspace();
		assertNotNull(pWorkspace);
		
		NSWorkspace workspace = pWorkspace.get();
		assertNotNull(workspace);
	}
    
    @Test
    public void testNSString() {
        for (String s : new String[] { "", "1", "ha\nha\u1234" }) {
            assertEquals(s, pointerToNSString(s).get().toString());
            
            NSString ns = new NSString(s);
            assertEquals(s, ns.toString());
            assertEquals(s.length(), ns.length());
        }
    }
    
    @Test
    public void testSEL() {
        for (String s : new String[] { "", "1", "ha:ha" }) {
            SEL sel = SEL.valueOf(s);
            assertEquals(s, sel.getName());
        }
    }
    
    @Test
    public void testNSDictionary() {
    		Map<String, NSObject> map = new HashMap<String, NSObject>(), map2;
    		for (String s : new String[] { "", "1", "ha\nha\u1234" })
    			map.put(s, NSString.valueOf(s + s));
    		
    		NSDictionary dic = NSDictionary.valueOf(map);
    		assertEquals(map.size(), dic.count());
    		
    		map2 = dic.toMap();
    		assertEquals(map.size(), map2.size());
    		
    		//assertEquals(map, map2);
    		for (Map.Entry<String, NSObject> e : map.entrySet()) {
    			String key = e.getKey();
    			NSObject expected = e.getValue();
    			NSObject got = map2.get(key);
    			assertEquals(expected, got);
    			//assertEquals(expected.toString(), got.toString());
    		}
    }
    
    public static class NSNonExistentTestClass extends NSObject {
    		public native void whatever();
    }
    static void call_NSNonExistentTestClass_Whatever(ObjCProxy proxy) {
    		NSNonExistentTestClass p = pointerTo(proxy).as(NSNonExistentTestClass.class).get();
		//System.out.println(p);
		p.whatever();
    }
    @Test
    public void testProxySubClass() {
    		final boolean called[] = new boolean[1];
		call_NSNonExistentTestClass_Whatever(new ObjCProxy() {
			public void whatever() {
				called[0] = true;
			}
		});
		assertTrue(called[0]);
    }
    @Test
    public void testProxyDelegate() {
    		final boolean called[] = new boolean[1];
    		call_NSNonExistentTestClass_Whatever(new ObjCProxy(new Object() {
			public void whatever() {
				//System.out.println("Called whatever !!!");
				called[0] = true;
			}
		}));
		assertTrue(called[0]);
    }
}
