/*
	Copyright (c) 2009 Olivier Chafik (http://ochafik.free.fr/)
	
	This file is part of OpenCL4Java (http://code.google.com/p/nativelibs4java/wiki/OpenCL).
	
	OpenCL4Java is free software: you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation, either version 2.1 of the License, or
	(at your option) any later version.
	
	OpenCL4Java is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.
	
	You should have received a copy of the GNU Lesser General Public License
	along with OpenCL4Java.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.nativelibs4java.opencl;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import com.ochafik.util.string.StringUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.lang.reflect.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OpenCL error
 * @author ochafik
 */
public class CLException extends RuntimeException {
    protected int code;
    CLException(String message, int code) {
        super(message);
        this.code = code;
    }
    public int getCode() {
        return code;
    }

	@Retention(RetentionPolicy.RUNTIME)
	public @interface ErrorCode {
		int value();
	}

	public static class CLTypedException extends CLException {
		String message;
		public CLTypedException() {
			super("", 0);
			ErrorCode code = getClass().getAnnotation(ErrorCode.class);
			this.code = code.value();
			this.message = getClass().getSimpleName();
		}

		@Override
		public String getMessage() {
			return message;
		}
	}

	@ErrorCode(CL_OUT_OF_RESOURCES)
	public static class OutOfResources extends CLTypedException {}
    @ErrorCode(CL_COMPILER_NOT_AVAILABLE)
	public static class CompilerNotAvailable extends CLTypedException {}
    @ErrorCode(CL_INVALID_GLOBAL_WORK_SIZE)
    public static class InvalidGlobalWorkSize extends CLTypedException {}
	@ErrorCode(CL_MAP_FAILURE)
	public static class MapFailure extends CLTypedException {}
	@ErrorCode(CL_MEM_OBJECT_ALLOCATION_FAILURE)
	public static class MemObjectAllocationFailure extends CLTypedException {}
    @ErrorCode(CL_INVALID_EVENT_WAIT_LIST)
	public static class InvalidEventWaitList extends CLTypedException {}
	@ErrorCode(CL_INVALID_ARG_INDEX)
	public static class InvalidArgIndex extends CLTypedException {}
	@ErrorCode(CL_INVALID_ARG_SIZE)
	public static class InvalidArgSize extends CLTypedException {}
	@ErrorCode(CL_INVALID_ARG_VALUE)
	public static class InvalidArgValue extends CLTypedException {}
	@ErrorCode(CL_INVALID_BINARY)
	public static class InvalidBinary extends CLTypedException {}
	@ErrorCode(CL_INVALID_EVENT)
	public static class InvalidEvent extends CLTypedException {}
	@ErrorCode(CL_INVALID_IMAGE_FORMAT_DESCRIPTOR)
	public static class InvalidImageFormatDescriptor extends CLTypedException {}
	@ErrorCode(CL_INVALID_IMAGE_SIZE)
	public static class InvalidImageSize extends CLTypedException {}
	@ErrorCode(CL_INVALID_WORK_DIMENSION)
	public static class InvalidWorkDimension extends CLTypedException {}
	@ErrorCode(CL_INVALID_WORK_GROUP_SIZE)
	public static class InvalidWorkGroupSize extends CLTypedException {}
	@ErrorCode(CL_INVALID_WORK_ITEM_SIZE)
	public static class InvalidWorkItemSize extends CLTypedException {}
	@ErrorCode(CL_INVALID_OPERATION)
	public static class InvalidOperation extends CLTypedException {}
	@ErrorCode(CL_INVALID_BUFFER_SIZE)
	public static class InvalidBufferSize extends CLTypedException {}
	@ErrorCode(CL_INVALID_GLOBAL_OFFSET)
	public static class InvalidGlobalOffset extends CLTypedException {}
	@ErrorCode(CL_OUT_OF_HOST_MEMORY)
	public static class OutOfHostMemory extends CLTypedException {}
	@ErrorCode(CL_INVALID_COMMAND_QUEUE)
	public static class InvalidCommandQueue extends CLTypedException {}
    @ErrorCode(CL_MEM_COPY_OVERLAP)
	public static class MemCopyOverlap extends CLTypedException {}
	@ErrorCode(CL_INVALID_CONTEXT)
	public static class InvalidContext extends CLTypedException {}
	@ErrorCode(CL_INVALID_KERNEL)
	public static class InvalidKernel extends CLTypedException {}
	@ErrorCode(CL_INVALID_GL_CONTEXT_APPLE)
	public static class InvalidGLContextApple extends CLTypedException {}
    @ErrorCode(CL_INVALID_GL_SHAREGROUP_REFERENCE_KHR)
	public static class InvalidGLShareGroupReference extends CLTypedException {}
	@ErrorCode(CL_INVALID_GL_OBJECT)
	public static class InvalidGLObject extends CLTypedException {}
	@ErrorCode(CL_INVALID_KERNEL_ARGS)
	public static class InvalidKernelArgs extends CLTypedException {}
	@ErrorCode(CL_INVALID_KERNEL_DEFINITION)
	public static class InvalidKernelDefinition extends CLTypedException {}
	@ErrorCode(CL_INVALID_KERNEL_NAME)
	public static class InvalidKernelName extends CLTypedException {}
	@ErrorCode(CL_INVALID_MEM_OBJECT)
	public static class InvalidMemObject extends CLTypedException {}
	@ErrorCode(CL_INVALID_MIP_LEVEL)
	public static class InvalidMipLevel extends CLTypedException {}
	@ErrorCode(CL_INVALID_PROGRAM)
	public static class InvalidProgram extends CLTypedException {}
	@ErrorCode(CL_INVALID_PROGRAM_EXECUTABLE)
	public static class InvalidProgramExecutable extends CLTypedException {}
	@ErrorCode(CL_INVALID_QUEUE_PROPERTIES)
	public static class InvalidQueueProperties extends CLTypedException {}
	@ErrorCode(CL_INVALID_VALUE)
	public static class InvalidValue extends CLTypedException {}
	@ErrorCode(CL_INVALID_SAMPLER)
	public static class InvalidSampler extends CLTypedException {}
	@ErrorCode(CL_INVALID_DEVICE_TYPE)
	public static class InvalidDeviceType extends CLTypedException {}
	@ErrorCode(CL_INVALID_BUILD_OPTIONS)
	public static class InvalidBuildOptions extends CLTypedException {}

