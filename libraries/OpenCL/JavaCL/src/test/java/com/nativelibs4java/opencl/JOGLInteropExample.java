/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;


import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;

import javax.swing.*;
import java.io.IOException;
import java.nio.FloatBuffer;

import com.sun.opengl.util.BufferUtil;
import com.sun.opengl.util.FPSAnimator;
import com.sun.opengl.util.texture.TextureIO;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.Semaphore;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import org.junit.Test;
import static org.junit.Assert.*;

public class JOGLInteropExample {
    
    public GLCanvas createGLCanvas(int width, int height) {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        GLCanvas glCanvas = new GLCanvas(new GLCapabilities(GLProfile.getDefault()));
        glCanvas.setSize( width, height );
        glCanvas.setIgnoreRepaint( true );

        FPSAnimator animator = new FPSAnimator( glCanvas, 60 );
        animator.setRunAsFastAsPossible(false);
        return glCanvas;
    }

    @Test
    public void testBuffer() {
		try {
			final Semaphore sem = new Semaphore(0);
			JFrame f = new JFrame();
			GLCanvas canvas = createGLCanvas(100, 100);
			f.getContentPane().add("Center", canvas);
			canvas.addGLEventListener(new GLEventListener() {

				@Override
				public void init(GLAutoDrawable drawable) {
					int bufferSize = 10;
					FloatBuffer buffer;
					int[] VBO = new int[1];
					GL gl = drawable.getGL();
					buffer = BufferUtil.newFloatBuffer(bufferSize);
					gl.glGenBuffers(1, VBO, 0); // Get A Valid Name
					gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBO[0]); // Bind The Buffer
					gl.glBufferData(GL.GL_ARRAY_BUFFER, bufferSize * BufferUtil.SIZEOF_FLOAT, buffer, GL.GL_STATIC_DRAW);
					CLContext context = JavaCL.createBestGLCompatibleContext(gl.getContext().CONTEXT_CURRENT, JavaCL.getBestDevice());
					CLByteBuffer clbuf = context.createBufferFromGLBuffer(CLMem.Usage.Input, VBO[0]);
					assertNotNull(clbuf);
					assertEquals(bufferSize, clbuf.asCLFloatBuffer().getElementCount());
					sem.release();
				}

				@Override
				public void dispose(GLAutoDrawable drawable) {
				}

				@Override
				public void display(GLAutoDrawable drawable) {
				}

				@Override
				public void reshape(GLAutoDrawable drawable, int i, int i1, int i2, int i3) {
				}
			});
			f.pack();
			f.setVisible(true);
			sem.acquire();
			f.setVisible(false);
		} catch (Throwable ex) {
			Logger.getLogger(JOGLInteropExample.class.getName()).log(Level.SEVERE, null, ex);
			ex.printStackTrace();
			assertTrue(ex.toString(), false);
		}
    }
}
