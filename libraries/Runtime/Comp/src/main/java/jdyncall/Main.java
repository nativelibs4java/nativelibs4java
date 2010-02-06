/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jdyncall;

import com.nativelibs4java.runtime.JNI;
import com.nativelibs4java.runtime.ann.*;
import com.nativelibs4java.runtime.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Olivier Chafik
 */
public class Main {

    @Library("test")
    public static class Test extends NativeLib<Test> {
        public Test() throws FileNotFoundException {
			//super(Test.class);
        	//super(new File("/Users/ochafik/nativelibs4java/Runtime/JDyncall/src/main/cpp/jdyncall/build_out/darwin_universal_gcc_debug/libtest.dylib"));
            super(new File("test.dll"));
		}
        //@Library("C:\\Prog\\dyncall\\dyncall\\buildsys\\vs2008\\Debug\\test")
        //@Library("C:\\Users\\Olivier\\Prog\\dyncall\\dyncall\\buildsys\\vs2008\\x64\\Debug\\test")
        @Mangling({"?sinInt@@YANH@Z", "__Z6sinInti"})
        public native double sinInt(int d);

        public native void voidTest();
        
        static class Struct1 {
            @Wide String ws;
            String s;
            
        }
        static class Struct2 extends Struct<Struct2> {
            @ByValue Struct1 s1;
            @Wide @Length(10) String s;
        }
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        Test test = null;
        try {
            System.out.println(Main.class.getResource("Main.class"));
            int nWarmUp = 16000;
            int nCalls = 1000000;
            int nTests = 10;
            int arg = 10;
            double res = 0;
            
            boolean warmup = true;
            test = new Test();

            if (true) {
                Method[] mes = Test.class.getDeclaredMethods();
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
                res = test.sinInt(arg);
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
                System.out.println("# Dyncall+JNI's sinus is " + (totalSlower / nTests) + " times slower than java sinus function");
            }

            PerfTest.DynCallTest dct = new PerfTest.DynCallTest();
            if (true) {
                //JNI.registerClass(PerfTest.class);
                //PerfTest.DynCall.testAddDyncall(1, 2);
                int tot = 0, seed = System.getenv().size();
                if (warmup) {
                    for (int i = 0; i < nWarmUp; i++)
                        tot = PerfTest.testAddJNI(tot, seed);
                    for (int i = 0; i < nWarmUp; i++)
                        tot = dct.testAddDyncall(tot, seed);
                    for (int i = 0; i < nWarmUp; i++)
                        tot = PerfTest.JNATest.testAddJNA(tot, seed);
                }

                double totalJNI = 0, totalDynCall = 0, totalJNA = 0;
                
                long startJNI = System.nanoTime();
                for (int iTest = 0; iTest < nTests; iTest++) {
                    for (int i = 0; i < nCalls; i++)
                        tot = PerfTest.testAddJNI(tot, seed);
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
                        tot = PerfTest.JNATest.testAddJNA(tot, seed);
                }
                long timeJNA = System.nanoTime() - startJNA;
                totalJNA += timeJNA;

                //System.out.println("timeNat = " + timeNat);
                //System.out.println("timePrim = " + timePrim);
                System.out.println("# Dyncall's simple int add is " + (totalDynCall / totalJNI) + " times slower than pure JNI in average");
                System.out.println("# JNA's simple int add is " + (totalJNA / totalJNI) + " times slower than pure JNI in average");
                System.out.println("# => Dyncall is " + (totalJNA / totalDynCall) + " times faster than JNA");

            }

            if (true) {
                //JNI.registerClass(PerfTest.class);
                //PerfTest.DynCall.testAddDyncall(1, 2);
                int tot = 0, seed = System.getenv().size();
                if (warmup) {
                    for (int i = 0; i < nWarmUp; i++)
                        tot = PerfTest.testASinB(tot, seed);
                    for (int i = 0; i < nWarmUp; i++)
                        tot = dct.testASinB(tot, seed);
                    for (int i = 0; i < nWarmUp; i++)
                        tot = PerfTest.JNATest.testASinB(tot, seed);
                }

                double totalJNI = 0, totalDynCall = 0, totalJNA = 0;
                long startJNI = System.nanoTime();
                for (int iTest = 0; iTest < nTests; iTest++) {
                    for (int i = 0; i < nCalls; i++)
                        tot = PerfTest.testASinB(tot, seed);
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
                        tot = PerfTest.JNATest.testASinB(tot, seed);
                }
                long timeJNA = System.nanoTime() - startJNA;
                totalJNA += timeJNA;

                System.out.println("# Dyncall's 'a * sin(b)' add is " + (totalDynCall / totalJNI) + " times slower than pure JNI in average");
                System.out.println("# JNA's 'a * sin(b)' add is " + (totalJNA / totalJNI) + " times slower than pure JNI in average");
                System.out.println("# => Dyncall is " + (totalJNA / totalDynCall) + " times faster than JNA");

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
            System.in.read();
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
