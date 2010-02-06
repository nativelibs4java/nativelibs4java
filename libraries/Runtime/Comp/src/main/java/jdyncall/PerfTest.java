/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jdyncall;

import com.nativelibs4java.runtime.JNI;
import com.nativelibs4java.runtime.DynCall;
import com.nativelibs4java.runtime.NativeLib;
import com.nativelibs4java.runtime.ann.Library;
//import com.sun.jna.Native;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Olivier
 */
@Library("test")
public class PerfTest {
    static {
        try {
            String f = DynCall.getLibFile(PerfTest.class).toString();
            System.load(f);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PerfTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public static class DynCallTest extends NativeLib {
        public DynCallTest() throws FileNotFoundException {
            super(DynCallTest.class);
        }
        public native int testAddDyncall(int a, int b);
        public native int testASinB(int a, int b);
    }

    public static class JNATest implements com.sun.jna.Library {
        static {
            com.sun.jna.Native.register("test");
        }
        public static native int testAddJNA(int a, int b);
        public static native int testASinB(int a, int b);
    }
    public static native int testAddJNI(int a, int b);
    public static native int testASinB(int a, int b);
}
