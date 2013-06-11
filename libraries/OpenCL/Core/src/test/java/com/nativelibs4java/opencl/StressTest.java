/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nativelibs4java.opencl;

import org.bridj.Pointer;
//import org.junit.Test;

/**


javac -d target/classes -cp target/javacl-core-1.0-SNAPSHOT.jar:/Users/ochafik/.m2/repository/com/nativelibs4java/ochafik-util/0.12-SNAPSHOT/ochafik-util-0.12-SNAPSHOT.jar:/Users/ochafik/.m2/repository/com/nativelibs4java/opencl4java/1.0-SNAPSHOT/opencl4java-1.0-SNAPSHOT.jar:/Users/ochafik/.m2/repository/com/nativelibs4java/bridj/0.7-SNAPSHOT/bridj-0.7-SNAPSHOT.jar src/test/java/com/nativelibs4java/opencl/StressTest.java && java -cp target/classes:target/javacl-core-1.0-SNAPSHOT.jar:/Users/ochafik/.m2/repository/com/nativelibs4java/nativelibs4java-utils/1.6-SNAPSHOT/nativelibs4java-utils-1.6-SNAPSHOT.jar:/Users/ochafik/.m2/repository/com/nativelibs4java/ochafik-util/0.12-SNAPSHOT/ochafik-util-0.12-SNAPSHOT.jar:/Users/ochafik/.m2/repository/com/nativelibs4java/opencl4java/1.0-SNAPSHOT/opencl4java-1.0-SNAPSHOT.jar:/Users/ochafik/.m2/repository/com/nativelibs4java/bridj/0.7-SNAPSHOT/bridj-0.7-SNAPSHOT.jar com.nativelibs4java.opencl.StressTest


*/
public class StressTest {
    // @Test
    public static void main(String[] args) {
        CLContext context = JavaCL.createBestContext(CLPlatform.DeviceFeature.GPU);
        System.out.println(context);
        int n = 128;// * 128;
//        Pointer<Integer> p = Pointer.allocateInts(n);
        for (int i = 0; i < 100000; i++) {
//            if ((i & 0xff) == 0xff) 
                System.out.print(".");
            CLQueue queue = context.createDefaultQueue();
            CLBuffer<Integer> buffer = context.createByteBuffer(CLMem.Usage.Output, 4 * n).as(Integer.class);//p);
            CLProgram program = context.createProgram("kernel void f(global int* input, int n) {\n" +
                    "int i = get_global_id(0);\n" +
                    "if (i >= n) return;\n" +
                    "input[i] = i;\n" +
                    "}");
            CLKernel kernel = program.createKernel("f");
            
            for (int j = 0; j < 100; j++) {
                kernel.setArgs(buffer, n);
                kernel.enqueueNDRange(queue, new int[] { n });
            }
            queue.finish();
            queue.release();
            kernel.release();
            program.release();
            buffer.release();
        }
        context.release();
    }
}
