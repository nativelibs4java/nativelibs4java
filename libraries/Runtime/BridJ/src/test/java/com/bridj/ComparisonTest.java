/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj;
import static com.bridj.Pointer.*;

import com.bridj.ann.*;
import com.bridj.cpp.CPPRuntime;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;
/**
 * mvn -o test-compile && MAVEN_OPTS="-Xmx2g -Xrunhprof:cpu=samples,doe=y,depth=15" mvn -o -DforkMode=never surefire:test -Dtest=ComparisonTest
 * @author Olivier Chafik
 */
@com.bridj.ann.Runtime(CPPRuntime.class)
public class ComparisonTest {

    @Library("test")
    public static class TestLib {
    	public TestLib() {
			BridJ.register(getClass());
		}
        //@Mangling({"?sinInt@@YANH@Z", "_Z6sinInti"})
        public native double sinInt(int d);

        //@Mangling({"?voidTest@@YAXXZ", "_Z8voidTestv"})
        public native void voidTest();
        
        static class Struct1 {
            @Wide String ws;
            String s;
            
        }
        static class Struct2 extends StructObject {
            @ByValue Struct1 s1;
            @Wide @Array(10) String s;
        }
    }
    /**
     * @param args the command line arguments
     */
    @Test
    public void perfTest() {
    	
		//com.sun.jna.Native.setProtected(true);
        TestLib test = null;
        try {
            System.out.println(ComparisonTest.class.getResource("Main.class"));
            int nWarmUp = 1600;
            int nCalls = 100000;
            int nTests = 10;
            int arg = 10;
            double res = 0;
            
            boolean warmup = true;
            test = new TestLib();

            if (true) {
                Method[] mes = TestLib.class.getDeclaredMethods();
                Method me = mes[0];
                //Test.class.getMethod("sinInt", Integer.TYPE)
                //long address = DynCall.getSymbolAddress(me);
                //JNI.registerClass(Test.class);

                /*
                mes = PerfTest.class.getDeclaredMethods();
                me = mes[0];
                //Test.class.getMethod("sinInt", Integer.TYPE)
                address = DynCall.getSymbolAddress(me);
                JNI.registerClass(PerfTest.class);
    */
                test.voidTest();
                //res = test.sinInt(arg);
                double tot = 0;
                if (warmup) {
                    for (int i = 0; i < nWarmUp; i++)
                        tot += test.sinInt(arg);
                    for (int i = 0; i < nWarmUp; i++)
                        tot += Math.sin(arg);
                }

                double totalSlower = 0;
                for (int iTest = 0; iTest < nTests; iTest++) {
                    long startNat = System.nanoTime();
                    for (int i = 0; i < nCalls; i++)
                        tot += test.sinInt(arg);
                    long timeNat = System.nanoTime() - startNat;

                    long startPrim = System.nanoTime();
                    for (int i = 0; i < nCalls; i++)
                        tot += Math.sin(arg);
                    long timePrim = System.nanoTime() - startPrim;

                    //System.out.println("timeNat = " + timeNat);
                    //System.out.println("timePrim = " + timePrim);
                    double slower = (timeNat / (double)timePrim);
                    totalSlower += slower;
                }
                System.out.println("#");
                System.out.println("# Dyncall+JNI's sinus is " + (totalSlower / nTests) + " times slower than java sinus function");
                System.out.println("#");
                
            }

            PerfLib.DynCallTest dct = new PerfLib.DynCallTest();
            if (true) {
                //JNI.registerClass(PerfTest.class);
                //PerfTest.DynCall.testAddDyncall(1, 2);
                int tot = 0, seed = System.getenv().size();
                if (warmup) {
                    for (int i = 0; i < nWarmUp; i++)
                        tot = PerfLib.testAddJNI(tot, seed);
                    for (int i = 0; i < nWarmUp; i++)
                        tot = dct.testAddDyncall(tot, seed);
                    for (int i = 0; i < nWarmUp; i++)
                        tot = PerfLib.JNATest.testAddJNA(tot, seed);
                    for (int i = 0; i < nWarmUp; i++)
                        tot = PerfLib.JNAInterfaceTest.INSTANCE.testAddJNA(tot, seed);
                }

                double totalJNI = 0, totalDynCall = 0, totalJNA = 0, totalJNAInterface = 0;
                
                long startJNI = System.nanoTime();
                for (int iTest = 0; iTest < nTests; iTest++) {
                    for (int i = 0; i < nCalls; i++)
                        tot = PerfLib.testAddJNI(tot, seed);
                }
                long timeJNI = System.nanoTime() - startJNI;
                totalJNI += timeJNI;

                long startDyncall = System.nanoTime();
                for (int iTest = 0; iTest < nTests; iTest++) {
                    for (int i = 0; i < nCalls; i++)
                        tot = dct.testAddDyncall(tot, seed);
                }
                long timeDyncall = System.nanoTime() - startDyncall;
                totalDynCall += timeDyncall;

                long startJNA = System.nanoTime();
                for (int iTest = 0; iTest < nTests; iTest++) {
                    for (int i = 0; i < nCalls; i++)
                        tot = PerfLib.JNATest.testAddJNA(tot, seed);
                }
                long timeJNA = System.nanoTime() - startJNA;
                totalJNA += timeJNA;
                
                long startJNAInterface = System.nanoTime();
                for (int iTest = 0; iTest < nTests; iTest++) {
                    for (int i = 0; i < nCalls; i++)
                        tot = PerfLib.JNAInterfaceTest.INSTANCE.testAddJNA(tot, seed);
                }
                long timeJNAInterface = System.nanoTime() - startJNAInterface;
                totalJNAInterface += timeJNAInterface;
                
                

                //System.out.println("timeNat = " + timeNat);
                //System.out.println("timePrim = " + timePrim);
                System.out.println("#");
                System.out.println("# Dyncall's simple int add is " + (totalDynCall / totalJNI) + " times slower than pure JNI in average");
                System.out.println("# JNA's simple int add is " + (totalJNA / totalJNI) + " times slower than pure JNI in average");

		double bridJFaster = totalJNA / (double)totalDynCall;
                double bridJFasterInterf = totalJNAInterface / (double)totalDynCall;
                System.out.println("# => Dyncall is " + bridJFaster + " times faster than JNA in direct mode");
                System.out.println("# => Dyncall is " + bridJFasterInterf + " times faster than JNA in interface mode");
                System.out.println("#");

		if (Math.abs(bridJFaster - 1) > 1e10) {
			assertTrue(bridJFaster > 0.8);
			assertTrue(bridJFasterInterf > 5.0);
		}

            }

            if (true) {
                //JNI.registerClass(PerfTest.class);
                //PerfTest.DynCall.testAddDyncall(1, 2);
                int tot = 0, seed = System.getenv().size();
                if (warmup) {
                    for (int i = 0; i < nWarmUp; i++)
                        tot += (int)PerfLib.testASinB(tot, seed);
                    for (int i = 0; i < nWarmUp; i++)
                        tot = dct.testASinB(tot, seed);
                    for (int i = 0; i < nWarmUp; i++)
                        tot = PerfLib.JNATest.testASinB(tot, seed);
                    for (int i = 0; i < nWarmUp; i++)
                        tot = PerfLib.JNAInterfaceTest.INSTANCE.testASinB(tot, seed);
                }

                double totalJNI = 0, totalDynCall = 0, totalJNA = 0, totalJNAInterface = 0;
                long startJNI = System.nanoTime();
                for (int iTest = 0; iTest < nTests; iTest++) {
                    for (int i = 0; i < nCalls; i++)
                        tot += (int)PerfLib.testASinB(tot, seed);
                }
                long timeJNI = System.nanoTime() - startJNI;
                totalJNI += timeJNI;

                long startDyncall = System.nanoTime();
                for (int iTest = 0; iTest < nTests; iTest++) {
                    for (int i = 0; i < nCalls; i++)
                        tot = dct.testASinB(tot, seed);
                }
                long timeDyncall = System.nanoTime() - startDyncall;
                totalDynCall += timeDyncall;

                long startJNA = System.nanoTime();
                for (int iTest = 0; iTest < nTests; iTest++) {
                    for (int i = 0; i < nCalls; i++)
                        tot = PerfLib.JNATest.testASinB(tot, seed);
                }
                long timeJNA = System.nanoTime() - startJNA;
                totalJNA += timeJNA;


                long startJNAInterface = System.nanoTime();
                for (int iTest = 0; iTest < nTests; iTest++) {
                    for (int i = 0; i < nCalls; i++)
                        tot = PerfLib.JNAInterfaceTest.INSTANCE.testASinB(tot, seed);
                }
                long timeJNAInterface = System.nanoTime() - startJNAInterface;
                totalJNAInterface += timeJNAInterface;

                System.out.println("#");
                System.out.println("# Dyncall's 'a * sin(b)' add is " + (totalDynCall / (double)totalJNI) + " times slower than pure JNI in average");
                System.out.println("# JNA's 'a * sin(b)' add is " + (totalJNA / (double)totalJNI) + " times slower than pure JNI in average");
                double bridJFaster = totalJNA / (double)totalDynCall;
                double bridJFasterInterf = totalJNAInterface / (double)totalDynCall;
                System.out.println("# => Dyncall is " + bridJFaster + " times faster than JNA in direct mode");
                System.out.println("# => Dyncall is " + bridJFasterInterf + " times faster than JNA in interface mode");
                System.out.println("#");

		if (Math.abs(bridJFaster - 1) > 1e10) {
			assertTrue(bridJFaster > 0.8);
			assertTrue(bridJFasterInterf > 5.0);
		}
            }
            System.out.println("res = " + res + ", sin(" + arg + ") = " + Math.sin(arg));
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (test != null)
                test.voidTest();
        }
        System.out.println(JNI.SIZE_T_SIZE);
        try {
            //System.in.read();
        } catch (Exception ex) {
            Logger.getLogger(ComparisonTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    
    @Test
	public void compareStructCreations() throws InterruptedException {
		//System.err.println("#");
		//System.err.println("# Warming structs up...");
		long n = 100000;
		long warmup = 2000;
		for (int i = 0; i < warmup; i++)
			new StructTest.MyStruct();
		
		for (int i = 0; i < warmup; i++)
			new StructTest.MyJNAStruct();
		
		for (int i = 0; i < warmup; i++)
			new StructTest.MyNIOStruct();
		
		for (int i = 0; i < warmup; i++)
			new StructTest.MyOptimalStruct();
		
		for (int i = 0; i < warmup; i++)
			new StructTest.MyJavolutionStruct();
		
		long timeJNA, timeOptimal, timeBridJ, timeNIO, timeJavolution;
		
        doGC();
        //System.err.println("# Testings NIO structs...");
		{
			long start = System.nanoTime();
			for (int i = 0; i < n; i++)
				new StructTest.MyNIOStruct();
			
			timeNIO = System.nanoTime() - start;
		}
        doGC();
		//System.err.println("# Testings JNA structs...");
		{
			long start = System.nanoTime();
			for (int i = 0; i < n; i++)
				new StructTest.MyJNAStruct();
			
			timeJNA = System.nanoTime() - start;
		}
        doGC();
        //System.err.println("# Testings Optimal structs...");
		{
			long start = System.nanoTime();
			for (int i = 0; i < n; i++)
				new StructTest.MyOptimalStruct();
			
			timeOptimal = System.nanoTime() - start;
		}
        doGC();
		//System.err.println("# Testings BridJ structs...");
		{
			long start = System.nanoTime();
			for (int i = 0; i < n; i++)
				new StructTest.MyStruct();
			
			timeBridJ = System.nanoTime() - start;
		}
        doGC();
		//System.err.println("# Testings Javolution structs...");
		{
			long start = System.nanoTime();
			for (int i = 0; i < n; i++)
				new StructTest.MyJavolutionStruct().getByteBuffer();
			
			timeJavolution = System.nanoTime() - start;
		}
		double bridJFaster = printResults("Creation of struct", "Creation of BridJ's structs", "create", n, timeJNA, timeOptimal, timeBridJ, timeNIO, timeJavolution);
        
		if (Math.abs(bridJFaster - 1) < 1e10)
			return;
		
		assertTrue(bridJFaster > 20);
	}
	
	@Test
	public void compareStructCasts() throws InterruptedException {
//		System.err.println("#");
//		System.err.println("# Warming structs up...");
		long n = 100000;
		long warmup = 2000;
		Pointer pBridJ = allocateBytes(100);
		com.sun.jna.Memory pJNA = new com.sun.jna.Memory(100);
        ByteBuffer pNIO = ByteBuffer.allocateDirect(100);
		
		for (int i = 0; i < warmup; i++)
			new StructTest.MyStruct(pBridJ);
		
		for (int i = 0; i < warmup; i++)
			new StructTest.MyNIOStruct(pNIO);
		
		for (int i = 0; i < warmup; i++)
			new StructTest.MyJNAStruct(pJNA);
		
		for (int i = 0; i < warmup; i++)
			new StructTest.MyOptimalStruct(pJNA);
		
		for (int i = 0; i < warmup; i++)
			new StructTest.MyJavolutionStruct().setByteBuffer(pNIO, 0);
		
		long timeJNA, timeOptimal, timeBridJ, timeNIO, timeJavolution;
		
		doGC();
//        System.err.println("# Testings NIO structs...");
		{
			long start = System.nanoTime();
			for (int i = 0; i < n; i++)
				new StructTest.MyNIOStruct(pNIO);
			
			timeNIO = System.nanoTime() - start;
		}
		doGC();
//        System.err.println("# Testings JNA structs...");
		{
			long start = System.nanoTime();
			for (int i = 0; i < n; i++)
				new StructTest.MyJNAStruct(pJNA);
			
			timeJNA = System.nanoTime() - start;
		}
        doGC();
//        System.err.println("# Testings Optimal structs...");
		{
			long start = System.nanoTime();
			for (int i = 0; i < n; i++)
				new StructTest.MyOptimalStruct(pJNA);
			
			timeOptimal = System.nanoTime() - start;
		}
        doGC();
//		System.err.println("# Testings BridJ structs...");
		{
			long start = System.nanoTime();
			for (int i = 0; i < n; i++)
				new StructTest.MyStruct(pBridJ);
			
			timeBridJ = System.nanoTime() - start;
		}
        doGC();
//		System.err.println("# Testings Javolution structs...");
		{
			long start = System.nanoTime();
			for (int i = 0; i < n; i++)
				new StructTest.MyJavolutionStruct().setByteBuffer(pNIO, 0);
			
			timeJavolution = System.nanoTime() - start;
		}
        double bridJFaster = printResults("Cast to struct", "Cast to BridJ's structs", "cast", n, timeJNA, timeOptimal, timeBridJ, timeNIO, timeJavolution);
        
        assertTrue(bridJFaster > 30); // */
	}
	@Test
	public void compareFieldsAccess() throws InterruptedException {
		long n = 100000;
		long warmup = 2000;
		Pointer pBridJ = allocateBytes(100);
		com.sun.jna.Memory pJNA = new com.sun.jna.Memory(100);
        ByteBuffer pNIO = ByteBuffer.allocateDirect(100);

		StructTest.MyStruct bridJ = new StructTest.MyStruct();
		StructTest.MyNIOStruct nio = new StructTest.MyNIOStruct();
		StructTest.MyJNAStruct jna = new StructTest.MyJNAStruct();
		StructTest.MyOptimalStruct optim = new StructTest.MyOptimalStruct();
		StructTest.MyJavolutionStruct javo = new StructTest.MyJavolutionStruct();
		
		for (int i = 0; i < warmup; i++) {
			bridJ.a(bridJ.a() + 1);
			bridJ.b(bridJ.a() + bridJ.b());
		}
		
		for (int i = 0; i < warmup; i++) {
			nio.a(nio.a() + 1);
			nio.b(nio.a() + nio.b());
		}
		
		for (int i = 0; i < warmup; i++) {
			jna.a = jna.a + 1;
			jna.b = jna.a + jna.b;
			//jna.writeField("b");
			//jna.writeField("b");
			jna.write();
		}
		
		for (int i = 0; i < warmup; i++) {
			optim.a(optim.a() + 1);
			optim.b(optim.a() + optim.b());
		}
		
		for (int i = 0; i < warmup; i++) {
			javo.a.set(javo.a.get() + 1);
			javo.b.set(javo.a.get() + javo.b.get());
		}
		
		long timeJNA, timeOptimal, timeBridJ, timeNIO, timeJavolution;
		
		doGC();
		{
			long start = System.nanoTime();
			for (int i = 0; i < n; i++) {
				nio.a(nio.a() + 1);
				nio.b(nio.a() + nio.b());
			}
			
			timeNIO = System.nanoTime() - start;
		}
		doGC();
		{
			long start = System.nanoTime();
			for (int i = 0; i < n; i++) {
				jna.a = jna.a + 1;
				jna.b = jna.a + jna.b;
				//jna.writeField("b");
				//jna.writeField("b");
				jna.write();
			}
			
			timeJNA = System.nanoTime() - start;
		}
        doGC();
		{
			long start = System.nanoTime();
			for (int i = 0; i < n; i++) {
				optim.a(optim.a() + 1);
				optim.b(optim.a() + optim.b());
			}
			
			timeOptimal = System.nanoTime() - start;
		}
        doGC();
		{
			long start = System.nanoTime();
			for (int i = 0; i < n; i++) {
				bridJ.a(bridJ.a() + 1);
				bridJ.b(bridJ.a() + bridJ.b());
			}
			
			timeBridJ = System.nanoTime() - start;
		}
        doGC();
		{
			long start = System.nanoTime();
			for (int i = 0; i < n; i++) {
				javo.a.set(javo.a.get() + 1);
				javo.b.set(javo.a.get() + javo.b.get());
			}
			
			timeJavolution = System.nanoTime() - start;
		}
        double bridJFaster = printResults("Fields read/write", "Read/write of BridJ's struct fields", "read/write", n, timeJNA, timeOptimal, timeBridJ, timeNIO, timeJavolution);
        
        //assertTrue(bridJFaster > 30); // */
	}
    static double printResults(String title, String longOp, String op, long n, long timeJNA, long timeOptimal, long timeBridJ, long timeNIO, long timeJavolution) {
        System.err.println("#");
        System.err.println("# " + title + " :");
		System.err.println("# Optimal took " + (timeOptimal / 1000000d) + " millis to " + op + " " + n + " simple structs : " + microPerStruct(timeOptimal, n));
		System.err.println("# BridJ took " + (timeBridJ / 1000000d) + " millis to " + op + " " + n + " simple structs : " + microPerStruct(timeBridJ, n));
		System.err.println("# Javolution took " + (timeJavolution / 1000000d) + " millis to " + op + " " + n + " simple structs : " + microPerStruct(timeJavolution, n));
		System.err.println("# NIO took " + (timeNIO / 1000000d) + " millis to " + op + " " + n + " simple structs : " + microPerStruct(timeNIO, n));
		System.err.println("# JNA took " + (timeJNA / 1000000d) + " millis to " + op + " " + n + " simple structs : " + microPerStruct(timeJNA, n));
		
		double bridJFaster = timeJNA / (double)timeBridJ;
		System.err.println("# " + longOp + " is " + bridJFaster + " times faster than JNA's ");
        System.err.println("#");
		return bridJFaster;
    }
    static String microPerStruct(long nanoTime, long n) {
        return (nanoTime / 1000d / (double)n) + " microsecond per struct";
    }
    static void doGC() throws InterruptedException {
        System.gc();
        Thread.sleep(200);
		System.gc();
        Thread.sleep(100);
    }
}
