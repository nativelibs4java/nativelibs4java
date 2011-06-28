package com.nativelibs4java.opencl.util;

import org.bridj.Pointer;
import static org.bridj.Pointer.*;

import com.nativelibs4java.opencl.JavaCL;

import org.junit.Assert;
import org.junit.Test;

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
				Pointer<Integer> values = random.next();
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
		Assert.assertEquals(piRef, piApprox, 0.001);
	}
}
