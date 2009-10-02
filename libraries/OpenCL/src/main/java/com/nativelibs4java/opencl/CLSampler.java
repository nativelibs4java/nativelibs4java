/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;
import com.nativelibs4java.util.EnumValue;
import com.nativelibs4java.util.EnumValues;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.nio.*;
import static com.nativelibs4java.opencl.OpenCL4Java.*;
import java.util.*;
import static com.nativelibs4java.opencl.CLException.*;

/**
 * OpenCL sampler object.<br/>
 * A sampler object describes how to sample an image when the image is read in the kernel. <br/>
 * The built-in functions to read from an image in a kernel take a sampler as an argument. <br/>
 * The sampler arguments to the image read function can be sampler objects created using OpenCL functions and passed as argument values to the kernel or can be samplers declared inside a kernel.
 * @author Olivier Chafik
 */
public class CLSampler extends CLEntity<cl_sampler> {
	private static CLInfoGetter<cl_sampler> infos = new CLInfoGetter<cl_sampler>() {
		@Override
		protected int getInfo(cl_sampler entity, int infoTypeEnum, NativeLong size, Pointer out, NativeLongByReference sizeOut) {
			return CL.clGetSamplerInfo(entity, infoTypeEnum, size, out, sizeOut);
		}
	};

	CLSampler(cl_sampler entity) {
		super(entity);
	}
	
	@Override
	protected void clear() {
		error(CL.clReleaseSampler(get()));
	}

	/**
	 * Values for CL_SAMPLER_ADDRESSING_MODE<br/>
	 * How out-of-range image coordinates are handled when reading from an image
	 */
	public enum CLAddressingMode {
		@EnumValue(CL_ADDRESS_REPEAT        ) Repeat        ,
		@EnumValue(CL_ADDRESS_CLAMP_TO_EDGE ) ClampToEdge 	,
		@EnumValue(CL_ADDRESS_CLAMP         ) Clamp         ,
		@EnumValue(CL_ADDRESS_NONE          ) None          ;
		
		public long getValue() { return (int)EnumValues.getValue(this); }
		public static CLAddressingMode getEnum(long v) { return EnumValues.getEnum(v, CLAddressingMode.class); }
	}

	/**
	 * Return the value specified by addressing_mode argument to CLContext.createSampler.
	 */
	@CLInfoName("CL_SAMPLER_ADDRESSING_MODE")
	public CLAddressingMode getAddressingMode() {
		return CLAddressingMode.getEnum(infos.getInt(get(), CL_SAMPLER_ADDRESSING_MODE));
	}

	/**
	 * Values for CL_SAMPLER_FILTER_MODE<br/>
	 * Type of filter that must be applied when reading an image
	 */
	public enum CLFilterMode {
		@EnumValue(CL_FILTER_NEAREST ) Nearest,
		@EnumValue(CL_FILTER_LINEAR  ) Linear ;
		
		public int getValue() { return (int)EnumValues.getValue(this); }
		public static CLFilterMode getEnum(int v) { return EnumValues.getEnum(v, CLFilterMode.class); }
	}
	/**
	 * Return the value specified by filter_mode argument to CLContext.createSampler.
	 */
	@CLInfoName("CL_SAMPLER_FILTER_MODE")
	public CLFilterMode getFilterMode() {
		return CLFilterMode.getEnum(infos.getInt(get(), CL_SAMPLER_FILTER_MODE));
	}

	/**
	 * Return the value specified by normalized_coords argument to CLContext.createSampler.
	 */
	@CLInfoName("CL_SAMPLER_NORMALIZED_COORDS")
	public String getNormalizedCoords() {
		return infos.getString(get(), CL_SAMPLER_NORMALIZED_COORDS);
	}



}
