/*
 * JavaCL - Java API and utilities for OpenCL
 * http://javacl.googlecode.com/
 *
 * Copyright (c) 2009-2015, Olivier Chafik (http://ochafik.com/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Olivier Chafik nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY OLIVIER CHAFIK AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.nativelibs4java.opencl;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import com.ochafik.util.string.StringUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;
import java.lang.reflect.*;
import static com.nativelibs4java.opencl.JavaCL.log;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OpenCL error
 * @author ochafik
 */
@SuppressWarnings("serial")
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
		protected String message;
		public CLTypedException() {
			super("", 0);
			ErrorCode code = getClass().getAnnotation(ErrorCode.class);
			this.code = code.value();
			this.message = getClass().getSimpleName();
		}

		@Override
		public String getMessage() {
			return message + logSuffix;
		}
		
		void setKernelArg(CLKernel kernel, int argIndex) {
			message += " (kernel name = " + kernel.getFunctionName() + ", num args = " + kernel.getNumArgs() + ", arg index = " + argIndex;
			CLProgram program = kernel.getProgram();
			if (program != null)
				message += ", source = <<<\n\t" + program.getSource().replaceAll("\n", "\n\t");
			
			message += "\n>>> )";
		}
	
	}

	@ErrorCode(CL_MISALIGNED_SUB_BUFFER_OFFSET)
	public static class MisalignedSubBufferOffset extends CLTypedException {}
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
	@ErrorCode(CL_BUILD_PROGRAM_FAILURE)
	public static class BuildProgramFailure extends CLTypedException {}

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

    static boolean failedForLackOfMemory(int err, int previousAttempts) {
    	switch (err) {
    	case CL_SUCCESS:
    		return false;
    	case CL_OUT_OF_HOST_MEMORY:
    	case CL_OUT_OF_RESOURCES:
    	case CL_MEM_OBJECT_ALLOCATION_FAILURE:
    		if (previousAttempts <= 1) {
	    		System.gc();
	    		if (previousAttempts == 1) {
	    			try {
	    				Thread.sleep(100);
	    			} catch (InterruptedException ex) {}
	    		}
	    		return true;
    		}
		default:
			error(err);
			assert false; // won't reach
			return false;
    	}
    }
    static final String logSuffix = System.getenv("CL_LOG_ERRORS") == null ? " (make sure to log all errors with environment variable CL_LOG_ERRORS=stdout)" : "";
        
	static Map<Integer, Class<? extends CLTypedException>> typedErrorClassesByCode;
    @SuppressWarnings("unchecked")
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
        CLException toThrow = null;
        Class<? extends CLTypedException> c = typedErrorClassesByCode.get(err);
        if (c != null) {
			try {
				toThrow = c.newInstance();
			} catch (InstantiationException ex) {
				assert log(Level.SEVERE, null, ex);
			} catch (IllegalAccessException ex) {
				assert log(Level.SEVERE, null, ex);
			}
        }
        if (toThrow == null)
        		toThrow = new CLException("OpenCL Error : " + errorString(err) + logSuffix, err);
        	
        	//assert log(Level.SEVERE, null, toThrow);
        	
        	throw toThrow;
    }
}
