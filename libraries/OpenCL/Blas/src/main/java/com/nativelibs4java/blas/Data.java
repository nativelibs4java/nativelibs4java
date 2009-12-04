/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas;

/**
 *
 * @author Olivier
 */
public interface Data {
    public enum Usage {
        Read, Write, ReadWrite
    }
    void attach(Usage usage);
    void detach();
}