    public static String errorString(int err) {
        if (err == CL_SUCCESS)
            return null;

        List<String> candidates = new ArrayList<String>();
        for (Field f : OpenCLLibrary.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            if (f.getType().equals(Integer.TYPE)) {
                try {
                    int i = (Integer) f.get(null);
                    if (i == err) {
                        String name = f.getName(), lname = name.toLowerCase();
                        if (lname.contains("invalid") || lname.contains("bad") || lname.contains("illegal") || lname.contains("wrong")) {
                            candidates.clear();
                            candidates.add(name);
                            break;
                        } else
                            candidates.add(name);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return StringUtils.implode(candidates, " or ");
    }

	static Map<Integer, Class<? extends CLTypedException>> typedErrorClassesByCode;
    public static void error(int err) {
        if (err == CL_SUCCESS)
            return;
        //if (err == CL_OUT_OF_RESOURCES)
        //    return;

        if (typedErrorClassesByCode == null) {
                typedErrorClassesByCode = new HashMap<Integer, Class<? extends CLTypedException>>();
                for (Class<?> c : CLException.class.getDeclaredClasses()) {
                        if (c == CLTypedException.class || !CLTypedException.class.isAssignableFrom(c))
                                continue;
                        typedErrorClassesByCode.put(c.getAnnotation(ErrorCode.class).value(), (Class<? extends CLTypedException>)c);
                }
        }
        Class<? extends CLTypedException> c = typedErrorClassesByCode.get(err);
        if (c != null) {
                try {
                        throw c.newInstance();
                } catch (InstantiationException ex) {
                        Logger.getLogger(CLException.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                        Logger.getLogger(CLException.class.getName()).log(Level.SEVERE, null, ex);
                }
        }

        throw new CLException("OpenCL Error : " + errorString(err), err);
    }
}