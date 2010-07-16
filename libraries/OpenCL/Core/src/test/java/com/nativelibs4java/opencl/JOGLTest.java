/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nativelibs4java.opencl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JFrame;

import org.junit.BeforeClass;
import org.junit.Test;

import com.nativelibs4java.opencl.CLMem.GLObjectInfo;
import com.nativelibs4java.util.NIOUtils;
import com.sun.opengl.util.BufferUtil;
import com.sun.opengl.util.FPSAnimator;

import com.sun.jna.Memory;
import com.sun.jna.PointerUtils;
import java.nio.ByteOrder;

public class JOGLTest {

    @BeforeClass
    public static void initialise() {
        //if (Platform.isWindows())
            System.setProperty("sun.java2d.noddraw","true");
    }
    
    public GLCanvas createGLCanvas(int width, int height) {
        //GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        GLCanvas glCanvas = new GLCanvas(new GLCapabilities(GLProfile.get(GLProfile.GL2)));
        glCanvas.setSize( width, height );
        glCanvas.setIgnoreRepaint( true );

        return glCanvas;
    }

    @Test
    public void testBuffer() {
        if (JavaCL.listGPUPoweredPlatforms().length == 0) {
            System.out.println("#\n# Warning: There is no GPU-powered OpenCL platform available. Skipping test.\n#");
            return;
        }

        try {
            final Semaphore sem = new Semaphore(0);
            JFrame f = new JFrame();
            GLCanvas canvas = createGLCanvas(100, 100);
            f.getContentPane().add("Center", canvas);
            final AssertionError[] err = new AssertionError[1];
            final Throwable[] exx = new Throwable[1];
            canvas.addGLEventListener(new GLEventListener() {

                @Override
                public void init(GLAutoDrawable drawable) {
                    try {
                        CLContext context = JavaCL.createContextFromCurrentGL();
                        assertNotNull(context);
                        if (context != null) {
                            //int glcontext = gl.glGet.getContext().CONTEXT_CURRENT;
                            CLQueue queue = context.createDefaultQueue();

                            System.err.println("Initializing...");
                            int bufferSize = 1024;
                            FloatBuffer buffer;
                            int[] VBO = new int[1];
                            int[] Texture = new int[1];
                            GL gl = drawable.getGL();
                            buffer = BufferUtil.newFloatBuffer(bufferSize);
                            gl.glGenBuffers(1, VBO, 0); // Get A Valid Name
                            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBO[0]); // Bind The Buffer
                            gl.glBufferData(GL.GL_ARRAY_BUFFER, bufferSize * BufferUtil.SIZEOF_FLOAT, buffer, GL2.GL_DYNAMIC_READ);

                            gl.glGenTextures(1, Texture, 0);
                            gl.glBindTexture(GL2.GL_TEXTURE_2D, Texture[0]);
                            int width = 2, height = 2, border = 0;
                            byte bZero = (byte)0, bMin1 = (byte)0xFF;
                            ByteBuffer texData = NIOUtils.directCopy(ByteBuffer.wrap(new byte[] {
                                bMin1,bMin1,bMin1,bMin1, bMin1,bMin1,bMin1,bMin1,
                                bMin1,bMin1,bMin1,bMin1, bMin1,bMin1,bMin1,bMin1
                            }), queue.getDevice().getByteOrder());
                            gl.glTexImage2D (
                                GL2.GL_TEXTURE_2D,
                                0, // no mipmap
                                4, // 4 colours
                                width,
                                height,
                                border,
                                GL2.GL_RGBA,
                                GL2.GL_UNSIGNED_INT_8_8_8_8,
                                texData
                            );
						
                            CLFloatBuffer clbuf = context.createBufferFromGLBuffer(CLMem.Usage.Input, VBO[0]).asCLFloatBuffer();
                            CLImage2D climg = context.createImage2DFromGLTexture2D(CLMem.Usage.InputOutput, CLContext.GLTextureTarget.Texture2D, Texture[0], 0);

                            assertNotNull(clbuf);
                            assertNotNull(climg);
                            
                            queue.enqueueAcquireGLObjects(new CLMem[] { clbuf });
                            queue.enqueueAcquireGLObjects(new CLMem[] { climg });
                            queue.finish();

                            {
                                GLObjectInfo info = clbuf.getGLObjectInfo();
                                assertNotNull(info);
                                assertEquals(CLMem.GLObjectType.Buffer, info.getType());
                                assertEquals(VBO[0], info.getName());
                            }
                            {
                                GLObjectInfo info = climg.getGLObjectInfo();
                                assertNotNull(info);
                                assertEquals(CLMem.GLObjectType.Texture2D, info.getType());
                                assertEquals(Texture[0], info.getName());
                            }
                            
                            float expected = 10;
                            try {
                                CLKernel kernel = context.createProgram("__kernel void fill(__global float* out) { out[get_global_id(0)] = (float)" + expected + ";}").build().createKernel("fill", clbuf);
                                kernel.enqueueNDRange(queue, new int[]{bufferSize}, new int[]{1}).waitFor();
                            } catch (CLException.InvalidKernelArgs ex) {
                                assertTrue("GL-converted buffer was refused as kernel argument", false);
                            } catch (Throwable ex) {
                                assertTrue("Unexpected error : " + ex, false);
                            }

                            //inbuf.put(0, expected);
                            //clbuf.write(queue, 0, 4 * bufferSize, inbuf, true).waitFor();

                            queue.enqueueReleaseGLObjects(new CLMem[] { clbuf });
                            queue.finish();

                            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, VBO[0]); // Bind The Buffer
                            ByteBuffer mb = gl.glMapBuffer(GL.GL_ARRAY_BUFFER, GL2.GL_READ_ONLY);
                            if (mb != null) {
                                buffer = mb.asFloatBuffer();
                                float val = buffer.get(0);
                                assertEquals(expected, val, 0);
                            }
                        }

                    } catch (AssertionError ex) {
                        ex.printStackTrace();
                        err[0] = ex;
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                        exx[0] = ex;
                    } finally {
                        System.out.println("Releasing...");
                        sem.release();
                    }
                }

                public void dispose(GLAutoDrawable glad) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @SuppressWarnings("unused")
				public void displayChanged(GLAutoDrawable glad, boolean bln, boolean bln1) {
                    throw new UnsupportedOperationException("Not supported yet.");
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

            FPSAnimator animator = new FPSAnimator(canvas, 60);
            animator.setRunAsFastAsPossible(true);
            animator.start();

            System.out.println("Acquiring...");
            sem.acquire();
            System.out.println("Acquired !");
            
            f.setVisible(false);
            if (err[0] != null) {
                throw err[0];
            }
            if (err[0] != null) {
                throw exx[0];
            }
        } catch (AssertionError ex) {
        		throw ex;
        } catch (Throwable ex) {
            Logger.getLogger(JOGLTest.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
            assertTrue(ex.toString(), false);
        }
    }
}
