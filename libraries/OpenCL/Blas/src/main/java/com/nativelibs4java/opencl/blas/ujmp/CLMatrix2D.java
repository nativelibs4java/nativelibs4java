/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nativelibs4java.opencl.blas.ujmp;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import java.nio.Buffer;

/**
 *
 * @author ochafik
 */
public interface CLMatrix2D<T> {
    CLEvents getEvents();
    CLBuffer<T> getBuffer();
    CLContext getContext();
    long getRowCount();
    long getColumnCount();
    CLMatrix2D<T> blankClone();
}
