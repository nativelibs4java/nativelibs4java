/*
	Copyright (c) 2009
		Sascha Meudt (sascha.meudt@uni-ulm.de) and
		Universitat Ulm (http://www.uni-ulm.de/) and
        Olivier Chafik (olivier.chafik@gmail.com)

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
import java.awt.image.*;
import java.io.*;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import static com.nativelibs4java.util.NIOUtils.*;


public class SobelFilterDemo {

     public static void main(String[] args) {
        try {
            SetupUtils.failWithDownloadProposalsIfOpenCLNotAvailable();

            BufferedImage image = ImageIO.read(SobelFilterDemo.class.getResourceAsStream("test2.jpg"));

            JFrame f = new JFrame("JavaCL Sobel Filter Demo");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            f.getContentPane().add("Center",
                new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                    new JScrollPane(new JLabel(new ImageIcon(image))),
                    new JScrollPane(new JLabel(new ImageIcon(computeSobel(image))))
                )
            );
            f.pack();
            f.setVisible(true);
        } catch (Throwable th) {
            SetupUtils.exception(th);
        }
    }

    public static BufferedImage computeSobel(BufferedImage img) throws IOException, CLBuildException {
        int width = img.getWidth(), height = img.getHeight();

        int dataSize = height * width;
        int[] pixels = img.getRGB(0, 0, width, height, null, 0, width);
        
        CLContext context = JavaCL.createBestContext();
        CLQueue queue = context.createDefaultQueue();

        String src = IOUtils.readTextClose(SobelFilterDemo.class.getResourceAsStream("SobelFilter.cl"));
        CLKernel kernel = context.createProgram(src).build().createKernel("ckSobel");

        CLIntBuffer memIn = context.createIntBuffer(CLMem.Usage.Input, IntBuffer.wrap(pixels), true);
        CLIntBuffer memOut = context.createIntBuffer(CLMem.Usage.Output, dataSize);

        float threshold = 80.0f;
        /*
         * ckSobel(__global uchar4* uc4Source, __global unsigned int* uiDest,
                  __local uchar4* uc4LocalData,
                  unsigned int uiWidth, unsigned int uiHeight, float fThresh)
         */
        kernel.setArgs(memIn, memOut, new CLKernel.LocalSize(108), width, height, threshold);

        long[] maxLocalWS = queue.getDevice().getMaxWorkItemSizes();
        int[] localWorkSizes = new int[] { Math.min(16, (int)maxLocalWS[0]), Math.min(4, (int)maxLocalWS[1])};

        int[] globalWorkSizes = new int[] {
            shrRoundUp(localWorkSizes[0], width),
            shrRoundUp(localWorkSizes[1], height)
        };
        System.out.println("Global work size = " + Arrays.toString(globalWorkSizes));

        CLEvent evt = kernel.enqueueNDRange(queue, globalWorkSizes, localWorkSizes);
        memOut.read(queue, evt).get(pixels);
        
        BufferedImage out =  new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        out.setRGB(0, 0, width,height, pixels, 0, width);
        return out;
    }
    

    public static int shrRoundUp(int group_size, int global_size)
    {
        int r = global_size % group_size;
        if(r == 0)
        {
            return global_size;
        } else
        {
            return global_size + group_size - r;
        }
    }
}

