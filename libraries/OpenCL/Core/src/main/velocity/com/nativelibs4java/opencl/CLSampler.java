#parse("main/Header.vm")
package com.nativelibs4java.opencl;
import static com.nativelibs4java.opencl.CLException.error;
import static com.nativelibs4java.opencl.JavaCL.CL;
import static com.nativelibs4java.opencl.library.IOpenCLImplementation.CL_ADDRESS_CLAMP;
import static com.nativelibs4java.opencl.library.IOpenCLImplementation.CL_ADDRESS_CLAMP_TO_EDGE;
import static com.nativelibs4java.opencl.library.IOpenCLImplementation.CL_ADDRESS_NONE;
import static com.nativelibs4java.opencl.library.IOpenCLImplementation.CL_ADDRESS_REPEAT;
import static com.nativelibs4java.opencl.library.IOpenCLImplementation.CL_FILTER_LINEAR;
import static com.nativelibs4java.opencl.library.IOpenCLImplementation.CL_FILTER_NEAREST;
import static com.nativelibs4java.opencl.library.IOpenCLImplementation.CL_SAMPLER_ADDRESSING_MODE;
import static com.nativelibs4java.opencl.library.IOpenCLImplementation.CL_SAMPLER_FILTER_MODE;
import static com.nativelibs4java.opencl.library.IOpenCLImplementation.CL_SAMPLER_NORMALIZED_COORDS;

import com.nativelibs4java.opencl.library.IOpenCLImplementation.cl_sampler;
import com.nativelibs4java.util.EnumValue;
import com.nativelibs4java.util.EnumValues;
import org.bridj.*;
import static org.bridj.Pointer.*;

/**
 * OpenCL sampler object.<br/>
 * A sampler object describes how to sample an image when the image is read in the kernel. <br/>
 * The built-in functions to read from an image in a kernel take a sampler as an argument. <br/>
 * The sampler arguments to the image read function can be sampler objects created using OpenCL functions and passed as argument values to the kernel or can be samplers declared inside a kernel.
 *  <br/>
 * see {@link CLContext#createSampler(boolean, com.nativelibs4java.opencl.CLSampler.AddressingMode, com.nativelibs4java.opencl.CLSampler.FilterMode) } 
 * @author Olivier Chafik
 */
public class CLSampler extends CLAbstractEntity {
	
	#declareInfosGetter("infos", "CL.clGetSamplerInfo")

	CLSampler(long entity) {
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
	public enum AddressingMode implements com.nativelibs4java.util.ValuedEnum {
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
	public enum FilterMode implements com.nativelibs4java.util.ValuedEnum {
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
