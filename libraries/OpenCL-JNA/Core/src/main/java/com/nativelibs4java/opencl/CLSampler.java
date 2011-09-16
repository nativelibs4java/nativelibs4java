/*
 * JavaCL - Java API and utilities for OpenCL
 * http://javacl.googlecode.com/
 *
 * Copyright (c) 2009-2011, Olivier Chafik (http://ochafik.com/)
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
import static com.nativelibs4java.opencl.CLException.error;
import static com.nativelibs4java.opencl.JavaCL.CL;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_ADDRESS_CLAMP;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_ADDRESS_CLAMP_TO_EDGE;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_ADDRESS_NONE;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_ADDRESS_REPEAT;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_FILTER_LINEAR;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_FILTER_NEAREST;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_SAMPLER_ADDRESSING_MODE;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_SAMPLER_FILTER_MODE;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.CL_SAMPLER_NORMALIZED_COORDS;

import com.nativelibs4java.opencl.library.OpenCLLibrary.cl_sampler;
import com.nativelibs4java.util.EnumValue;
import com.nativelibs4java.util.EnumValues;
import com.nativelibs4java.util.ValuedEnum;
import com.ochafik.lang.jnaerator.runtime.NativeSize;
import com.ochafik.lang.jnaerator.runtime.NativeSizeByReference;
import com.sun.jna.Pointer;

/**
 * OpenCL sampler object.<br/>
 * A sampler object describes how to sample an image when the image is read in the kernel. <br/>
 * The built-in functions to read from an image in a kernel take a sampler as an argument. <br/>
 * The sampler arguments to the image read function can be sampler objects created using OpenCL functions and passed as argument values to the kernel or can be samplers declared inside a kernel.
 *  <br/>
 * see {@link CLContext#createSampler(boolean, com.nativelibs4java.opencl.CLSampler.AddressingMode, com.nativelibs4java.opencl.CLSampler.FilterMode) } 
 * @author Olivier Chafik
 */
public class CLSampler extends CLAbstractEntity<cl_sampler> {
	private static CLInfoGetter<cl_sampler> infos = new CLInfoGetter<cl_sampler>() {
		@Override
		protected int getInfo(cl_sampler entity, int infoTypeEnum, NativeSize size, Pointer out, NativeSizeByReference sizeOut) {
			return CL.clGetSamplerInfo(entity, infoTypeEnum, size, out, sizeOut);
		}
	};

	CLSampler(cl_sampler entity) {
		super(entity);
	}
	
	@Override
	protected void clear() {
		error(CL.clReleaseSampler(getEntity()));
	}

	/**
	 * Values for CL_SAMPLER_ADDRESSING_MODE<br/>
	 * How out-of-range image coordinates are handled when reading from an image
	 */
	public enum AddressingMode implements ValuedEnum {
		Repeat(CL_ADDRESS_REPEAT),
		ClampToEdge(CL_ADDRESS_CLAMP_TO_EDGE),
		Clamp(CL_ADDRESS_CLAMP),
		None(CL_ADDRESS_NONE);
		
		AddressingMode(long value) { this.value = value; }
		long value;
		@Override
		public long value() { return value; }
		public static AddressingMode getEnum(long v) { return EnumValues.getEnum(v, AddressingMode.class); }
	}

	/**
	 * Return the value specified by addressing_mode argument to CLContext.createSampler.
	 */
	@InfoName("CL_SAMPLER_ADDRESSING_MODE")
	public AddressingMode getAddressingMode() {
		return AddressingMode.getEnum(infos.getInt(getEntity(), CL_SAMPLER_ADDRESSING_MODE));
	}

	/**
	 * Values for CL_SAMPLER_FILTER_MODE<br/>
	 * Type of filter that must be applied when reading an image
	 */
	public enum FilterMode implements ValuedEnum {
		Nearest(CL_FILTER_NEAREST),
		Linear(CL_FILTER_LINEAR);
		
		FilterMode(long value) { this.value = value; }
		long value;
		@Override
		public long value() { return value; }
		public static FilterMode getEnum(int v) { return EnumValues.getEnum(v, FilterMode.class); }
	}
	/**
	 * Return the value specified by filter_mode argument to CLContext.createSampler.
	 */
	@InfoName("CL_SAMPLER_FILTER_MODE")
	public FilterMode getFilterMode() {
		return FilterMode.getEnum(infos.getInt(getEntity(), CL_SAMPLER_FILTER_MODE));
	}

	/**
	 * Return the value specified by normalized_coords argument to CLContext.createSampler.
	 */
	@InfoName("CL_SAMPLER_NORMALIZED_COORDS")
	public boolean getNormalizedCoords() {
		return infos.getBool(getEntity(), CL_SAMPLER_NORMALIZED_COORDS);
	}



}
