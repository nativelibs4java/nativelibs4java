/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas.opencl;

import com.nativelibs4java.blas.LinearAlgebra;
import com.nativelibs4java.blas.Matrix;
import com.nativelibs4java.blas.Vector;
import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.opencl.ReductionUtils;
import com.nativelibs4java.opencl.ReductionUtils.Reductor;
import com.nativelibs4java.util.IOUtils;
import com.nativelibs4java.util.NIOUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.DoubleBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Olivier
 */
public class CLLinearAlgebra<FM extends Matrix<FM, FV, DoubleBuffer>, FV extends Vector<FM, FV, DoubleBuffer>> implements LinearAlgebra<CLMatrix, CLVector> {

    CLContext context;
    CLQueue queue;
    CLProgram multiplyProg;
    CLKernel mulMatKernel, mulVecKernel;

    static final String blas1Source = "Blas1.c";


	LinearAlgebra<FM, FV> fallBackLibrary;
	
    public CLLinearAlgebra(LinearAlgebra<FM, FV> fallBackLibrary) throws IOException, CLBuildException {
        context = JavaCL.createBestContext();
        queue = context.createDefaultQueue();
		this.fallBackLibrary = fallBackLibrary;

        InputStream in = CLLinearAlgebra.class.getResourceAsStream(blas1Source);
        if (in == null)
            throw new FileNotFoundException(blas1Source);

        String source = IOUtils.readText(in);
        multiplyProg = context.createProgram(source).build();
        mulMatKernel = multiplyProg.createKernel("mulMat");
        mulVecKernel = multiplyProg.createKernel("mulVec");
    }

	public static <MM extends Matrix<MM, VV, DoubleBuffer>, VV extends Vector<MM, VV, DoubleBuffer>>
			MM newMatrix(LinearAlgebra<MM, VV> other, CLMatrix m) {
		DoubleBuffer b = NIOUtils.directDoubles(m.size());
		m.read(b);
		MM mm = other.newMatrix(m.getRows(), m.getColumns());
		mm.write(b);
		return mm;
	}

	public static <MM extends Matrix<MM, VV, DoubleBuffer>, VV extends Vector<MM, VV, DoubleBuffer>>
			VV newVector(LinearAlgebra<MM, VV> other, CLVector m) {
		DoubleBuffer b = NIOUtils.directDoubles(m.size());
		m.read(b);
		VV mm = other.newVector(m.size());
		mm.write(b);
		return mm;
	}

    private static final int[] unitIntArr = new int[] { 1 };
    private static final int[] unitInt2Arr = new int[] { 1, 1 };

	synchronized CLEvent multiply(CLMatrix a, CLMatrix b, CLMatrix out, CLEvent... eventsToWaitFor) {
        mulMatKernel.setArgs(
            a.data, a.getRows(), a.getColumns(),
            b.data, b.getRows(), b.getColumns(),
            out.data
        );
        return mulMatKernel.enqueueNDRange(queue, new int[] { out.getRows(), out.getColumns() }, unitInt2Arr, eventsToWaitFor);
    }

    synchronized CLEvent multiply(CLMatrix a, CLVector b, CLVector out, CLEvent... eventsToWaitFor) {
        mulVecKernel.setArgs(
            a.data, a.getRows(), a.getColumns(),
            b.data, b.size(),
            out.data
        );
        return mulVecKernel.enqueueNDRange(queue, new int[] { out.size() }, unitIntArr, eventsToWaitFor);
    }

    synchronized CLEvent dot(CLVector a, CLVector b, CLVector out, CLEvent... eventsToWaitFor) {
		CLEvent.waitFor(eventsToWaitFor);
		a.waitForRead();
		b.waitForRead();
		out.waitForWrite();
		FV aa  = newVector(fallBackLibrary, a);
		FV bb  = newVector(fallBackLibrary, b);
		out.write(aa.dot(bb, null).read());
		return null;
    }

	Reductor<DoubleBuffer> addReductor;
	synchronized Reductor<DoubleBuffer> getAddReductor() {
		if (addReductor == null) {
			try {
				addReductor = ReductionUtils.createReductor(context, ReductionUtils.Operation.Add, ReductionUtils.Type.Double, 1);
			} catch (CLBuildException ex) {
				Logger.getLogger(CLLinearAlgebra.class.getName()).log(Level.SEVERE, null, ex);
				throw new RuntimeException("Failed to create an addition reductor !", ex);
			}
		}
		return addReductor;
	}

    @Override
    public CLMatrix newMatrix(int rows, int columns) {
        return new CLMatrix(this, rows, columns);
    }

    @Override
    public CLVector newVector(int size) {
        return new CLVector(this, size);
    }

}
