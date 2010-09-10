/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas.ujmp;

/**
 *
 * @author ochafik
 */
public interface CLDenseMatrix2D<V> {
    CLDenseMatrix2DImpl<V> getImpl();
}