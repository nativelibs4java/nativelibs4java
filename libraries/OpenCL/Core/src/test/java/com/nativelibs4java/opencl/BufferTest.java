/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;

import static com.nativelibs4java.util.NIOUtils.directBuffer;
import static com.nativelibs4java.util.NIOUtils.get;
import static com.nativelibs4java.util.NIOUtils.put;
import static org.junit.Assert.assertEquals;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import org.junit.BeforeClass;
import org.junit.Test;

import com.nativelibs4java.test.MiscTestUtils;
import com.nativelibs4java.util.NIOUtils;
import com.bridj.*;
import java.nio.ByteOrder;
import static com.bridj.Pointer.*;

/**
 *
 * @author ochafik
 */
@SuppressWarnings("unchecked")
public class BufferTest extends AbstractCommon {

    @BeforeClass
    public static void setup() {
        MiscTestUtils.protectJNI();
    }

    @Test
    public void testReadWrite() {
        for (Class<? extends Buffer> bufferClass : bufferClasses)
            testReadWrite(bufferClass, 10, 3, 3);
    }
    public <B extends Buffer> void testReadWrite(Class<B> bufferClass, int n, int zeroOffset, int zeroLength) {
        CLBuffer<B> buf = context.createBuffer(CLMem.Usage.InputOutput, bufferClass, n);
        assertEquals(n, buf.getElementCount());

        Pointer<B> initial = allocateArray(bufferClass, n).order(context.getByteOrder());
        Pointer<B> zeroes = allocateArray(bufferClass, n).order(context.getByteOrder());
        for (int i = 0; i < n; i++) {
            initial.set(i, (B)(Object)(i + 1));
        }

        buf.write(queue, initial, true);

        Pointer<B> retrieved = buf.read(queue);
        assertEquals(buf.getElementCount(), retrieved.getRemainingElements());

        for (int i = 0; i < n; i++)
            assertEquals(bufferClass.getName(), initial.get(i), retrieved.get(i));

        buf.write(queue, zeroOffset, zeroLength, zeroes, true);

        buf.read(queue, retrieved, true);
        for (int i = 0; i < n; i++) {
            if (i >= zeroOffset && i < (zeroOffset + zeroLength))
                assertEquals(bufferClass.getName(), zeroes.get(i), retrieved.get(i));
            else
                assertEquals(bufferClass.getName(), initial.get(i), retrieved.get(i));
        }
    }

    Class<? extends Buffer>[] bufferClasses = new Class[] {
        IntBuffer.class,
        LongBuffer.class,
        ShortBuffer.class,
        ByteBuffer.class,
        DoubleBuffer.class,
        //CharBuffer.class,
        FloatBuffer.class
    };
    @Test
    public void testMap() {
        for (Class<? extends Buffer> bufferClass : bufferClasses)
            testMap(bufferClass);
    }
    public void testMap(Class<? extends Buffer> bufferClass) {
        int size = 10;
        Pointer<Byte> data = allocateBytes(size).order(context.getByteOrder());
        CLBuffer<Byte> buf = context.createBuffer(CLMem.Usage.Input, data, false);
        Pointer<Byte> mapped = buf.map(queue, CLMem.MapFlags.Read);

        assertEquals(data, mapped);
    }

}