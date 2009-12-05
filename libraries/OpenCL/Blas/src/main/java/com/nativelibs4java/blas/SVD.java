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
public interface SVD<M extends Matrix<M, B>, B extends Buffer> {
	M getS();
	M getV();
	M getU();
	M getCond();
	M getSingularValues();
	M getRank();
	M getNorm2();
}
