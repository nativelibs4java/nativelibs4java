/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas;

/**
 *
 * @author Olivier
 */
public abstract class AbstractLinearAlgebra<M extends Matrix, V extends Vector, E extends ComputationEvent> implements LinearAlgebra<M, V, E> {

    protected void waitFor(E... eventsToWaitFor) {
		if (eventsToWaitFor == null || eventsToWaitFor.length == 0)
			return;
		for (E event : eventsToWaitFor) {
			if (event != null)
				event.waitFor();
		}
	}
	
    @Override
    public E multiply(M a, M b, M out, E... eventsToWaitFor) {
        waitFor(eventsToWaitFor);
        multiplyNow(a, b, out);
        return null;
    }

    @Override
    public E multiply(M a, V b, V out, E... eventsToWaitFor) {
        waitFor(eventsToWaitFor);
        multiplyNow(a, b, out);
        return null;
    }

}
