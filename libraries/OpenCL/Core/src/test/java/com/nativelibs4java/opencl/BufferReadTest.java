package com.nativelibs4java.opencl;

import java.nio.LongBuffer;
import java.util.Random;

import junit.framework.TestCase;

import com.nativelibs4java.util.IOUtils;
import com.nativelibs4java.util.NIOUtils;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLDevice;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLLongBuffer;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;

public class BufferReadTest extends TestCase {

       private CLProgram program;
       private CLContext context;
       private CLQueue q;
       private Random r;
       @Override
       protected void setUp() throws Exception {
               super.setUp();
               r = new Random(2011);
               initializeCLStuff();
               compileProgram();
       }

       @Override
       protected void tearDown() throws Exception {
               super.tearDown();
               if(context != null)
                       context.release();
               if(q != null)
                       q.release();
               if(program != null)
                       program.release();
               context = null;
               q = null;
               program = null;
       }

       public void testBufferRead() throws Exception {
               int size = 50;
               CLKernel kernel = program.createKernel("testLongRead");
               CLLongBuffer clInBuff = context.createLongBuffer(CLMem.Usage.Input, size);
               CLLongBuffer clOutBuff = context.createLongBuffer(CLMem.Usage.Output, size);
               long[] longArray = new long[size];
               for(int i = 0; i < longArray.length ; i ++) {
                       longArray[i] = i;
               }
               clInBuff.write(q, LongBuffer.wrap(longArray), true);
               kernel.setArg(0, clInBuff);
               kernel.setArg(1, clOutBuff);
               CLEvent completion = kernel.enqueueNDRange(q, new int[] {size});
               completion.waitFor();


               ///////////////////////////////////////////////////
               //we tried to make a buffer before the clOutBuff.read() so that it isn't creating new ones
               //this way when we're on a loop we won't have to waste memory creating more buffers
               //we thought that the read would simply read everything into the LongBuffer,
               //and since the LongBuffer is wrapped around the outPrim, it would update the values as soon as it is read.
               long[] outPrim = new long[size];
               if (false) {
                   LongBuffer outBuffJava = LongBuffer.wrap(outPrim);
                   clOutBuff.read(q, 0, size,outBuffJava, true);
               } else {
                   LongBuffer outBuffJava = NIOUtils.directLongs(size, context.getByteOrder());
                   clOutBuff.read(q, 0, size,outBuffJava, true);
                   //LongBuffer outBuffJava = clOutBuff.read(q, 0, size);
                   outBuffJava.get(outPrim);
               }
               System.out.println("test1:\nThese should read 1, 2, 3, 4, 5");
               for(int i = 0 ; i < 5 ; i ++) {
                       System.out.print(outPrim[i] + ", ");

               }
               System.out.println();


               ////////////////////////////////////////////////////
               //this is the way that we know works, but it creates a new LongBuffer every read, and we also have to do a
               //outBuffJava2.get(), which forces us to separate the outPrim2 from the java LongBuffer, and require us to create a new array.
               LongBuffer outBuffJava2 = clOutBuff.read(q);
               long[] outPrim2 = new long[size];
               outBuffJava2.get(outPrim2, 0, size);
               System.out.println("test2:\nThese should read 1, 2, 3, 4, 5");
               for(int i = 0 ; i < 5 ; i ++) {
                       System.out.print(outPrim2[i] + ", ");

               }
               System.out.println();

       }

       private void initializeCLStuff() {
               initializeCLContextAndQueueOrNothing(0, 0);
       }
       private void compileProgram() throws Exception{
               String sources = IOUtils.readText(BufferReadTest.class.getResourceAsStream("BufferReadTest.c"));
               program = context.createProgram(sources).build();

       }
       private synchronized void initializeCLContextAndQueueOrNothing(int platformNumber, int deviceNumber) {
               //should check for index out of bounds later.  need to figure out how I should throw the errors
               if(context==null || q == null) {
                       CLDevice device = JavaCL.listPlatforms()[platformNumber].listAllDevices(false)[deviceNumber];
                       System.out.println("Device = " + device);
                       context = (JavaCL.createContext(null, device));
                       q = (context.createDefaultQueue());
               }
       }
}
