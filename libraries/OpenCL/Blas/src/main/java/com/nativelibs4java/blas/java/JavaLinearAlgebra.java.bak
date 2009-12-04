package com.nativelibs4java.opencl.blas;
import com.nativelibs4java.opencl.*;
import com.nativelibs4java.util.*;
import static com.nativelibs4java.opencl.JavaCL.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.*;
import java.util.logging.Level;
import java.util.logging.Logger;
public class JavaLinearAlgebra extends AbstractLinearAlgebra {

    @Override
	public void multiplyNow(Matrix a, Matrix b, Matrix out) {
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
	public void multiplyNow(Matrix a, Vector b, Vector out) {
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
}
