/*
 * JavaCL - Java API and utilities for OpenCL
 * http://javacl.googlecode.com/
 *
 * Copyright (c) 2009-2010, Olivier Chafik (http://ochafik.free.fr/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Olivier Chafik nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY OLIVIER CHAFIK AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.nativelibs4java.opencl.demos.sobelfilter;

import org.bridj.JNI;
import javax.swing.*;

import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.util.*;
import com.nativelibs4java.opencl.demos.SetupUtils;
import com.nativelibs4java.util.Pair;
import java.awt.image.*;
import java.io.*;

import org.bridj.Platform;
import org.bridj.Pointer;
import static org.bridj.Pointer.*;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import java.awt.FileDialog;
public class SobelFilterDemo {

	static File chooseFile() {
		if (Platform.isMacOSX()) {
			FileDialog d = new FileDialog((java.awt.Frame)null);
			d.setMode(FileDialog.LOAD);
			d.show();
			String f = d.getFile();
			if (f != null)
				return new File(new File(d.getDirectory()), d.getFile());
		} else {
	        JFileChooser chooser = new JFileChooser();
	        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
	        	return chooser.getSelectedFile();
		}
        return null;
	}
     public static void main(String[] args) {
        try {
            SetupUtils.failWithDownloadProposalsIfOpenCLNotAvailable();

            File imageFile = chooseFile();
            if (imageFile == null)
            	return;
            
            BufferedImage image = ImageIO.read(imageFile);
            int width = image.getWidth(), height = image.getHeight();
            //int step = 32;
           // image = image.getSubimage(0, 0, (width / step) * step, (height / step) * step);
            //image = image.getSubimage(0, 0, 512, 512);//(width / step) * step, (height / step) * step);
            

            JFrame f = new JFrame("JavaCL Sobel Filter Demo");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            SobelFilterDemo demo = new SobelFilterDemo();
			Pair<BufferedImage, BufferedImage> imgs = demo.computeSobel(image);
			f.getContentPane().add("Center",
				//new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				//	new JScrollPane(new JLabel(new ImageIcon(image))),
                    new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                        new JScrollPane(new JLabel(new ImageIcon(imgs.getFirst()))),
                        new JScrollPane(new JLabel(new ImageIcon(imgs.getSecond())))
                    ) {/**
						 * 
						 */
						private static final long serialVersionUID = 8267014922143370639L;

					{
                        setResizeWeight(0.5);
                    }}
                //)
			);

            f.pack();
            f.setVisible(true);
        } catch (Throwable th) {
            th.printStackTrace();
            SetupUtils.exception(th);
        }
    }
     
    CLContext context;
    CLQueue queue;
    SimpleSobel sobel;
    ReductionUtils.Reductor<Float> floatMinReductor;
    

    public SobelFilterDemo() throws IOException, CLBuildException {
        context = JavaCL.createBestContext();
        queue = context.createDefaultQueue();
        sobel = new SimpleSobel(context);
        floatMinReductor = ReductionUtils.createReductor(context, ReductionUtils.Operation.Max, OpenCLType.Float, 1);

    }

    public static int roundUp(int group_size, int global_size)
    {
        int r = global_size % group_size;
        return r == 0 ? global_size : global_size + group_size - r;
    }
    public Pair<BufferedImage, BufferedImage> computeSobel(BufferedImage img) throws IOException, CLBuildException {
        int width = img.getWidth(), height = img.getHeight();

        int dataSize = height * width;
        Pointer<Integer> pixels = Pointer.pointerToInts(img.getRGB(0, 0, width, height, null, 0, width));

        CLBuffer<Float>
            gradients = context.createBuffer(CLMem.Usage.InputOutput, Float.class, dataSize),
            directions = context.createBuffer(CLMem.Usage.InputOutput, Float.class, dataSize);

        CLEvent evt = sobel.simpleSobel(queue,
            (CLBuffer)context.createBuffer(CLMem.Usage.Input, pixels, true),
            width,
            height,
            gradients,
            directions,
            new int[] { width, height },
            null//new int[] { 1, 1 }
        );

        //queue.finish();
        
        //float[] test = new float[1000];
        //gradients.read(queue).get(test);

        float gradientMax = floatMinReductor.reduce(queue, gradients, dataSize, 32, evt).get();
        float dirMax = floatMinReductor.reduce(queue, directions, dataSize, 32, evt).get();

        //CLEvent.waitFor(evtGradMax, evtDirMax);

        CLBuffer<Integer> gradientPixels = context.createBuffer(CLMem.Usage.Output, Integer.class, dataSize);
        CLBuffer<Integer> directionPixels = context.createBuffer(CLMem.Usage.Output, Integer.class, dataSize);

        //CLEvent evtGrad =
        sobel.normalizeImage(queue, gradients, gradientMax, (CLBuffer)gradientPixels, new int[] { dataSize }, null);
        //CLEvent evtDir =
        sobel.normalizeImage(queue, directions, dirMax, (CLBuffer)directionPixels, new int[] { dataSize }, null);

        queue.finish();
        
        BufferedImage gradientsImage = getRowsOrderImage(queue, gradientPixels, width, height);//, evtGrad);
        BufferedImage directionsImage = getRowsOrderImage(queue, directionPixels, width, height);//, evtDir);

        return new Pair<BufferedImage, BufferedImage>(gradientsImage, directionsImage);
    }
    static BufferedImage getRowsOrderImage(CLQueue queue, CLBuffer<Integer> buffer, int width, int height, CLEvent... eventsToWaitFor) {
        queue.finish();
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = buffer.read(queue, eventsToWaitFor).getInts(width * height);
        img.setRGB(0, 0, width,height, pixels, 0, width);
        return img;
    }
}

