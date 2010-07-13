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

import com.bridj.Pointer;
import com.bridj.SizeT;
import static com.bridj.Pointer.*;

/**
 * OpenCL sampler object.<br/>
 * A sampler object describes how to sample an image when the image is read in the kernel. <br/>
 * The built-in functions to read from an image in a kernel take a sampler as an argument. <br/>
 * The sampler arguments to the image read function can be sampler objects created using OpenCL functions and passed as argument values to the kernel or can be samplers declared inside a kernel.
 *  <br/>
 * @see CLContext#createSampler(boolean, com.nativelibs4java.opencl.CLSampler.AddressingMode, com.nativelibs4java.opencl.CLSampler.FilterMode) 
 * @author Olivier Chafik
 */
public class CLSampler extends CLAbstractEntity<cl_sampler> {
	private static CLInfoGetter<cl_sampler> infos = new CLInfoGetter<cl_sampler>() {
		@Override
		protected int getInfo(cl_sampler entity, int infoTypeEnum, long size, Pointer out, Pointer<SizeT> sizeOut) {
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
	public enum AddressingMode {
		@EnumValue(CL_ADDRESS_REPEAT        ) Repeat        ,
		@EnumValue(CL_ADDRESS_CLAMP_TO_EDGE ) ClampToEdge 	,
		@EnumValue(CL_ADDRESS_CLAMP         ) Clamp         ,
		@EnumValue(CL_ADDRESS_NONE          ) None          ;
		
		public long getValue() { return (int)EnumValues.getValue(this); }
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
	public enum FilterMode {
		@EnumValue(CL_FILTER_NEAREST ) Nearest,
		@EnumValue(CL_FILTER_LINEAR  ) Linear ;
		
		public int getValue() { return (int)EnumValues.getValue(this); }
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
