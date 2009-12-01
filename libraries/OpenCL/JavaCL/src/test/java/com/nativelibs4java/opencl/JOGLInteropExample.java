/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;


import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;

import javax.swing.*;
import java.io.IOException;
import java.nio.FloatBuffer;

import com.sun.opengl.util.BufferUtil;
import com.sun.opengl.util.texture.TextureIO;
import javax.media.opengl.awt.GLCanvas;
import org.junit.Test;

public class JOGLInteropExample {
    public static class Renderer implements GLEventListener {

        @Override
        public void init(GLAutoDrawable drawable) {
            
            
        }

        @Override
        public void dispose(GLAutoDrawable gdrawablelad) {}

        @Override
        public void display(GLAutoDrawable drawable) {
            GL gl = drawable.getGL();

        }

        @Override
        public void reshape(GLAutoDrawable drawable, int i, int i1, int i2, int i3) {
            GL gl = drawable.getGL();

        }
        
    }

    GLCanvas createGLCanvas(int width, int height) {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        GLCanvas glCanvas = new GLCanvas(new GLCapabilities());
        glCanvas.setSize( width, height );
        glCanvas.setIgnoreRepaint( true );

        FPSAnimator animator = new FPSAnimator( glCanvas, 60 );
        animator.setRunAsFastAsPossible(false);
    }

    @Test
    public void testBuffer() {
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

                gl.glGenBuffers(1, VBO, 0);							// Get A Valid Name
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBO[0]);			// Bind The Buffer
                gl.glBufferData(GL.GL_ARRAY_BUFFER, bufferSize * BufferUtil.SIZEOF_FLOAT, buffer, GL.GL_STATIC_DRAW);

                CLContext context = JavaCL.createGLCompatibleContext(gl.getContext().CONTEXT_CURRENT, JavaCL.getBestDevice());
            }

            @Override
            public void dispose(GLAutoDrawable drawable) {}

            @Override
            public void display(GLAutoDrawable drawable) {

            }

            @Override
            public void reshape(GLAutoDrawable drawable, int i, int i1, int i2, int i3) {

            }
        });
    }
}
