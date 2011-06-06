/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;

import static com.nativelibs4java.util.NIOUtils.directBuffer;
import static com.nativelibs4java.util.NIOUtils.getPrimitiveClass;
import static com.nativelibs4java.util.NIOUtils.getBufferClass;
import static com.nativelibs4java.util.NIOUtils.get;
import static com.nativelibs4java.util.NIOUtils.put;
import static org.junit.Assert.assertEquals;

import java.nio.*;

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
        for (Class<? extends Buffer> primClass : primClasses)
            testReadWrite(primClass, 10, 3, 3);
    }
    public <N> void testReadWrite(Class<N> primClass, int n, int zeroOffset, int zeroLength) {
        Class bufferClass = getBufferClass(primClass);
        CLBuffer<N> buf = context.createBuffer(CLMem.Usage.InputOutput, n, primClass);
        assertEquals(n, buf.getElementCount());

        Buffer initial = directBuffer(n, context.getByteOrder(), bufferClass);
        Buffer zeroes = directBuffer(n, context.getByteOrder(), bufferClass);
        for (int i = 0; i < n; i++) {
            put(initial, i, i + 1);
            put(zeroes, i, 0);
        }

        buf.write(queue, initial, true);

        Buffer retrieved = buf.read(queue);
        assertEquals(buf.getElementCount(), retrieved.capacity());

        retrieved.rewind();
        initial.rewind();

        for (int i = 0; i < n; i++)
            assertEquals(bufferClass.getName(), get(initial, i), get(retrieved, i));

        buf.write(queue, zeroOffset, zeroLength, zeroes, true);

        for (boolean direct : new boolean[] { true, false }) {
            String type = direct ? "read to direct buffer" : "read to indirect buffer";
            Buffer readBuffer;
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
        CharBuffer.class,
        FloatBuffer.class
    };
    Class<? extends Buffer>[] primClasses = new Class[] {
        Integer.class,
        Long.class,
        Short.class,
        Byte.class,
        Double.class,
        Character.class,
        Float.class
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