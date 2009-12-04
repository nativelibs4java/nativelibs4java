/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas;

/**
 *
 * @author Olivier
 */
public interface LinearAlgebra<M extends Matrix, V extends Vector, E extends ComputationEvent> {
    M newMatrix(int rows, int columns);
    V newVector(int size);

    E multiply(M a, M b, M out, E... eventsToWaitFor);
	E multiply(M a, V b, V out, E... eventsToWaitFor);

    void multiplyNow(M a, M b, M out);
	void multiplyNow(M a, V b, V out);
}
