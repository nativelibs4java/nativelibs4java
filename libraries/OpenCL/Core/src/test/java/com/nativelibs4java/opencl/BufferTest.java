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
import com.sun.jna.Native;
import java.nio.ByteOrder;

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
        CLBuffer<B> buf = context.createBuffer(CLMem.Usage.InputOutput, n, bufferClass);
        assertEquals(n, buf.getElementCount());

        B initial = directBuffer(n, context.getByteOrder(), bufferClass);
        B zeroes = directBuffer(n, context.getByteOrder(), bufferClass);
        for (int i = 0; i < n; i++) {
            put(initial, i, i + 1);
            put(zeroes, i, 0);
        }

        buf.write(queue, initial, true);

        B retrieved = buf.read(queue);
        assertEquals(buf.getElementCount(), retrieved.capacity());

        retrieved.rewind();
        initial.rewind();

        for (int i = 0; i < n; i++)
            assertEquals(bufferClass.getName(), get(initial, i), get(retrieved, i));

        buf.write(queue, zeroOffset, zeroLength, zeroes, true);

        for (boolean direct : new boolean[] { true, false }) {
            String type = direct ? "read to direct buffer" : "read to indirect buffer";
            B readBuffer;
            if (direct)
                readBuffer = retrieved;
            else
                readBuffer = NIOUtils.indirectBuffer(n, bufferClass);
            buf.read(queue, readBuffer, true);
            
            for (int i = 0; i < n; i++) {
                if (i >= zeroOffset && i < (zeroOffset + zeroLength))
                    assertEquals(bufferClass.getName(), get(zeroes, i), get(readBuffer, i));
                else
                    assertEquals(bufferClass.getName(), get(initial, i), get(readBuffer, i));
            }
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
        ByteBuffer data = NIOUtils.directBytes(size, context.getByteOrder());
        CLBuffer<ByteBuffer> buf = context.createBuffer(CLMem.Usage.Input, data, false);
        ByteBuffer mapped = buf.map(queue, CLMem.MapFlags.Read);

        assertEquals(Native.getDirectBufferPointer(data), Native.getDirectBufferPointer(mapped));
    }

}