#parse("main/Header.vm")
package com.nativelibs4java.opencl;
import static com.nativelibs4java.opencl.CLException.error;
import static com.nativelibs4java.opencl.JavaCL.CL;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;

import java.util.EnumSet;

import com.nativelibs4java.opencl.library.OpenCLLibrary;
import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_mem;
import com.nativelibs4java.util.EnumValue;
import com.nativelibs4java.util.EnumValues;
import org.bridj.*;
import static org.bridj.Pointer.*;


/**
 * OpenCL memory object.<br/>
 * Memory objects are categorized into two types: buffer objects, and image objects. <br/>
 * A buffer object stores a one-dimensional collection of elements whereas an image object is used to store a two- or three- dimensional texture, frame-buffer or image.<br/>
 * Elements of a buffer object can be a scalar data type (such as an int, float), vector data type, or a user-defined structure. An image object is used to represent a buffer that can be used as a texture or a frame-buffer. The elements of an image object are selected from a list of predefined image formats. <br/>
 * The minimum number of elements in a memory object is one.<br/>
 * The fundamental differences between a buffer and an image object are:
 * <ul>
 * <li>Elements in a buffer are stored in sequential fashion and can be accessed using a pointer by a kernel executing on a device. Elements of an image are stored in a format that is opaque to the user and cannot be directly accessed using a pointer. Built-in functions are provided by the OpenCL C programming language to allow a kernel to read from or write to an image.</li>
 * <li>For a buffer object, the data is stored in the same format as it is accessed by the kernel, but in the case of an image object the data format used to store the image elements may not be the same as the data format used inside the kernel. Image elements are always a 4- component vector (each component can be a float or signed/unsigned integer) in a kernel. The built-in function to read from an image converts image element from the format it is stored into a 4-component vector. Similarly, the built-in function to write to an image converts the image element from a 4-component vector to the appropriate image format specified such as 4 8-bit elements, for example.</li>
 * </ul>
 *
 * Kernels take memory objects as input, and output to one or more memory objects.
 * @author Olivier Chafik
 */
public abstract class CLMem extends CLAbstractEntity {

    protected final CLContext context;
    protected long byteCount = -1;
    boolean isGL;

    #declareInfosGetter("infos", "CL.clGetImageInfo")

    CLMem(CLContext context, long byteCount, long entityPeer) {
        super(entityPeer);
        this.byteCount = byteCount;
        this.context = context;
    }
	
    public CLContext getContext() {
        return context;
    }

    public interface DestructorCallback {
    	void callback(CLMem mem);
    }
    
    /**
     * Registers a user callback function that will be called when the memory object is deleted and its resources freed. <br/>
     * Each call to clSetMemObjectDestructorCallback registers the specified user callback function on a callback stack associated with memobj. <br/>
     * The registered user callback functions are called in the reverse order in which they were registered. <br/>
     * The user callback functions are called and then the memory object's resources are freed and the memory object is deleted. <br/>
     * This provides a mechanism for the application (and libraries) using memobj to be notified when the memory referenced by host_ptr, specified when the memory object is created and used as the storage bits for the memory object, can be reused or freed.
     * @since OpenCL 1.1
     * @param callback
     */
    public void setDestructorCallback(final DestructorCallback callback) {
    	clSetMemObjectDestructorCallback_arg1_callback cb = new clSetMemObjectDestructorCallback_arg1_callback() {
    		/// @param cl_mem1 user_data
    		public void apply(OpenCLLibrary.cl_mem mem, Pointer userData) {
    			callback.callback(CLMem.this);
    		}
    	};
    	BridJ.protectFromGC(cb);
    	error(CL.clSetMemObjectDestructorCallback(getEntity(), getPeer(pointerTo(cb)), 0));
    }
    
    public CLEvent acquireGLObject(CLQueue queue, CLEvent... eventsToWaitFor) {
        return queue.enqueueAcquireGLObjects(new CLMem[] { this }, eventsToWaitFor);
    }

    public CLEvent releaseGLObject(CLQueue queue, CLEvent... eventsToWaitFor) {
        return queue.enqueueReleaseGLObjects(new CLMem[] { this }, eventsToWaitFor);
    }

    /**
     * Get the actual size of the memory object in bytes
     * @return actual size of the memory object in bytes
     */
    public long getByteCount() {
        if (byteCount < 0) {
            try {
                byteCount = infos.getIntOrLong(getEntity(), CL_MEM_SIZE);
            } catch (CLException.InvalidMemObject ex) {
                if (isGL)
                    return -1; // GL objects are not (always?) considered as valid mem objects
                else
                    throw ex;
            }
        }
        return byteCount;
    }

	/**
	 * Memory Object Usage enum
	 */
	public enum Usage {
		Input(CL_MEM_READ_ONLY, Flags.ReadOnly),
		Output(CL_MEM_WRITE_ONLY, Flags.WriteOnly),
		InputOutput(CL_MEM_READ_WRITE, Flags.ReadWrite);

		private int intFlags;
		private Flags flags;
		Usage(int intFlags, Flags flags) {
			this.intFlags = intFlags;
			this.flags = flags;
		}
		public int getIntFlags() {
			return intFlags;
		}
		public Flags getFlags() {
			return flags;
		}
	}

