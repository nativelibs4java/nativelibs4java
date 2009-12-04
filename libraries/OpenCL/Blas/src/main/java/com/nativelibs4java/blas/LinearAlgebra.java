/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas;

/**
 *
 * @author Olivier
 */
public interface LinearAlgebra {
    Matrix newMatrix(int rows, int columns);
    Vector newVector(int size);

    ComputationEvent multiply(Matrix a, Matrix b, Matrix out, ComputationEvent... eventsToWaitFor);
	ComputationEvent multiply(Matrix a, Vector b, Vector out, ComputationEvent... eventsToWaitFor);

    void multiplyNow(Matrix a, Matrix b, Matrix out);
	void multiplyNow(Matrix a, Vector b, Vector out);
}
