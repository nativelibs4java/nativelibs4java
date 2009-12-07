/***********************************************************************

 Copyright (c) 2008, 2009, Memo Akten, www.memo.tv
 *** The Mega Super Awesome Visuals Company ***
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
 *     * Neither the name of MSA Visuals nor the names of its contributors
 *       may be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ***********************************************************************/ 

package com.nativelibs4java.opencl.demos;


import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.util.*;
import java.util.logging.*;
import javax.media.opengl.*;

import javax.swing.*;
import java.nio.FloatBuffer;

import com.sun.opengl.util.FPSAnimator;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileReader;
import java.util.Random;
import javax.media.opengl.*;
import static javax.media.opengl.GL2.*;
import javax.media.opengl.awt.*;

/**
 *
 * @author Olivier (ported to JavaCL/OpenCL4Java)
 */
public class ParticlesDemo implements GLEventListener {

    public static GLCanvas createGLCanvas(int width, int height) {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        GLCanvas glCanvas = new GLCanvas(new GLCapabilities(GLProfile.getDefault()));
        glCanvas.setSize( width, height );
        glCanvas.setIgnoreRepaint( true );

        return glCanvas;
    }

    public static void main(String[] args) {
        System.setProperty("sun.java2d.noddraw","true");
        
        JFrame f = new JFrame();
        GLCanvas canvas = createGLCanvas(400, 400);
        f.getContentPane().add("Center", canvas);
        final AssertionError[] err = new AssertionError[1];
        final ParticlesDemo demo = new ParticlesDemo(100000);
        canvas.addGLEventListener(demo);
        canvas.addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {
                demo.mouseX = e.getX();
                demo.mouseY = e.getY();
            }

        });
        
        f.pack();

        FPSAnimator animator = new FPSAnimator(canvas, 60);
        animator.setRunAsFastAsPossible(true);
        animator.start();
        
        f.setVisible(true);

    }

    CLContext context;
    CLQueue queue;

    boolean useOpenGLContext;
    int particlesCount;
    int[] vbo = new int[1];

    float mouseX, mouseY, width, height;
    int iMouseArg, iDimensionsArg;

    CLKernel updateParticleKernel;
    CLFloatBuffer positionsMem;
    FloatBuffer particlesPos;

    public ParticlesDemo(int particlesCount) {
        this.particlesCount = particlesCount;
    }
    @Override
    public void init(GLAutoDrawable glad) {
        try {
            GL2 gl = (GL2)glad.getGL();
            gl.glClearColor(0, 0, 0, 1);
            gl.glClear(GL_COLOR_BUFFER_BIT);
            
            try {
                if (useOpenGLContext) {
                    context = JavaCL.createContextFromCurrentGL();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (context == null) {
                useOpenGLContext = false;
                context = JavaCL.createBestContext();
            }
            queue = context.createDefaultQueue();
            
            particlesPos = NIOUtils.directFloats(particlesCount * 2);
            Random random = new Random(System.nanoTime());

            FloatBuffer masses = NIOUtils.directFloats(particlesCount);
            for (int i = 0; i < particlesCount; i++) {
                //Particle &p = particles[i];
                //p.vel.set(0, 0);
                masses.put(0.5f + 0.5f * random.nextFloat());
                particlesPos.put(random.nextFloat() * 300);
                particlesPos.put(random.nextFloat() * 300);
            }
            masses.rewind();
            particlesPos.rewind();
            
            gl.glGenBuffers(1, vbo, 0);
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
            gl.glBufferData(GL_ARRAY_BUFFER, particlesPos.capacity() * 4, particlesPos, GL_DYNAMIC_COPY);
            gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

            FloatBuffer velocities = NIOUtils.directFloats(2 * particlesCount);
            CLFloatBuffer velocitiesMem = context.createFloatBuffer(Usage.InputOutput, velocities, false);

            if (useOpenGLContext)
                positionsMem = context.createBufferFromGLBuffer(Usage.InputOutput, vbo[0]).asCLFloatBuffer();
            else
                positionsMem = context.createFloatBuffer(Usage.InputOutput, 2 * particlesCount);

            //String src = IOUtils.readText(ParticlesDemo.class.getClassLoader().getResourceAsStream(ParticlesDemo.class.getPackage().getName().replace('.', '/') + "/ParticlesDemo.cl"));
            String src = IOUtils.readText(new File("C:/Users/Olivier/Prog/nativelibs4java/OpenCL/Demos/src/main/resources/com/nativelibs4java/opencl/demos/ParticlesDemo.c"));
            updateParticleKernel = context.createProgram(src).build().createKernel(
                "updateParticle",
                context.createFloatBuffer(Usage.Input, masses, true),
                velocitiesMem,
                positionsMem
            );
            iMouseArg = 3;
            iDimensionsArg = 4;

            updateKernelArgs();

            gl.glPointSize(1);

        } catch (Exception ex) {
            Logger.getLogger(ParticlesDemo.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
        
    }

    @Override
    public void display(GLAutoDrawable glad) {
        GL2 gl = (GL2)glad.getGL();
        gl.glColor3f(1.0f, 1.0f, 1.0f);
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);

        if (useOpenGLContext)
            queue.finish();
        else {
            positionsMem.read(queue, particlesPos, true);
            gl.glBufferSubData(GL_ARRAY_BUFFER, 0, (int)NIOUtils.getSizeInBytes(particlesPos), particlesPos);
        }
        gl.glEnableClientState(GL_VERTEX_ARRAY);
        gl.glVertexPointer(2, GL_FLOAT, 0, 0);
        gl.glDrawArrays(GL_POINTS, 0, particlesCount);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

        //gl.glColor3f(1, 1, 1);
        //String info = "fps: " + ofToString(ofGetFrameRate()) + "\nnumber of particles: " + ofToString(NUM_PARTICLES);
        //ofDrawBitmapString(info, 20, 20);

        updateKernelArgs();
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int width, int height) {
        this.width = width;
        this.height = height;
        
    }

    private void updateKernelArgs() {
        updateParticleKernel.setArg(iMouseArg, new float[] {mouseX, mouseY});
        updateParticleKernel.setArg(iDimensionsArg, new float[] {width, height});

        updateParticleKernel.enqueueNDRange(queue, new int[] { particlesCount }, null);
    }
}
