/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;

import static com.nativelibs4java.util.NIOUtils.directBuffer;
import static com.nativelibs4java.util.NIOUtils.get;
import static com.nativelibs4java.util.NIOUtils.put;
import static org.junit.Assert.assertEquals;

import java.nio.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.nativelibs4java.test.MiscTestUtils;
import com.nativelibs4java.util.NIOUtils;
import org.bridj.*;
import java.nio.ByteOrder;
import static org.bridj.Pointer.*;
import java.nio.ByteOrder;
import java.util.List;
import org.junit.runners.Parameterized;

/**
 *
 * @author ochafik
 */
@SuppressWarnings("unchecked")
public class BufferTest extends AbstractCommon {
    public BufferTest(CLDevice device) {
        super(device);
    }
    
    @Parameterized.Parameters
    public static List<Object[]> getDeviceParameters() {
        return AbstractCommon.getDeviceParameters();
    }

    static ByteOrder otherOrder(ByteOrder o) {
    	return o.equals(ByteOrder.BIG_ENDIAN) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testMismatchingOrder() {
    	context.createBuffer(CLMem.Usage.InputOutput, allocateFloats(10).order(otherOrder(context.getByteOrder())));
    }
    @Test
    public void testMismatchingByteOrder() {
        context.createBuffer(CLMem.Usage.InputOutput, allocateBytes(10).order(otherOrder(context.getByteOrder())));
    }
    
    @Test
    public void testReadWrite() {
        for (Class<?> bufferClass : bufferClasses)
            testReadWrite(bufferClass, 10, 3, 3);
    }
    public <B> void testReadWrite(Class<B> bufferClass, int n, int zeroOffset, int zeroLength) {
        CLBuffer<B> buf = context.createBuffer(CLMem.Usage.InputOutput, bufferClass, n);
        assertEquals(n, buf.getElementCount());

        Pointer<B> initial = allocateArray(bufferClass, n).order(context.getByteOrder());
        Pointer<B> zeroes = allocateArray(bufferClass, n).order(context.getByteOrder());
        for (int i = 0; i < n; i++) {
        		int v = i + 1;
        		Object value = null;
        		if (bufferClass == Integer.class)
        			value = (Object)v;
            else if (bufferClass == Long.class)
        			value = (Object)(long)v;
            else if (bufferClass == Short.class)
        			value = (Object)(short)v;
            else if (bufferClass == Byte.class)
        			value = (Object)(byte)v;
            else if (bufferClass == Double.class)
        			value = (Object)(double)v;
            else if (bufferClass == Float.class)
        			value = (Object)(float)v;
            else if (bufferClass == Boolean.class)
        			value = (Object)(v != 0);
            else if (bufferClass == Character.class)
        			value = (Object)(char)v;
        		else
        			throw new RuntimeException();
            initial.set(i, (B)value);
        }

        buf.write(queue, initial, true);

        Pointer<B> retrieved = buf.read(queue);
        assertEquals(buf.getElementCount(), retrieved.getValidElements());

        for (int i = 0; i < n; i++)
            assertEquals(bufferClass.getName(), initial.get(i), retrieved.get(i));

        buf.write(queue, zeroOffset, zeroLength, zeroes, true);

        boolean direct = true;
            String type = direct ? "read to direct buffer" : "read to indirect buffer";
            Pointer<B> readBuffer;
            //if (direct)
                readBuffer = retrieved;
            //else
            //    readBuffer = NIOUtils.indirectBuffer(n, bufferClass);
            buf.read(queue, readBuffer, true);
            
            for (int i = 0; i < n; i++) {
                if (i >= zeroOffset && i < (zeroOffset + zeroLength))
                    assertEquals(bufferClass.getName(), zeroes.get(i), readBuffer.get(i));
                else
                    assertEquals(bufferClass.getName(), initial.get(i), readBuffer.get(i));
            }
    }

    Class<?>[] bufferClasses = new Class[] {
        Integer.class,
        Long.class,
        Short.class,
        Byte.class,
        Double.class,
        Float.class
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
        for (Class<?> bufferClass : bufferClasses)
            testMap(bufferClass);
    }
    public <T> void testMap(Class<T> bufferClass) {
        int size = 10;
        Pointer<Byte> data = allocateBytes(size).order(context.getByteOrder());
        CLBuffer<T> buf = context.createBuffer(CLMem.Usage.Input, data, false).as(bufferClass);
        Pointer<T> mapped = buf.map(queue, CLMem.MapFlags.Read);

        assertEquals(data, mapped);
    }

}