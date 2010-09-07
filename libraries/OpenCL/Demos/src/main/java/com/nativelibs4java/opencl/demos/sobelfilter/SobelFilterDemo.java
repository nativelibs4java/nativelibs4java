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

import org.bridj.JNI;
import javax.swing.*;

import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.util.*;
import com.nativelibs4java.opencl.demos.SetupUtils;
import com.nativelibs4java.util.Pair;
import java.awt.image.*;
import java.io.*;
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
		if (JNI.isMacOSX()) {
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
        floatMinReductor = ReductionUtils.createReductor(context, ReductionUtils.Operation.Max, ReductionUtils.Type.Float, 1);

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
            new int[] { 1, 1 }
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