	public enum Flags implements com.nativelibs4java.util.ValuedEnum {
		/**
		 * This flag specifies that the memory object will be read and written by a kernel. This is the default.
		 */
		ReadWrite(CL_MEM_READ_WRITE),
		/**
		 * This flags specifies that the memory object will be written but not read by a kernel.<br/>
		 * Reading from a buffer or image object created with CL_MEM_WRITE_ONLY inside a kernel is undefined.
		 */
		WriteOnly(CL_MEM_WRITE_ONLY),
		/**
		 * This flag specifies that the memory object is a read-only memory object when used inside a kernel. <br/>
		 * Writing to a buffer or image object created with CL_MEM_READ_ONLY inside a kernel is undefined.
		 */
		ReadOnly(CL_MEM_READ_ONLY),
		/**
		 * This flag is valid only if host_ptr is not NULL. If specified, it indicates that the application wants the OpenCL implementation to use memory referenced by host_ptr as the storage bits for the memory object. <br/>
		 * OpenCL implementations are allowed to cache the buffer contents pointed to by host_ptr in device memory. This cached copy can be used when kernels are executed on a device. <br/>
		 * The result of OpenCL commands that operate on multiple buffer objects created with the same host_ptr or overlapping host regions is considered to be undefined.
		 */
		UseHostPtr(CL_MEM_USE_HOST_PTR),
		/**
		 * This flag specifies that the application wants the OpenCL implementation to allocate memory from host accessible memory. <br/>
		 * CL_MEM_ALLOC_HOST_PTR and CL_MEM_USE_HOST_PTR are mutually exclusive.<br/>
		 * CL_MEM_COPY_HOST_PTR: This flag is valid only if host_ptr is not NULL. If specified, it indicates that the application wants the OpenCL implementation to allocate memory for the memory object and copy the data from memory referenced by host_ptr.<br/>
		 * CL_MEM_COPY_HOST_PTR and CL_MEM_USE_HOST_PTR are mutually exclusive.<br/>
		 * CL_MEM_COPY_HOST_PTR can be used with CL_MEM_ALLOC_HOST_PTR to initialize the contents of the cl_mem object allocated using host-accessible (e.g. PCIe) memory.
		 */
		AllocHostPtr(CL_MEM_ALLOC_HOST_PTR),
		CopyHostPtr(CL_MEM_COPY_HOST_PTR);

		Flags(long value) { this.value = value; }
		long value;
		@Override
		public long value() { return value; }
		public static long getValue(EnumSet<Flags> set) { return EnumValues.getValue(set); }
		public static EnumSet<Flags> getEnumSet(long v) { return EnumValues.getEnumSet(v, Flags.class); }
	}
	public enum ObjectType implements com.nativelibs4java.util.ValuedEnum {
		Buffer(CL_MEM_OBJECT_BUFFER),
		Image2D(CL_MEM_OBJECT_IMAGE2D),
		Image3D(CL_MEM_OBJECT_IMAGE3D);

		ObjectType(long value) { this.value = value; }
		long value;
		@Override
		public long value() { return value; }
		public static ObjectType getEnum(long v) { return EnumValues.getEnum(v, ObjectType.class); }
	}

    public enum GLObjectType implements com.nativelibs4java.util.ValuedEnum {
		Buffer(CL_GL_OBJECT_BUFFER),
		RenderBuffer(CL_GL_OBJECT_RENDERBUFFER),
		Texture2D(CL_GL_OBJECT_TEXTURE2D),
		Texture3D(CL_GL_OBJECT_TEXTURE3D);

		GLObjectType(long value) { this.value = value; }
		long value;
		@Override
		public long value() { return value; }
		public static GLObjectType getEnum(long v) { return EnumValues.getEnum(v, GLObjectType.class); }
	}

    public static class GLObjectInfo {
        final GLObjectType type;
        final int name;
        public GLObjectInfo(GLObjectType type, int name) {
            this.type = type;
            this.name = name;
        }
        public GLObjectType getType() {
            return type;
        }
        public int getName() {
            return name;
        }
    }
    @SuppressWarnings("deprecation")
	public GLObjectInfo getGLObjectInfo() {
		#declareReusablePtrs()
        Pointer<Integer> typeRef = ptrs.int1;
        Pointer<Integer> nameRef = ptrs.int2;
        CL.clGetGLObjectInfo(getEntity(), getPeer(typeRef), getPeer(nameRef));
        return new GLObjectInfo(GLObjectType.getEnum(typeRef.getInt()), nameRef.getInt());
    }
	public enum MapFlags implements com.nativelibs4java.util.ValuedEnum {
		Read(CL_MAP_READ),
		Write(CL_MAP_WRITE),
		ReadWrite(CL_MAP_READ | CL_MAP_WRITE);

		MapFlags(long value) { this.value = value; }
		long value;
		public long value() { return value; }
		public static MapFlags getEnum(long v) { return EnumValues.getEnum(v, MapFlags.class); }
	}

    @Override
    protected void clear() {
        error(CL.clReleaseMemObject(getEntity()));
    }
}
