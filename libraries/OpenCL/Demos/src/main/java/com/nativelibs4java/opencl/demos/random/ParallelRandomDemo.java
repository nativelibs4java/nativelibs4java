/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.demos.random;

import com.nativelibs4java.opencl.util.ParallelRandom;
import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLMem.MapFlags;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;
import java.io.IOException;
import java.nio.ByteOrder;
import org.bridj.Pointer;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ochafik
 */
public class ParallelRandomDemo {

    private static void println(Pointer<Integer> b) {
        for (int i = 0, n = (int)b.getValidElements(); i < n; i++) {
            if (i > 0)
                System.out.print(", ");
            System.out.print(b.get(i));

            if (i > 32)
                System.out.print("...");
        }
        System.out.println();
    }

    static class Stat {
        long total;
        long times;
        boolean skippedFirst;
        void add(long value) {
            if (!skippedFirst) {
                skippedFirst = true;
                return;
            }

            total += value;
            times++;
        }
        long average() {
            return times == 0 ? 0 : total / times;
        }
    }
    public static void main(String[] args) {
        try {
            CLContext context = JavaCL.createBestContext();
            CLQueue queue = context.createDefaultQueue();

            int warmupSize = 16;
            ParallelRandom demo = new ParallelRandom(queue, warmupSize, System.currentTimeMillis());

            println(demo.getSeeds().read(queue));
            Pointer<Integer> b = demo.next();
            println(b);
            b = demo.next();
            println(b);
            b = demo.next();
            println(b);

            gc();

            long start = System.nanoTime();
            int loops = 2800;
            Random random = new Random();
            int res = 0;
            for (int i = loops; i-- != 0;) {
                //demo.next(b);
                //demo.next();
                demo.doNext();
                res |= random.nextInt();
            }
            demo.getQueue().finish();
            long time = System.nanoTime() - start;

            System.err.println("Warmup took " + time + " ns");


            Stat stat;


            int testSize = 1024 * 1024;//1024 * 1024;
            int testLoops = 10;

            System.err.println("n = " + testSize);
            demo = new ParallelRandom(queue, testSize, System.currentTimeMillis());
            b = demo.next();

            gc();

            stat = new Stat();
            for (int iTest = 0; iTest < testLoops; iTest++) {
                start = System.nanoTime();
                //b = demo.next();//b);
                demo.doNext();
                demo.getQueue().finish();
                time = System.nanoTime() - start;
                stat.add(time);
                //System.err.println("[OpenCL] Cost per random number = " + (time / (double)testSize) + " ns");

            }
            long avgCL = stat.average();
            System.err.println("[OpenCL] Avg Cost per random number = " + (stat.average() / (double)testSize) + " ns");
            System.err.println();

            gc();

            stat = new Stat();
            for (int iTest = 0; iTest < testLoops; iTest++) {
                start = System.nanoTime();
                for (int i = testSize; i-- != 0;)
                    res |= random.nextInt();
                time = System.nanoTime() - start;
                stat.add(time);
                //System.err.println("[Random.nextInt()] Cost per random number = " + (time / (double)testSize) + " ns");
            }
            long avgJava = stat.average();
            System.err.println("[Random.nextInt()] Avg Cost per random number = " + (stat.average() / (double)testSize) + " ns");
            System.err.println();

            double timesCLFasterThanJava = avgJava / (double)avgCL;
            System.err.println("Java / CL (avg) = " + timesCLFasterThanJava);
            System.err.println(res); // make sure no brutal optimization happens
            System.err.println(b.get(0)); // make sure no brutal optimization happens
            //println(b);

        } catch (IOException ex) {
            Logger.getLogger(ParallelRandom.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    static void gc() {
        try {
            System.gc();
            Thread.sleep(200);
            System.gc();
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            Logger.getLogger(ParallelRandom.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
