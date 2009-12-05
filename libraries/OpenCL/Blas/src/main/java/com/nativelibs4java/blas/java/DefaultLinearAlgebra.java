package com.nativelibs4java.blas.java;
import com.nativelibs4java.blas.AbstractLinearAlgebra;
import com.nativelibs4java.blas.LinearAlgebra;
import com.nativelibs4java.blas.Matrix;
import com.nativelibs4java.opencl.*;
import com.nativelibs4java.util.*;
import static com.nativelibs4java.opencl.JavaCL.*;

public class DefaultLinearAlgebra extends AbstractLinearAlgebra<DefaultMatrix> {

	private DefaultLinearAlgebra() {}
	private volatile static DefaultLinearAlgebra instance;
	public static synchronized DefaultLinearAlgebra getInstance() {
		if (instance == null) {
			synchronized(DefaultLinearAlgebra.class) {
				if (instance == null)
					instance = new DefaultLinearAlgebra();
			}
		}
		return instance;
	}
	
    @Override
    public DefaultMatrix newMatrix(int rows, int columns) {
        return new DefaultMatrix(rows, columns);
    }

}
