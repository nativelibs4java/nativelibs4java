package com.nativelibs4java.opencl.demos.random;

import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLIntBuffer;
import com.nativelibs4java.opencl.CLMem.MapFlags;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import com.nativelibs4java.util.NIOUtils;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ochafik
 */
public class ParallelRandomTest {
	int nPoints = 1024 * 1024;
	int nLoops = 10;
	long seed = 1;

	static final int mask = 0x00ffffff;
	static final double divid = (1 << 24);
	//static final int mask = (1 << 30) - 1;
	//static final double divid = (1 << 30);

	/**
	 * http://fr.wikipedia.org/wiki/M%C3%A9thode_de_Monte-Carlo#Exemples
	 */
	@Test
	public void testPICircle() {
		try {
			ParallelRandom random = new ParallelRandom(
				JavaCL.createBestContext().createDefaultQueue(),
				nPoints * 2,
				seed
			);

			int nInside = 0, nTotalPoints = 0;

			for (int iLoop = 0; iLoop < nLoops; iLoop++) {
				IntBuffer values = random.next();
				for (int iPoint = 0; iPoint < nPoints; iPoint++) {
					int offset = iPoint * 2;
					int ix = values.get(offset), iy = values.get(offset + 1);
					float x = (float)((ix & mask) / divid);
					float y = (float)((iy & mask) / divid);

					float dist = x * x + y * y;
					if (dist <= 1)
						nInside++;
				}
				nTotalPoints += nPoints;
				//checkPICircleProba(nInside, nTotalPoints);
			}
			checkPICircleProba(nInside, nTotalPoints);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	void checkPICircleProba(int nInside, int nTotalPoints) {
		double piRef = Math.PI;
		double probaInside = nInside / (double)nTotalPoints; // = Pi / 4
		double piApprox = probaInside * 4;
		double error = Math.abs(piApprox - piRef);
		double relError = error / piRef;
		System.out.println(nInside + " points inside the circle quarter over " + nTotalPoints);
		System.out.println("Approximated PI = " + piApprox);
		System.out.println("   Reference PI = " + piRef);
		System.out.println("\tAbsolute error = " + error);
		System.out.println("\tRelative error = " + (relError * 100) + " %");
		assertEquals(piRef, piApprox, 0.001);
	}
}
