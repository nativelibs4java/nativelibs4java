package com.nativelibs4java.blas.java;
import com.nativelibs4java.blas.AbstractLinearAlgebra;
import com.nativelibs4java.blas.DummyComputationEvent;
import com.nativelibs4java.blas.Matrix;
import com.nativelibs4java.blas.Vector;
import com.nativelibs4java.opencl.*;
import com.nativelibs4java.util.*;
import static com.nativelibs4java.opencl.JavaCL.*;

public class JavaLinearAlgebra extends AbstractLinearAlgebra<DefaultMatrix, DefaultVector, DummyComputationEvent> {

    @Override
	public void multiplyNow(DefaultMatrix a, DefaultMatrix b, DefaultMatrix out) {
		assert out.getRows() == a.getRows();
		assert out.getColumns() == b.getColumns();
		assert a.getColumns() == b.getRows();
		
		
		for (int i = 0; i < a.getRows(); i++) {
			for (int j = 0; j < b.getColumns(); j++) {
				double sum = 0;
				for (int k = 0; k < a.getColumns(); k++) {
					sum += a.get(i, k) * b.get(k, j);
				}
				out.set(i, j, sum);
			}
		}
	}
	
	@Override
	public void multiplyNow(DefaultMatrix a, DefaultVector b, DefaultVector out) {
		assert out.size() == a.getRows();
		assert a.getColumns() == b.size();
		for (int i = 0; i < a.getRows(); i++) {
			double sum = 0;
			for (int k = 0; k < a.getColumns(); k++) {
				sum += a.get(i, k) * b.get(k);
			}
			out.set(i, sum);
		}
	}

    @Override
    public DefaultMatrix newMatrix(int rows, int columns) {
        return new DefaultMatrix(rows, columns);
    }

    @Override
    public DefaultVector newVector(int size) {
        return new DefaultVector(size);
    }
}
