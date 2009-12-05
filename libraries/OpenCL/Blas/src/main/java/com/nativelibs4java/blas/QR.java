/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas;

import java.nio.Buffer;
import java.util.concurrent.Future;

/**
 *
 * @author ochafik
 */
public interface QR<M extends Matrix<M, B>, B extends Buffer> {
	M getH();
	M getQ();
	M getR();
	M solve(M m);
	boolean isFullRank();
}
