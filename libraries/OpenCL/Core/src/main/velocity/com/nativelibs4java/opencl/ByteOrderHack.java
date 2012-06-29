#parse("main/Header.vm")
package com.nativelibs4java.opencl;

import com.nativelibs4java.util.EnumValue;
import com.nativelibs4java.util.EnumValues;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import com.nativelibs4java.util.IOUtils;
import com.nativelibs4java.util.NIOUtils;

import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import org.bridj.*;
import static org.bridj.Pointer.*;

import java.io.IOException;
import java.nio.*;
import static com.nativelibs4java.opencl.JavaCL.*;
import static com.nativelibs4java.util.NIOUtils.*;
import java.util.*;
import static com.nativelibs4java.opencl.CLException.*;
import org.bridj.ValuedEnum;
import java.util.logging.Level;
import java.util.logging.Logger;

class ByteOrderHack {
	
	public static ByteOrder guessByteOrderNeededForBuffers(CLDevice device) {
		CLPlatform platform = device.getPlatform();
		PlatformUtils.PlatformKind knownPlatform = PlatformUtils.guessPlatformKind(platform);
		if (knownPlatform != PlatformUtils.PlatformKind.AMDApp)
			return device.getByteOrder();
		else
			return checkByteOrderNeededForBuffers(device);
	}
	public static ByteOrder checkByteOrderNeededForBuffers(CLDevice device) {
		CLContext context = JavaCL.createContext((Map)null, device);
		CLQueue queue = context.createDefaultQueue();
		try {
			int n = 16;
			float testValue = 123456789.101112f;
			final int BIG_INDEX = 0, LITTLE_INDEX = 1;
			
			Pointer<Float> inPtr = Pointer.allocateFloats(n);
			inPtr.order(ByteOrder.BIG_ENDIAN).set(BIG_INDEX, testValue);
			inPtr.order(ByteOrder.LITTLE_ENDIAN).set(LITTLE_INDEX, testValue);
			
			CLBuffer<Float> inOut = context.createFloatBuffer(CLMem.Usage.InputOutput, inPtr);
			CLBuffer<Integer> success = context.createIntBuffer(CLMem.Usage.Output, n);
			
			String src =
				"kernel void compare(global float *inout, global int *success) {\n" +
					"int i = get_global_id(0);\n" +
					"success[i] = inout[i] == " + testValue + ";\n" +
					"inout[i] = " + testValue + ";\n" +
				"}";
				
			CLKernel test = context.createProgram(src).createKernel("compare");
			test.setArgs(inOut, success);
			test.enqueueNDRange(queue, new int[] { n }, new int[] { 1 });
			
			Pointer<Integer> successPtr = success.read(queue);
			Pointer<Float> outPtr = inOut.read(queue);
			
			boolean bigOk = successPtr.get(BIG_INDEX) != 0;
			boolean littleOk = successPtr.get(LITTLE_INDEX) != 0;
			
			int index, otherIndex;
			ByteOrder order, otherOrder;
			if (bigOk) {
				order = ByteOrder.BIG_ENDIAN;
				index = BIG_INDEX;
				otherOrder = ByteOrder.LITTLE_ENDIAN;
				otherIndex = LITTLE_INDEX;
			} else {
				order = ByteOrder.LITTLE_ENDIAN;
				index = LITTLE_INDEX;
				otherOrder = ByteOrder.BIG_ENDIAN;
				otherIndex = BIG_INDEX;
			}
			float value = outPtr.order(order).get(index);
			float otherValue = outPtr.order(otherOrder).get(otherIndex);
			
			if (JavaCL.debug)
				System.out.println("[" + device + "] Endianness test: bigOk = " + bigOk + ", littleOk = " + littleOk + "; value = " + value + ", otherValue = " + otherValue);
			
			if (!(bigOk ^ littleOk))
				throw new RuntimeException("[" + device + "] Endianness check failed, kernel recognized both endiannesses...");
			
			{
				if (value != testValue || otherValue == testValue)
					throw new RuntimeException("[" + device + "] Endianness double-check failed, expected " + testValue + " and found " + value + " instead for endianness " + order + " (otherValue = " + otherValue + " for " + otherOrder + ")");
				
				return order;
			}
		} catch (Throwable ex) {
			throw new RuntimeException("[" + device + "] Endianness check failed: " + ex, ex);
		} finally {
			queue.release();
			context.release();
		}
	}
}
