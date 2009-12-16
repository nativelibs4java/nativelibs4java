/*
	Copyright (c) 2009 Olivier Chafik (olivier.chafik@gmail.com)

	This file is part of JavaCL (http://code.google.com/p/javacl/).

	JavaCL is free software: you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation, either version 2.1 of the License, or
	(at your option) any later version.

	JavaCL is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public License
	along with JavaCL.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.nativelibs4java.opencl.demos.sobelfilter;

import java.nio.*;
import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.demos.SetupUtils;
import com.nativelibs4java.util.IOUtils;
import com.nativelibs4java.util.NIOUtils;
import com.ochafik.util.listenable.Pair;
import java.awt.image.*;
import java.io.*;
import java.nio.FloatBuffer;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;


public class SobelFilterDemo {

    CLContext context;
    CLQueue queue;
    SimpleSobel sobel;
    ReductionUtils.Reductor<FloatBuffer> floatMinReductor;
    

    public SobelFilterDemo() throws IOException, CLBuildException {
        context = JavaCL.createBestContext();
        queue = context.createDefaultQueue();
        sobel = new SimpleSobel(context);
        floatMinReductor = ReductionUtils.createReductor(context, ReductionUtils.SimpleOperation.Max, ReductionUtils.Type.Float, 1);

    }

    public Pair<BufferedImage, BufferedImage> computeSobel(BufferedImage img) throws IOException, CLBuildException {
        int width = img.getWidth(), height = img.getHeight();

        int dataSize = height * width;
        int[] pixels = img.getRGB(0, 0, width, height, null, 0, width);

        CLFloatBuffer
            gradients = context.createFloatBuffer(CLMem.Usage.InputOutput, dataSize),
            directions = context.createFloatBuffer(CLMem.Usage.InputOutput, dataSize);

        CLEvent evt = sobel.simpleSobel(queue,
            context.createIntBuffer(CLMem.Usage.Input, IntBuffer.wrap(pixels), true).asCLByteBuffer(),
            width,
            height,
            gradients,
            directions,
            new int[] { width, height },
            new int[] { 1, 1 }
        );

        FloatBuffer gradientMax = NIOUtils.directFloats(1),
                dirMax = NIOUtils.directFloats(1);
        CLEvent evtGradMax = floatMinReductor.reduce(queue, gradients, dataSize, gradientMax, 32, evt);
        CLEvent evtDirMax = floatMinReductor.reduce(queue, directions, dataSize, dirMax, 32, evt);

        CLEvent.waitFor(evtGradMax, evtDirMax);

        CLIntBuffer gradientPixels = context.createIntBuffer(CLMem.Usage.Output, dataSize);
        CLIntBuffer directionPixels = context.createIntBuffer(CLMem.Usage.Output, dataSize);

        CLEvent evtGrad = sobel.normalizeImage(queue, gradients, gradientMax.get(0), gradientPixels.asCLByteBuffer(), new int[] { dataSize }, null);
        CLEvent evtDir = sobel.normalizeImage(queue, directions, dirMax.get(0), directionPixels.asCLByteBuffer(), new int[] { dataSize }, null);

        BufferedImage gradientsImage = getRowsOrderImage(queue, gradientPixels, width, height, pixels, evtGrad);
        BufferedImage directionsImage = getRowsOrderImage(queue, directionPixels, width, height, pixels, evtDir);

        return new Pair<BufferedImage, BufferedImage>(gradientsImage, directionsImage);
    }
    static BufferedImage getRowsOrderImage(CLQueue queue, CLIntBuffer buffer, int width, int height, int[] pixelsTemp, CLEvent... eventsToWaitFor) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = pixelsTemp == null ? new int[width * height] : pixelsTemp;
        buffer.read(queue, eventsToWaitFor).get(pixels);
        img.setRGB(0, 0, width,height, pixels, 0, width);
        return img;
    }
     public static void main(String[] args) {
        try {
            SetupUtils.failWithDownloadProposalsIfOpenCLNotAvailable();

            BufferedImage image = ImageIO.read(SobelFilterDemo.class.getResourceAsStream("test.jpg"));

            JFrame f = new JFrame("JavaCL Sobel Filter Demo");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            SobelFilterDemo demo = new SobelFilterDemo();
			Pair<BufferedImage, BufferedImage> imgs = demo.computeSobel(image);
			f.getContentPane().add("Center",
				new JSplitPane(JSplitPane.VERTICAL_SPLIT,
					new JScrollPane(new JLabel(new ImageIcon(imgs.getFirst()))),
					new JScrollPane(new JLabel(new ImageIcon(imgs.getSecond())))
				)
			);
			
            f.pack();
            f.setVisible(true);
        } catch (Throwable th) {
            th.printStackTrace();
            SetupUtils.exception(th);
        }
    }
}

