/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas;

/**
 *
 * @author Olivier
 */
public abstract class AbstractLinearAlgebra implements LinearAlgebra {

    public static void waitFor(ComputationEvent... eventsToWaitFor) {
		if (eventsToWaitFor == null || eventsToWaitFor.length == 0)
			return;
		for (ComputationEvent event : eventsToWaitFor) {
			if (event != null)
				event.waitFor();
		}
	}
	
    @Override
    public Matrix newMatrix(int rows, int columns) {
        return new Matrix(rows, columns);
    }

    @Override
    public Vector newVector(int size) {
        return new Vector(size);
    }

    @Override
    public ComputationEvent multiply(Matrix a, Matrix b, Matrix out, ComputationEvent... eventsToWaitFor) {
        waitFor(eventsToWaitFor);
        multiplyNow(a, b, out);
        return null;
    }

    @Override
    public ComputationEvent multiply(Matrix a, Vector b, Vector out, ComputationEvent... eventsToWaitFor) {
        waitFor(eventsToWaitFor);
        multiplyNow(a, b, out);
        return null;
    }

    @Override
    public abstract void multiplyNow(Matrix a, Matrix b, Matrix out);

    @Override
    public abstract void multiplyNow(Matrix a, Vector b, Vector out);

}
