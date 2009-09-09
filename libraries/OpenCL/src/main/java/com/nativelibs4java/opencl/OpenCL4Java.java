package com.nativelibs4java.opencl;

import com.nativelibs4java.opencl.OpenCLLibrary;
import static com.nativelibs4java.opencl.OpenCLLibrary.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.ochafik.util.string.StringUtils;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.DoubleByReference;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.ptr.ShortByReference;

/**
 * Object-oriented wrappers around most common OpenCL structures and operations
 * @see https://developer.apple.com/mac/library/documentation/Performance/Conceptual/OpenCL_MacProgGuide/TheOpenCLWorkflow/TheOpenCLWorkflow.html
 * @author Olivier Chafik
 */
public class OpenCL4Java {

    public static final OpenCLLibrary CL = OpenCLLibrary.INSTANCE;

    public static IntBuffer intBuffer(int size) {
        return ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
    }

    public static LongBuffer longBuffer(int size) {
        return ByteBuffer.allocateDirect(size * 8).order(ByteOrder.nativeOrder()).asLongBuffer();
    }

    public static ShortBuffer shortBuffer(int size) {
        return ByteBuffer.allocateDirect(size * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
    }

    public static ByteBuffer byteBuffer(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    public static FloatBuffer floatBuffer(int size) {
        return ByteBuffer.allocateDirect(size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    public static DoubleBuffer doubleBuffer(int size) {
        return ByteBuffer.allocateDirect(size * 8).order(ByteOrder.nativeOrder()).asDoubleBuffer();
    }

    static abstract class CLEntity<T extends PointerType> {

        private T entity;

        public CLEntity(T entity) {
            if (entity == null) {
                throw new IllegalArgumentException("Null OpenCL " + getClass().getSimpleName() + " !");
            }
            this.entity = entity;
        }

        public T get() {
            return entity;
        }

        public Pointer getPointer() {
            return entity.getPointer();
        }

        @Override
        protected void finalize() throws Throwable {
            clear();
            entity = null;
        }

        protected abstract void clear();
    }

    /**
     * OpenCL command queue.
     * @author ochafik
     *
     */
    public static class CLQueue extends CLEntity<cl_command_queue> {

        final CLContext context;

        CLQueue(CLContext context, cl_command_queue entity) {
            super(entity);
            this.context = context;
        }

        @Override
        protected void clear() {
            error(CL.clReleaseCommandQueue(get()));
        }
        /// Wait for the queue to be fully executed. Costly.

        public void finish() {
            error(CL.clFinish(get()));
        }
    }
    /// OpenCL device (CPU core, GPU...)

    public static class CLDevice {

        final cl_device_id device;

        CLDevice(cl_device_id device) {
            this.device = device;
        }
        String name;

        public String getName() {
            if (name == null) {
                name = getInfoString(CL_DEVICE_NAME);
            }

            return name;
        }

        public String getExecutionCapabilities() {
            return getInfoString(CL_DEVICE_EXECUTION_CAPABILITIES);
        }

        private String getInfoString(int infoName) {
            NativeLongByReference pLen = new NativeLongByReference();
            error(CL.clGetDeviceInfo(get(), infoName, toNL(0), null, pLen));

            Memory buffer = new Memory(pLen.getValue().intValue() + 1);
            error(CL.clGetDeviceInfo(get(), infoName, pLen.getValue(), buffer, null));

            return buffer.getString(0);
        }

        @Override
        public String toString() {
            return getName();
        }

        @SuppressWarnings("deprecation")
        public CLQueue createQueue(CLContext context) {
            IntByReference errRef = new IntByReference();
            cl_command_queue queue = CL.clCreateCommandQueue(context.get(), device, 0, errRef);
            error(errRef.getValue());

            return new CLQueue(context, queue);
        }

        public cl_device_id get() {
            return device;
        }

        public static CLDevice[] listAllDevices() {
            return listDevices(true, true);
        }

        public static CLDevice[] listGPUDevices() {
            return listDevices(true, false);
        }

        public static CLDevice[] listCPUDevices() {
            return listDevices(false, true);
        }

        @SuppressWarnings("deprecation")
        protected static CLDevice[] listDevices(boolean gpu, boolean cpu) {
            int flags = (gpu ? CL_DEVICE_TYPE_GPU : 0) | (cpu ? CL_DEVICE_TYPE_CPU : 0);

            IntByReference pCount = new IntByReference();
            error(CL.clGetDeviceIDs(null, flags, 0, (PointerByReference) null, pCount));

            int nDevs = pCount.getValue();
            cl_device_id[] ids = new cl_device_id[nDevs];

            error(CL.clGetDeviceIDs(null, flags, nDevs, ids, pCount));
            CLDevice[] devices = new CLDevice[nDevs];

            for (int i = 0; i < nDevs; i++) {
                devices[i] = new CLDevice(ids[i]);
            }
            return devices;
        }
    }

    public static class CLProgram extends CLEntity<cl_program> {

        final CLContext context;

        CLProgram(CLContext context, cl_program entity) {
            super(entity);
            this.context = context;
        }

        public CLProgram build() throws CLBuildException {

            int err = CL.clBuildProgram(get(), 0, null/*context.getDeviceIds()*/, (String) null, null, null);
            if (err != CL_SUCCESS) {
                NativeLongByReference len = new NativeLongByReference();
                int bufLen = 2048;
                Memory buffer = new Memory(bufLen);

                HashSet<String> errs = new HashSet<String>();
                for (cl_device_id device_id : context.getDeviceIds()) {
                    error(CL.clGetProgramBuildInfo(get(), device_id, CL_PROGRAM_BUILD_LOG, toNL(bufLen), buffer, len));
                    String s = buffer.getString(0);
                    errs.add(s);
                }
                throw new CLBuildException(errorString(err), errs);
            }
            return this;
        }

        @Override
        protected void clear() {
            CL.clReleaseProgram(get());
        }

        public CLKernel createKernel(String name, Object... args) {
            IntBuffer errBuff = IntBuffer.wrap(new int[1]);
            cl_kernel kernel = CL.clCreateKernel(get(), name, errBuff);
            error(errBuff.get(0));

            CLKernel kn = new CLKernel(this, name, kernel);
            kn.setArgs(args);
            return kn;
        }
    }

    public static class CLKernel extends CLEntity<cl_kernel> {

        final CLProgram program;
        final String name;

        CLKernel(CLProgram program, String name, cl_kernel entity) {
            super(entity);
            this.program = program;
            this.name = name;
        }

        public void setArgs(Object... args) {
            for (int i = 0; i < args.length; i++) {
                setObjectArg(i, args[i]);
            }
        }

        public void setObjectArg(int i, Object arg) {

            if (arg instanceof NativeLong) {
                setArg(i, (NativeLong) arg);
            } else if (arg instanceof CLMem) {
                setArg(i, (CLMem) arg);
            } else if (arg instanceof Integer) {
                setArg(i, (Integer) arg);
            } else if (arg instanceof Long) {
                setArg(i, (Long) arg);
            } else if (arg instanceof Short) {
                setArg(i, (Short) arg);
            } else if (arg instanceof Byte) {
                setArg(i, (Byte) arg);
            } else if (arg instanceof Float) {
                setArg(i, (Float) arg);
            } else if (arg instanceof Double) {
                setArg(i, (Double) arg);
            } else {
                throw new IllegalArgumentException("Cannot handle kernel arguments of type " + arg.getClass().getName() + ". Use CLKernel.get() and OpenCL4Java directly.");
            }
        }

        public void setArg(int i, NativeLong arg) {
            error(CL.clSetKernelArg(get(), i, toNL(Native.LONG_SIZE), new NativeLongByReference(arg).getPointer()));
//			error(CL.clSetKernelArg(get(), i, OpenCL4Java.toNL(Native.LONG_SIZE), new IntByReference(128).getPointer()));
//			error(CL.clSetKernelArg(get(), i, toNL(Native.LONG_SIZE), new IntByReference(arg.intValue()).getPointer()));
        }

        public void setArg(int i, int arg) {
            error(CL.clSetKernelArg(get(), i, toNL(4), new IntByReference(arg).getPointer()));
        }

        public void setArg(int i, long arg) {
            error(CL.clSetKernelArg(get(), i, toNL(8), new LongByReference(arg).getPointer()));
        }

        public void setArg(int i, short arg) {
            error(CL.clSetKernelArg(get(), i, toNL(2), new ShortByReference(arg).getPointer()));
        }

        public void setArg(int i, byte arg) {
            error(CL.clSetKernelArg(get(), i, toNL(1), new ByteByReference(arg).getPointer()));
        }

        public void setArg(int i, float arg) {
            error(CL.clSetKernelArg(get(), i, toNL(4), new FloatByReference(arg).getPointer()));
        }

        public void setArg(int i, double arg) {
            error(CL.clSetKernelArg(get(), i, toNL(8), new DoubleByReference(arg).getPointer()));
        }

        public void setArg(int index, CLMem mem) {
//			new PointerByReference(input.getPointer()).getPointer())
            error(CL.clSetKernelArg(get(), index, toNL(Pointer.SIZE), new PointerByReference(mem.getPointer()).getPointer()));
        }

        @Override
        protected void clear() {
            CL.clReleaseKernel(get());
        }

        /// TODO: Get the maximum work-group size with CL.clGetKernelWorkGroupInfo(CL_KERNEL_WORK_GROUP_SIZE)
        public void enqueueNDRange(CLQueue queue, int[] globalSizes, int[] localSizes) {
            int nDims = globalSizes.length;
            if (localSizes.length != nDims) {
                throw new IllegalArgumentException("Global and local sizes must have same dimensions, given " + globalSizes.length + " vs. " + localSizes.length);
            }
            NativeLong[] globalSizesNL = new NativeLong[nDims], localSizesNL = new NativeLong[nDims];
            for (int i = 0; i < nDims; i++) {
                globalSizesNL[i] = toNL(globalSizes[i]);
                localSizesNL[i] = toNL(localSizes[i]);
            }
            error(CL.clEnqueueNDRangeKernel(queue.get(), get(), 1, null, globalSizesNL, localSizesNL, 0, null, null));
        }
    }

    @SuppressWarnings("serial")
    public static class CLBuildException extends Exception {

        public CLBuildException(String string, Collection<String> errors) {
            super(string + "\n" + StringUtils.implode(errors, "\n"));
        }
    }

    public static class CLContext extends CLEntity<cl_context> {

        final cl_device_id[] deviceIds;

        protected CLContext(cl_device_id[] deviceIds, cl_context context) {
            super(context);
            this.deviceIds = deviceIds;
        }

        public CLQueue createDefaultQueue() {
            return new CLDevice(deviceIds[0]).createQueue(this);
        }

        public cl_device_id[] getDeviceIds() {
            return deviceIds;
        }

        public CLProgram createProgram(String... srcs) {

            String[] source = new String[srcs.length];
            NativeLong[] lengths = new NativeLong[srcs.length];
            for (int i = 0; i < srcs.length; i++) {
                source[i] = srcs[i];
                lengths[i] = toNL(srcs[i].length());
            }
            IntBuffer errBuff = IntBuffer.wrap(new int[1]);
            cl_program program = CL.clCreateProgramWithSource(get(), srcs.length, source, lengths, errBuff);
            error(errBuff.get(0));
            return new CLProgram(this, program);
        }

        public static CLContext createContext(CLDevice... devices) {
            int nDevs = devices.length;
            cl_device_id[] ids = new cl_device_id[nDevs];
            for (int i = 0; i < nDevs; i++) {
                ids[i] = devices[i].get();
            }

            IntByReference errRef = new IntByReference();
            cl_context context = CL.clCreateContext(null, 1, ids, null, null, errRef);
            error(errRef.getValue());
            return new CLContext(ids, context);
        }

        //cl_queue queue;
        @Override
        protected void clear() {
            CL.clReleaseContext(get());
        }

        public CLMem createInput(Buffer buffer, boolean copy) {
            return createMem(buffer, -1, CL_MEM_READ_ONLY | (copy ? CL_MEM_COPY_HOST_PTR : CL_MEM_USE_HOST_PTR), true);
        }

        public CLMem createOutput(Buffer buffer) {
            return createMem(buffer, -1, CL_MEM_WRITE_ONLY | CL_MEM_USE_HOST_PTR, true);
        }

        public CLMem createInputOutput(Buffer buffer) {
            return createMem(buffer, -1, CL_MEM_READ_WRITE | CL_MEM_USE_HOST_PTR, true);
        }

        public CLMem createInput(int byteCount) {
            return createMem(null, byteCount, CL_MEM_READ_ONLY, false);
        }

        public CLMem createOutput(int byteCount) {
            return createMem(null, byteCount, CL_MEM_WRITE_ONLY, false);
        }

        public CLMem createInputOutput(int byteCount) {
            return createMem(null, byteCount, CL_MEM_READ_WRITE, false);
        }

        @SuppressWarnings("deprecation")
        protected CLMem createMem(final Buffer buffer, int byteCount, final int clMemFlags, final boolean retainBufferReference) {
            if (buffer != null) {
                byteCount = getByteCount(buffer);
            } else if (retainBufferReference) {
                throw new IllegalArgumentException("Cannot retain reference to null pointer !");
            }

            if (byteCount <= 0) {
                throw new IllegalArgumentException("Buffer size must be greater than zero (asked for size " + byteCount + ")");
            }

            IntByReference errRef = new IntByReference();
            //IntBuffer errBuff = IntBuffer.wrap(new int[1]);
            cl_mem mem = CL.clCreateBuffer(
                    get(),
                    clMemFlags,
                    toNL(byteCount),
                    buffer == null ? null : Native.getDirectBufferPointer(buffer),
                    errRef);
            error(errRef.getValue());

            return new CLMem(this, byteCount, mem) {
                /// keep a hard reference to the buffer

                public Buffer b = retainBufferReference ? buffer : null;

                @Override
                public String toString() {
                    return "CLMem(flags = " + clMemFlags + (b == null ? "" : ", " + getByteCount(b) + " bytes") + ")";
                }
            };
        }
    }

    public static class CLMem extends CLEntity<cl_mem> {

        final CLContext context;
        final int byteCount;

        public CLMem(CLContext context, int byteCount, cl_mem entity) {
            super(entity);
            this.byteCount = byteCount;
            this.context = context;
        }

        @Override
        protected void clear() {
            CL.clReleaseMemObject(get());
        }

        @SuppressWarnings("deprecation")
        public void read(Buffer out, CLQueue queue, boolean blocking) {
            Pointer pres = Native.getDirectBufferPointer(out);
            error(CL.clEnqueueReadBuffer(
                    queue.get(),
                    get(),
                    blocking ? CL_TRUE : 0,
                    toNL(0),
                    toNL(getByteCount(out)),
                    pres,
                    0,
                    null,
                    (PointerByReference) null//pevt
                    ));
        }
    }

    public static int getByteCount(Buffer b) {
        int c = b.capacity();
        if (b instanceof IntBuffer || b instanceof FloatBuffer) {
            return c * 4;
        }
        if (b instanceof LongBuffer || b instanceof DoubleBuffer) {
            return c * 8;
        }
        if (b instanceof ShortBuffer || b instanceof CharBuffer) {
            return c * 2;
        }
        if (b instanceof ByteBuffer) {
            return c;
        }
        throw new UnsupportedOperationException("Cannot guess byte size of buffers of type " + b.getClass().getName());
    }

    public static NativeLongByReference toNL(IntByReference local) {
        NativeLongByReference nl = new NativeLongByReference();
        nl.setPointer(local.getPointer());
        return nl;
    }

    public static NativeLong toNL(int i) {
        return new NativeLong(i);
    }

    static String errorString(int err) {
        if (err == CL_SUCCESS) {
            return null;
        }
        List<String> candidates = new ArrayList<String>();
        for (Field f : OpenCLLibrary.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            if (f.getType().equals(Integer.TYPE)) {
                try {
                    int i = (Integer) f.get(null);
                    if (i == err) {
                        candidates.add(f.getName());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return StringUtils.implode(candidates, " or ");
    }

    static void error(int err) {
        String str = errorString(err);
        if (str == null) {
            return;
        }
        throw new RuntimeException("OpenCL Error : " + str + " (code " + err + ")");
    }
}
