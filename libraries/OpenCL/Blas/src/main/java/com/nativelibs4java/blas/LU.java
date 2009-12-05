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
public interface LU<M extends Matrix<M, B>, B extends Buffer> {
	M getPivot();
	M getL();
	M getU();
	M solve(M m);
	M det();
	boolean isNonSingular();
}
