package org.bridj;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import org.bridj.objc.*;
import org.bridj.ann.Library;
import org.bridj.ann.Runtime;
import org.bridj.ann.Ptr;
import static org.bridj.Pointer.*;
import static org.bridj.objc.FoundationLibrary.*;
import java.util.*;

@Library("Foundation")
@Runtime(ObjectiveCRuntime.class)
public class ObjectiveCTest {
	static boolean mac = Platform.isMacOSX() && (System.getenv("BRIDJ_NO_OBJC") == null);
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
        if (!mac) return;
        for (String s : new String[] { "", "1", "ha\nha\u1234" }) {
            assertEquals(s, pointerToNSString(s).get().toString());
            
            NSString ns = new NSString(s);
            assertEquals(s, ns.toString());
            assertEquals(s.length(), ns.length());
        }
    }
    
    @Test
    public void testSEL() {
        if (!mac) return;
        for (String s : new String[] { "", "1", "ha:ha" }) {
            SEL sel = SEL.valueOf(s);
            assertEquals(s, sel.getName());
        }
    }
    
    @Test
    public void testNSDictionary() {
        if (!mac) return;
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
            public native int add2(int a, Pointer<Integer> p);
			public native float incf(float v);
            public native double add8(byte a, short b, int c, char d, long e, double f, Pointer<Integer> p);
    }
    static void test_NSNonExistentTestClass_add(ObjCObject proxy) {
    	if (!mac) return;
        NSNonExistentTestClass p = pointerTo(proxy).as(NSNonExistentTestClass.class).get();
        Pointer<Integer> ptr = pointerToInt(64);
        assertEquals(1 + ptr.get(), p.add2(1, ptr));
		assertEquals(127, p.add8((byte)1, (short)2, (int)4, (char)8, (long)16, (double)32, ptr), 0);
    }
    String DESCRIPTION = "WHATEVER !!!";
    @Test
    public void testProxy() {
        if (!mac) return;
        ObjCProxy proxy = new ObjCProxy() {
			public Pointer<NSString> description() {
				return pointerToNSString(DESCRIPTION);
			}
            public int add2(int a, Pointer<Integer> p) {
                return a + p.get();
            }
			public float incf(float v) {
                return v + 1;
            }
			public double add8(long a, int b, short c, byte d, char e, double f, Pointer<Integer> p) {
				return a + b + c + d + e + f + p.get();
			}
		};
		test_NSNonExistentTestClass_add(proxy);
        test_NSNonExistentTestClass_add(new ObjCProxy(proxy));
    }
    
    
    @Test
    public void testProxyFloat() {
        if (!mac) return;
        Object proxy = new Object() {
            public float incf(float v) {
                return v + 1;
            }
		};
        NSNonExistentTestClass p = pointerTo(new ObjCProxy(proxy)).as(NSNonExistentTestClass.class).get();
        System.out.println(p.description().get());
        assertEquals(11, p.incf(10), 0);
    }
    
    public static class NSEvent extends NSObject {
    		/*
	   public NSEvent(Pointer ptr) {
		   super(ptr);
	   }
	   */
	
	   //@Selector("addLocalMonitorForEventsMatchingMask:handler:")
	   public static native Pointer addGlobalMonitorForEventsMatchingMask_handler(@Ptr long mask, Pointer handler);
	}
	
	public abstract static class NSEventGlobalCallback extends Callback {
		public abstract void callback(Pointer<NSEvent> event);
	}

	public static class XXX extends NSEventGlobalCallback
	{
		@Override
		public void callback(Pointer<NSEvent> event) {
			System.out.println("Event: " + event);
		}
	}

   @Test
   public void testGlobalNSEventHook() throws Exception {
    	if (!mac) return;
        BridJ.register(NSEvent.class);

        XXX xxx = new XXX();

        Pointer handler = Pointer.pointerTo(xxx);

        System.out.println("handler: " + handler);

        Pointer hook = NSEvent.addGlobalMonitorForEventsMatchingMask_handler(1 << 1, handler);

        System.out.println("hook: " + hook);
   }
}
