/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj;

import com.bridj.JNI;
import com.bridj.DynCall;
import com.bridj.NativeLib;
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
public class PerfLib {
    static {
        try {
            String f = DynCall.getLibFile(PerfLib.class).toString();
            System.load(f);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PerfLib.class.getName()).log(Level.SEVERE, null, ex);
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
