package com.nativelibs4java.blas.java;
import com.nativelibs4java.blas.LinearAlgebra;
import com.nativelibs4java.blas.Matrix;
import com.nativelibs4java.blas.Vector;
import com.nativelibs4java.opencl.*;
import com.nativelibs4java.util.*;
import static com.nativelibs4java.opencl.JavaCL.*;

public class DefaultLinearAlgebra implements LinearAlgebra<DefaultMatrix, DefaultVector> {

    @Override
    public DefaultMatrix newMatrix(int rows, int columns) {
        return new DefaultMatrix(rows, columns);
    }

    @Override
    public DefaultVector newVector(int size) {
        return new DefaultVector(size);
    }
}
