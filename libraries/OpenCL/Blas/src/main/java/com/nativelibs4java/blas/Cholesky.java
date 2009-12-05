/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas;

import java.nio.Buffer;

/**
 *
 * @author ochafik
 */
public interface Cholesky<M extends Matrix<M, B>, B extends Buffer> {
	M getL();
	boolean isSPD();
	M solve(M m);
}
