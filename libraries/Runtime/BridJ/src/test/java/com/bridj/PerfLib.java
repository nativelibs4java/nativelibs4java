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

/**
 *
 * @author Olivier
 */
@Library("test")
@com.bridj.ann.Runtime(CRuntime.class)
public class PerfLib {
    static {
        String f = BridJ.getNativeLibraryFile(BridJ.getNativeLibraryName(PerfLib.class)).toString();
        System.load(f);
    }
    public static class DynCallTest {
        public DynCallTest() throws FileNotFoundException {
            BridJ.register(getClass());
        }
        public native int testAddDyncall(int a, int b);
        public native int testASinB(int a, int b);
    }

    public static class JNATest implements com.sun.jna.Library {
        static {
        	try {
        		com.sun.jna.Native.register(JNI.extractEmbeddedLibraryResource("test").toString());
        	} catch (Exception ex) {
        		throw new RuntimeException("Failed to initialize test JNA library", ex);
        	}
        }
        public static native int testAddJNA(int a, int b);
        public static native int testASinB(int a, int b);
    }
    public static native int testAddJNI(int a, int b);
    public static native double testASinB(int a, int b);
}
