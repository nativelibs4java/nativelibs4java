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

package com.nativelibs4java.opencl.demos.particles;


import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.demos.JavaCLSettingsPanel;
import com.nativelibs4java.opencl.demos.SetupUtils;
import com.nativelibs4java.util.*;
import com.ochafik.util.SystemUtils;
import com.sun.jna.Platform;
import java.io.IOException;
import java.util.logging.*;
import javax.media.opengl.*;

import javax.swing.*;
import java.nio.FloatBuffer;

import com.sun.opengl.util.FPSAnimator;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;
import static javax.media.opengl.GL2.*;
import javax.media.opengl.awt.*;
import javax.media.opengl.glu.GLU;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicMenuUI.ChangeHandler;

/**
 *
 * @author Olivier (ported to JavaCL/OpenCL4Java)
 */
public class ParticlesDemo implements GLEventListener {

    public static Component createGLCanvas(int width, int height, boolean useSwing) {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2));
        Component canvas = useSwing ? new GLJPanel(caps) : new GLCanvas(caps);
        canvas.setSize(width, height);
        canvas.setIgnoreRepaint(true);
        canvas.setPreferredSize(new Dimension(width, height));
        canvas.setSize(new Dimension(width, height));

        return canvas;
    }

    static File lastFile;
    volatile boolean paused;
    final static float DEFAULT_MOUSE_WEIGHT = 0.7f;
    volatile float mouseWeight = DEFAULT_MOUSE_WEIGHT;

    static class ParticlesCountItem {
        public int count;
        public String string;
        public ParticlesCountItem(int count, String string) {
            this.count = count;
            this.string = string;
        }
        @Override
        public String toString() {
            return string;
        }

    }

    public static void main(String[] args) {
        try {
            System.setProperty("sun.java2d.noddraw","true");

            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch(Exception ex) {}
            SetupUtils.failWithDownloadProposalsIfOpenCLNotAvailable();


            JFrame f = new JFrame("JavaCL Particles Demo");
            Box tb = Box.createHorizontalBox();
            final JButton openImage = new JButton("Import"), saveImage = new JButton("Export"), changeBlend = new JButton("Change Blend");
            tb.add(openImage);
            //tb.add(saveImage);
            tb.add(changeBlend);
            //final JCheckBox limi
            final AssertionError[] err = new AssertionError[1];


            final ParticlesDemo demo = new ParticlesDemo();
            final int nSpeeds = 21;

            final JSlider speedSlider = new JSlider(0, nSpeeds - 1);
            speedSlider.setValue(nSpeeds / 2);
            //f.getContentPane().add("West", slider);

            tb.add(speedSlider);
            //slider.setOrientation(JSlider.VERTICAL);
            speedSlider.addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent e) {
                    int d = speedSlider.getValue() - nSpeeds / 2;
                    demo.speedFactor = (d == 0 ? 1 : d > 0 ? d : -1f/d) * DEFAULT_SPEED_FACTOR;
                }

            });

            changeBlend.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    demo.iBlend = (demo.iBlend + 1) % demo.blends.length;
                }

            });

            final Component canvas = createGLCanvas(1000, 800, demo.settings.isDirectGLRendering());
            f.setLayout(new BorderLayout());
            f.add("Center", canvas);

            saveImage.addActionListener(new ActionListener() {

                @SuppressWarnings("deprecation")
				@Override
                public void actionPerformed(ActionEvent ae) {
                    boolean paused = demo.paused;
                    demo.paused = true;

                    BufferedImage im = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics g = im.createGraphics();
                    canvas.paint(g);
                    g.dispose();

                    FileDialog fc = new FileDialog((Frame)null);
                    fc.setMode(FileDialog.SAVE);
                    fc.show();
                    if (fc.getFile() != null) {
                        try {
                            ImageIO.write(im, "jpeg", lastFile = new File(new File(fc.getDirectory()), fc.getFile()));
                        } catch (Exception ex) {
                            ParticlesDemo.exception(ex);
                            Logger.getLogger(ParticlesDemo.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                    demo.paused = paused;
                }

            });

            openImage.addActionListener(new ActionListener() {

                @SuppressWarnings("deprecation")
				@Override
                public void actionPerformed(ActionEvent ae) {
                    boolean paused = demo.paused;
                    demo.paused = true;

                    FileDialog fc = new FileDialog((Frame)null);
                    fc.setMode(FileDialog.LOAD);
                    fc.show();
                    if (fc.getFile() != null) {
                        try {
                            BufferedImage im = ImageIO.read(lastFile = new File(new File(fc.getDirectory()), fc.getFile()));
                            demo.setImage(im);
                        } catch (Exception ex) {
                            ParticlesDemo.exception(ex);
                            Logger.getLogger(ParticlesDemo.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                    demo.paused = paused;
                }

            });

            canvas.addMouseWheelListener(new MouseWheelListener() {

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    if (e.getUnitsToScroll() > 0)
                        for (int i = e.getUnitsToScroll(); i-- != 0;)
                            demo.mouseWeight *= 1.1f;
                    else
                        for (int i = -e.getUnitsToScroll(); i-- != 0;)
                            demo.mouseWeight /= 1.1f;
                }
            });
            canvas.addKeyListener(new KeyAdapter() {

                @Override
                public void keyPressed(KeyEvent ke) {
                    switch (ke.getKeyCode()) {
                        case KeyEvent.VK_SPACE:
                            demo.paused = !demo.paused;
                            break;
                        case KeyEvent.VK_DELETE:
                        case KeyEvent.VK_BACK_SPACE:
                            demo.mouseWeight = 1;
                            break;
                    }
                }



            });
            final JSlider sliderMass = new JSlider(0, nSpeeds - 1);
            sliderMass.setValue(nSpeeds / 2);
            //f.getContentPane().add("East", sliderMass);
            //sliderMass.setOrientation(JSlider.VERTICAL);
            sliderMass.addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent e) {
                    int d = sliderMass.getValue() - nSpeeds / 2;
                    demo.massFactor = (d == 0 ? 1 : d > 0 ? d : -1f/d) * DEFAULT_MASS_FACTOR;
                }

            });
            tb.add(sliderMass);

            f.add("North", tb);

            canvas.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent me) {
                    if (me.getButton() != MouseEvent.BUTTON1 || me.isMetaDown() || me.isControlDown())
                        demo.mouseWeight = 1;
                    else
                        demo.paused = !demo.paused;
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    demo.hasMouse = false;
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    demo.hasMouse = true;
                }




            });
            canvas.addMouseMotionListener(new MouseMotionAdapter() {

                @Override
                public void mouseMoved(MouseEvent e) {
                    demo.mouseX = e.getX();
                    demo.mouseY = e.getY();
                    demo.lastMouseMove = System.currentTimeMillis();
                }

            });

            //f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.addWindowListener(new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });
            f.pack();

            f.setVisible(true);

            FPSAnimator animator;
            if (canvas instanceof GLCanvas) {
                ((GLCanvas)canvas).addGLEventListener(demo);
                animator = new FPSAnimator(((GLCanvas)canvas), 30);
            } else {
                ((GLJPanel)canvas).addGLEventListener(demo);
                animator = new FPSAnimator(((GLJPanel)canvas), 30);
            }

            animator.setRunAsFastAsPossible(true);
            animator.start();
        } catch (Exception ex) {
            exception(ex);
            Logger.getLogger(ParticlesDemo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setImage(BufferedImage image) {
        mouseWeight = DEFAULT_MOUSE_WEIGHT;

        int iWidth = image.getWidth(), iHeight = image.getHeight();
        int[] pixels = image.getRGB(0, 0, iWidth, iHeight, null, 0, iWidth);
        int nPixels = iWidth * iHeight;
        float[] nonEmptyPixelsX = new float[nPixels], nonEmptyPixelsY = new float[nPixels];
        int[] nonEmptyPixels = new int[nPixels];

        int nNonEmptyPixels = 0;
        int hw = iWidth / 2, hh = iHeight / 2;
        for (int iPixel = 0; iPixel < nPixels; iPixel++) {
            int pixel = pixels[iPixel];
            if ((pixel & 0xff000000) != 0)
            {
                int y = iPixel / iWidth, x = iPixel - y * iWidth;
                nonEmptyPixels[nNonEmptyPixels] = pixel;
                nonEmptyPixelsX[nNonEmptyPixels] = x - hw;
                nonEmptyPixelsY[nNonEmptyPixels] = hh - y;
                nNonEmptyPixels++;
            }
        }

        queue.finish();
        
        FloatBuffer positionsView = interleavedColorAndPositionsTemp.asFloatBuffer();
        IntBuffer colorView = interleavedColorAndPositionsTemp.asIntBuffer();
        for (int iPoint = 0; iPoint < particlesCount; iPoint++) {
            int iPixel = (int)(random.nextFloat() * (nNonEmptyPixels - 1));

            velocities.put(iPixel, 0);
            velocities.put(iPixel + 1, 0);

            int colorOffset = iPoint * (elementSize / 4);
            int posOffset = iPoint * (elementSize / 4) + 1;

            colorView.put(colorOffset, nonEmptyPixels[iPixel]);
            positionsView.put(posOffset, nonEmptyPixelsX[iPixel]);
            positionsView.put(posOffset + 1, nonEmptyPixelsY[iPixel]);
        }
        velocities.rewind();
        velocitiesMem.write(queue, velocities, false);

        if (useOpenGLContext)
            interleavedColorAndPositionsMem.acquireGLObject(queue);
        interleavedColorAndPositionsMem.write(queue, interleavedColorAndPositionsTemp, false);
        if (useOpenGLContext)
            interleavedColorAndPositionsMem.releaseGLObject(queue);

        queue.finish();
    }

    CLContext context;
    CLQueue queue;

    boolean useOpenGLContext = false;
    int particlesCount;
    int[] vbo = new int[1];

    static final float DEFAULT_SLOWDOWN_FACTOR = 0.7f;
    static final float DEFAULT_SPEED_FACTOR = 2f, DEFAULT_MASS_FACTOR = 2;
    float mouseX, mouseY, width, height, massFactor = DEFAULT_MASS_FACTOR, speedFactor = DEFAULT_SPEED_FACTOR, slowDownFactor = DEFAULT_SLOWDOWN_FACTOR;
    boolean hasMouse = false;
    boolean limitToScreen = false;

    long lastMouseMove;
    FloatBuffer velocities;
    //CLKernel updateParticleKernel;
    ParticlesDemoProgram particlesProgram;
    CLFloatBuffer massesMem, velocitiesMem;
    CLByteBuffer interleavedColorAndPositionsMem;
    ByteBuffer interleavedColorAndPositionsTemp;

    int elementSize = 4 * 4;//4 + 2 * 4 + 4; // 4 color bytes and 2 position floats, 1 dummy alignment float

    CLByteBuffer colorsMem;

    Random random = new Random(System.nanoTime());
    JavaCLSettingsPanel settings = new JavaCLSettingsPanel();

    public ParticlesDemo() {

        ParticlesCountItem[] items = new ParticlesCountItem[] {
            new ParticlesCountItem(1024, "1K"),
            new ParticlesCountItem(1024 * 10,"10K"),
            new ParticlesCountItem(1024 * 100,"100K"),
            new ParticlesCountItem(1024 * 1000,"1M"),
            new ParticlesCountItem(1024 * 10000,"10M")
        };
        JComboBox cb = new JComboBox(items);
        cb.setSelectedIndex(2);
        JLabel lb = new JLabel("Number of particles");
        Box countPanel = Box.createHorizontalBox();
        SetupUtils.setEtchedTitledBorder(countPanel, "Particles Demo Settings");
        countPanel.add(lb);
        countPanel.add(Box.createHorizontalStrut(5));
        countPanel.add(cb);
        cb.setMinimumSize(new Dimension(100, 10));
        cb.setMaximumSize(new Dimension(200, Integer.MAX_VALUE));
        countPanel.add(Box.createHorizontalGlue());


        //sett.removeOpenGLComponents();

        final JPanel opts = new JPanel(new BorderLayout());
        JLabel detailsLab = new JLabel("<html><body><a href='#'>Advanced OpenCL settings...</a></body>", JLabel.RIGHT);
        detailsLab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        opts.add("Center", detailsLab);
        detailsLab.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                opts.removeAll();
                opts.add("Center", settings);
                opts.invalidate();
                Component c = opts.getParent();
                while (c != null) {
                    if (c instanceof Frame) {
                        ((Frame)c).pack();
                        break;
                    }
                    if (c instanceof JDialog) {
                        ((JDialog)c).pack();
                        break;
                    }
                    c = c.getParent();
                }
            }

        });

        int opt = JOptionPane.showConfirmDialog(null, new Object[] { countPanel, opts }, "JavaCL Particles Demo", JOptionPane.OK_CANCEL_OPTION);
        if (opt != JOptionPane.OK_OPTION)
            System.exit(0);

        useOpenGLContext = settings.isGLSharingEnabled();
        particlesCount = ((ParticlesCountItem)cb.getSelectedItem()).count;
    }

    int[] blends = new int[] {
        GL_ONE_MINUS_SRC_ALPHA,
        GL_ONE,
        GL_ONE_MINUS_DST_ALPHA,
        GL_ONE_MINUS_DST_COLOR,
        GL_ONE_MINUS_SRC_COLOR,
        GL_SRC_ALPHA,
        GL_DST_ALPHA,
        GL_SRC_COLOR,
        GL_DST_COLOR,
    };
    volatile int iBlend = 0;

    public static void exception(Throwable ex) {
        StringWriter sout = new StringWriter();
        ex.printStackTrace(new PrintWriter(sout));
        JOptionPane.showMessageDialog(null, sout.toString(), "[Error] " + ParticlesDemo.class.getSimpleName() + " JavaCL Demo", JOptionPane.ERROR_MESSAGE);
    }
    @Override
    public void init(GLAutoDrawable glad) {
        try {
            GL2 gl = (GL2)glad.getGL();
            gl.glClearColor(0, 0, 0, 1);
            gl.glClear(GL_COLOR_BUFFER_BIT);
            //gl.glViewport(0, 0, (int)width, (int)height);
            gl.glEnable(GL_BLEND);
            gl.glEnable(GL_POINT_SMOOTH);

            try {
                if (useOpenGLContext) {
                    context = JavaCL.createContextFromCurrentGL();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Sharing of context between OpenCL and OpenGL failed.\n" +
                        "The demo will run fine without anyway.", "JavaCL Demo", JOptionPane.OK_OPTION);
            }
            if (context == null) {
                useOpenGLContext = false;
                CLDevice device = settings.getDevice();
                if (device == null)
                    device = JavaCL.getBestDevice();
                context = JavaCL.createContext(null, device);
            }
            queue = context.createDefaultQueue();
            
            FloatBuffer masses = NIOUtils.directFloats(particlesCount, context.getByteOrder());
            velocities = NIOUtils.directFloats(2 * particlesCount, context.getByteOrder());
            interleavedColorAndPositionsTemp = NIOUtils.directBytes(elementSize * particlesCount, context.getByteOrder());
            
            FloatBuffer positionsView = interleavedColorAndPositionsTemp.asFloatBuffer();
            for (int i = 0; i < particlesCount; i++) {
                masses.put(0.5f + 0.5f * random.nextFloat());
                
			    velocities.put((random.nextFloat() - 0.5f) * 0.2f);
                velocities.put((random.nextFloat() - 0.5f) * 0.2f);

                int colorOffset = i * elementSize;
                int posOffset = i * (elementSize / 4) + 1;

                byte r = (byte)128, g = r, b = r, a = r;
                interleavedColorAndPositionsTemp.put(colorOffset++, r);
                interleavedColorAndPositionsTemp.put(colorOffset++, g);
                interleavedColorAndPositionsTemp.put(colorOffset++, b);
                interleavedColorAndPositionsTemp.put(colorOffset, a);
                
                float x = (random.nextFloat() - 0.5f) * 200,
                        y = (random.nextFloat() - 0.5f) * 200;

                positionsView.put(posOffset, x);
                positionsView.put(posOffset + 1, y);

            }
            velocities.rewind();
            masses.rewind();
            interleavedColorAndPositionsTemp.rewind();
            
            velocitiesMem = context.createFloatBuffer(Usage.InputOutput, velocities, false);
            massesMem = context.createFloatBuffer(Usage.Input, masses, true);

            gl.glGenBuffers(1, vbo, 0);
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
            gl.glBufferData(GL_ARRAY_BUFFER, (int) NIOUtils.getSizeInBytes(interleavedColorAndPositionsTemp), interleavedColorAndPositionsTemp, GL_DYNAMIC_COPY);
            gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

            if (useOpenGLContext) {
                interleavedColorAndPositionsMem = context.createBufferFromGLBuffer(Usage.InputOutput, vbo[0]);
            } else
                interleavedColorAndPositionsMem = context.createByteBuffer(Usage.InputOutput, interleavedColorAndPositionsTemp, false);
                
            String hsv2rgbSrc = IOUtils.readText(ParticlesDemo.class.getResourceAsStream("HSVtoRGB.c"));
            //String src = IOUtils.readText(ParticlesDemo.class.getResourceAsStream("ParticlesDemo.c"));
            CLProgram program = context.createProgram(hsv2rgbSrc);
            particlesProgram = new ParticlesDemoProgram(program);
            //updateParticleKernel = program.build().createKernel("updateParticle");

            updateKernelArgs();

            gl.glPointSize(2f);

        } catch (Exception ex) {
            Logger.getLogger(ParticlesDemo.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
            exception(ex);
            System.exit(1);
        }
    }

    @Override
    public void dispose(GLAutoDrawable glad) {
        
    }

    @Override
    public void display(GLAutoDrawable glad) {

        GL2 gl = (GL2)glad.getGL();
        
        gl.glBlendFunc(GL_SRC_ALPHA, blends[iBlend]);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        new GLU().gluOrtho2D(-width / 2 - 1, width / 2 + 1, -height/2 - 1, height/2 + 1);
        gl.glMatrixMode(GL2.GL_MODELVIEW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);

        if (useOpenGLContext) {
            queue.finish();
        } else {
            //interleavedColorAndPositionsMem.map(queue, CLMem.MapFlags.Read);
            interleavedColorAndPositionsMem.read(queue, interleavedColorAndPositionsTemp, true);
            gl.glBufferSubData(GL_ARRAY_BUFFER, 0, (int)NIOUtils.getSizeInBytes(interleavedColorAndPositionsTemp), interleavedColorAndPositionsTemp);
            //interleavedColorAndPositionsMem.unmap(queue, interleavedColorAndPositionsTemp);
        }

        gl.glClear(GL_COLOR_BUFFER_BIT);

        gl.glColor3f(1.0f, 1.0f, 1.0f);
        //gl.glEnableClientState(GL_VERTEX_ARRAY);
        //gl.glEnableClientState(GL_COLOR_ARRAY);

        //gl.glColorPointer(4, GL_UNSIGNED_BYTE, elementSize,
        gl.glInterleavedArrays(GL_C4UB_V2F, elementSize, 0);
        
        gl.glDrawArrays(GL_POINTS, 0, particlesCount);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

        if (!paused)
            updateKernelArgs();
    }

    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int width, int height) {
        this.width = width;
        this.height = height;
        
    }

    private synchronized void updateKernelArgs() {
        if (useOpenGLContext)
            interleavedColorAndPositionsMem.acquireGLObject(queue);

        try {
            CLEvent evt = particlesProgram.updateParticle(
                queue,
                massesMem,
                velocitiesMem,
                interleavedColorAndPositionsMem.asCLFloatBuffer(),
                new float[] {mouseX - width / 2f, height / 2f - mouseY},
                new float[] {width, height},
                massFactor,
                speedFactor,
                slowDownFactor,
                hasMouse ? mouseWeight : 0,
                (byte)(limitToScreen ? 1 : 0),
                new int[] { particlesCount }, null
            );
            evt.release(); // the gc might be to slow to reclaim the event, so do manual memory management here

        } catch (Throwable ex) {
            exception(ex);
            System.exit(1);
        }

        if (useOpenGLContext)
            interleavedColorAndPositionsMem.releaseGLObject(queue);
    }
}
