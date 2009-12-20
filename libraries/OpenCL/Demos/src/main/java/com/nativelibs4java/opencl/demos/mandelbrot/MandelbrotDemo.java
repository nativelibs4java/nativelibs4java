package com.nativelibs4java.opencl.demos.mandelbrot;

//package bbbob.gparallel.mandelbrot;
import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.demos.SetupUtils;
import com.nativelibs4java.util.IOUtils;
import static com.nativelibs4java.opencl.OpenCL4Java.*;
import com.nativelibs4java.util.NIOUtils;

import java.awt.Color;
import javax.imageio.ImageWriter;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.IOException;
import java.io.File;
import java.nio.IntBuffer;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;


/**
 * Copied and adapted from Bob Boothby's code :
 * http://bbboblog.blogspot.com/2009/10/gpgpu-mandelbrot-with-opencl-and-java.html
 */
public class MandelbrotDemo {
    //boundary of view on mandelbrot set

    public static void main(String[] args) throws IOException, CLBuildException {

        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch(Exception ex) {}
        SetupUtils.failWithDownloadProposalsIfOpenCLNotAvailable();

        //Setup variables for parameters

        //Boundaries
        float realMin = -2.25f; //-0.19920f; //  -2.25
        float realMax = 0.75f; //-0.12954f; //   0.75
        float imaginaryMin = -1.5f; //1.01480f; //  -1.5
        float imaginaryMax = 1.5f; //1.06707f; //   1.5

        //Resolution
        int realResolution = 1640; // TODO validate against device capabilities
        int imaginaryResolution = 1640;

        //The maximum iterations to perform before returning and assigning 0 to a pixel (infinity)
        int maxIter = 64;

        //TODO describe what this number means...
        int magicNumber = 4;

        //Derive the distance in imaginary / real coordinates between adjacent pixels of the image.
        float deltaReal = (realMax - realMin) / (realResolution-1);
        float deltaImaginary = (imaginaryMax - imaginaryMin) / (imaginaryResolution-1);

        //Setup output buffer
        int size = realResolution * imaginaryResolution;
        IntBuffer results = NIOUtils.directInts(size);

        //TODO use an image object directly.
        //CL.clCreateImage2D(context.get(), 0, OpenCLLibrary);
        //TODO set up a Float4 array in order to be able to provide a colour map.
        //This depends on whether we will be able to pass in a Float4 array as an argument in the future.

        //Read the source file.
        String src = IOUtils.readTextClose(MandelbrotDemo.class.getResourceAsStream("Mandelbrot.cl"));

        long time = buildAndExecuteKernel(realMin, imaginaryMin, realResolution, imaginaryResolution, maxIter, magicNumber,
                deltaReal, deltaImaginary, results, src);

		int nPixels = imaginaryResolution * realResolution;
		long timePerPixel = time / nPixels;
		String label = "Computed in " + ((time / 1000)) + " microseconds\n(" + ((timePerPixel)) + " nanoseconds per pixel)";

		BufferedImage image = getImage(realResolution, imaginaryResolution, results);
		outputImage(image);

		JFrame f = new JFrame("JavaCL Mandelbrot Demo");
		f.getContentPane().add("Center", new JScrollPane(new JLabel(new ImageIcon(image))));
		f.getContentPane().add("North", new JLabel(label));
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.pack();
		f.setVisible(true);
    }

	private static BufferedImage getImage(int realResolution, int imaginaryResolution,
                                      IntBuffer results) {
        int[] outputResults = new int[realResolution * imaginaryResolution];
		results.get(outputResults);
		int max = Integer.MIN_VALUE;
		for (int i = outputResults.length; i-- != 0;) {
			int v = outputResults[i];
			if (v > max)
				max = v;
		}

        for (int i = outputResults.length; i-- != 0;) {
			int v = outputResults[i];
			float f = v / (float)max;
			outputResults[i] = Color.HSBtoRGB(1 - f, 0.5f, 0.3f + f * 0.7f);
		}
		
        BufferedImage image = new BufferedImage(realResolution, imaginaryResolution, BufferedImage.TYPE_INT_RGB);
		image.setRGB(0, 0, realResolution, imaginaryResolution, outputResults, 0, realResolution);
        return image;
	}
    private static void outputImage(BufferedImage image) {
        try {
            ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
            ImageOutputStream stream = ImageIO.createImageOutputStream(new File("test.gif"));
            writer.setOutput(stream);
            writer.write(image);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private static long buildAndExecuteKernel(float realMin, float imaginaryMin, int realResolution,
                                              int imaginaryResolution, int maxIter, int magicNumber, float deltaReal,
                                              float deltaImaginary, IntBuffer results, String src) throws CLBuildException, IOException {

        //Create a context and program using the devices discovered.
        CLContext context = createBestContext();
        CLQueue queue = context.createDefaultQueue();

        long startTime = System.nanoTime();
        if (true) {
            Mandelbrot mandelbrot = new Mandelbrot(context);
            mandelbrot.mandelbrot(
                queue,
                new float[] { deltaReal, deltaImaginary },
                new float[] { realMin, imaginaryMin },

                maxIter,
                magicNumber,
                realResolution,
                context.createIntBuffer(CLMem.Usage.Output, results, false),

                new int[]{realResolution, imaginaryResolution},
                new int[]{1,1}
            );
        } else {
            CLProgram program = context.createProgram(src).build();

            //Create a kernel instance from the mandelbrot kernel, passing in parameters.
            CLKernel kernel = program.createKernel(
                    "mandelbrot",
                    new float[] { deltaReal, deltaImaginary },
                    new float[] { realMin, imaginaryMin },

                    maxIter,
                    magicNumber,
                    realResolution,
                    context.createIntBuffer(CLMem.Usage.Output, results, false)
            );

            //Enqueue and complete work using a 2D range of work groups corrsponding to individual pizels in the set.
            //The work groups are 1x1 in size and their range is defined by the desired resolution. This corresponds
            //to one device thread per pixel.

            kernel.enqueueNDRange(queue, new int[]{realResolution, imaginaryResolution}, new int[]{1,1});
        }
        queue.finish();
		long time = System.nanoTime() - startTime;
		return time;
		
    }
}
