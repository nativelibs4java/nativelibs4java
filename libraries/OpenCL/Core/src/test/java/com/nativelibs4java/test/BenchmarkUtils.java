/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.test;

/**
 *
 * @author ochafik
 */
public class BenchmarkUtils {
	public static void gc() {
        try {
            System.gc();
            Thread.sleep(200);
            System.gc();
            Thread.sleep(200);
        } catch (InterruptedException ex) {}
    }
    
}
