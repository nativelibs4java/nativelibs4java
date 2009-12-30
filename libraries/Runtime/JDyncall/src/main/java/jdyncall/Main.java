/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jdyncall;

import com.nativelibs4java.runtime.DynCall;
import com.nativelibs4java.runtime.JNI;
import com.nativelibs4java.runtime.ann.Length;
import com.nativelibs4java.runtime.ann.ByValue;
import com.nativelibs4java.runtime.ann.Library;
import com.nativelibs4java.runtime.ann.Mangling;
import com.nativelibs4java.runtime.ann.Wide;
import com.nativelibs4java.runtime.structs.Struct;
import java.lang.reflect.Method;
import java.net.URL;

/**
 *
 * @author Olivier Chafik
 */
public class Main {

    @Library("test")
    public static class Test {
        static {
            JNI.registerClass(Test.class);
        }
        //@Library("C:\\Prog\\dyncall\\dyncall\\buildsys\\vs2008\\Debug\\test")
        //@Library("C:\\Users\\Olivier\\Prog\\dyncall\\dyncall\\buildsys\\vs2008\\x64\\Debug\\test")
        @Mangling({"?sinInt@@YANH@Z"})
        public static native double sinInt(int d);

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
        try {
            System.out.println(Main.class.getResource("Main.class"));
            Method[] mes = Test.class.getDeclaredMethods();
            Method me = mes[0];
            //Test.class.getMethod("sinInt", Integer.TYPE)
            long address = DynCall.getSymbolAddress(me);
            JNI.registerClass(Test.class);

            /*
            mes = PerfTest.class.getDeclaredMethods();
            me = mes[0];
            //Test.class.getMethod("sinInt", Integer.TYPE)
            address = DynCall.getSymbolAddress(me);
            JNI.registerClass(PerfTest.class);
*/
            int arg = 10;
            double res = Test.sinInt(arg);
            int nWarmUp = 16000;
            int nCalls = 10000;
            int nTests = 10;
            boolean warmup = true;

            if (true) {
                double tot = 0;
                if (warmup) {
                    for (int i = 0; i < nWarmUp; i++)
                        tot += Test.sinInt(arg);
                    for (int i = 0; i < nWarmUp; i++)
                        tot += Math.sin(arg);
                }

                double totalSlower = 0;
                for (int iTest = 0; iTest < nTests; iTest++) {
                    long startNat = System.nanoTime();
                    for (int i = 0; i < nCalls; i++)
                        tot += Test.sinInt(arg);
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

            if (true) {
                //JNI.registerClass(PerfTest.class);
                //PerfTest.DynCall.testAddDyncall(1, 2);
                int tot = 0, seed = System.getenv().size();
                if (warmup) {
                    for (int i = 0; i < nWarmUp; i++)
                        tot = PerfTest.testAddJNI(tot, seed);
                    for (int i = 0; i < nWarmUp; i++)
                        tot = PerfTest.DynCallTest.testAddDyncall(tot, seed);
                }

                double totalSlower = 0;
                for (int iTest = 0; iTest < nTests; iTest++) {
                    long startJNI = System.nanoTime();
                    for (int i = 0; i < nCalls; i++)
                        tot = PerfTest.testAddJNI(tot, seed);
                    long timeJNI = System.nanoTime() - startJNI;

                    long startDyncall = System.nanoTime();
                    for (int i = 0; i < nCalls; i++)
                        tot = PerfTest.DynCallTest.testAddDyncall(tot, seed);
                    long timeDyncall = System.nanoTime() - startDyncall;

                    //System.out.println("timeNat = " + timeNat);
                    //System.out.println("timePrim = " + timePrim);
                    double slower = (timeDyncall / (double)timeJNI);
                    totalSlower += slower;
                }
                System.out.println("# Dyncall's simple int add is " + (totalSlower / nTests) + " times slower than pure JNI in average");

            }

            if (true) {
                //JNI.registerClass(PerfTest.class);
                //PerfTest.DynCall.testAddDyncall(1, 2);
                int tot = 0, seed = System.getenv().size();
                if (warmup) {
                    for (int i = 0; i < nWarmUp; i++)
                        tot = PerfTest.testASinB(tot, seed);
                    for (int i = 0; i < nWarmUp; i++)
                        tot = PerfTest.DynCallTest.testASinB(tot, seed);
                }

                double totalSlower = 0;
                for (int iTest = 0; iTest < nTests; iTest++) {
                    long startJNI = System.nanoTime();
                    for (int i = 0; i < nCalls; i++)
                        tot = PerfTest.testASinB(tot, seed);
                    long timeJNI = System.nanoTime() - startJNI;

                    long startDyncall = System.nanoTime();
                    for (int i = 0; i < nCalls; i++)
                        tot = PerfTest.DynCallTest.testASinB(tot, seed);
                    long timeDyncall = System.nanoTime() - startDyncall;

                    //System.out.println("timeNat = " + timeNat);
                    //System.out.println("timePrim = " + timePrim);
                    double slower = (timeDyncall / (double)timeJNI);
                    totalSlower += slower;
                }
                System.out.println("# Dyncall's 'a * sin(b)' is " + (totalSlower / nTests) + " times slower than pure JNI in average");

            }
            System.out.println("res = " + res + ", sin(" + arg + ") = " + Math.sin(arg));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println(JNI.SIZE_T_SIZE);
    }

}
