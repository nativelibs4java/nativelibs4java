/*
 * JavaCL - Java API and utilities for OpenCL
 * http://javacl.googlecode.com/
 *
 * Copyright (c) 2009-2010, Olivier Chafik (http://ochafik.free.fr/)
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

import com.nativelibs4java.opencl.library.cl_image_format;
import com.nativelibs4java.util.EnumValue;
import com.nativelibs4java.util.EnumValues;
import com.nativelibs4java.util.ValuedEnum;

/**
 * OpenCL Image Format
 * @see CLContext#getSupportedImageFormats(com.nativelibs4java.opencl.CLMem.Flags, com.nativelibs4java.opencl.CLMem.ObjectType) 
 * @author Olivier Chafik
 */
public class CLImageFormat {

	private final ChannelOrder channelOrder;
	private final ChannelDataType channelDataType;

	CLImageFormat(cl_image_format fmt) {
		this(ChannelOrder.getEnum(fmt.image_channel_order), ChannelDataType.getEnum(fmt.image_channel_data_type));
	}
	cl_image_format to_cl_image_format() {
		return new cl_image_format((int)channelOrder.value(), (int)channelDataType.value());
	}
	public CLImageFormat(ChannelOrder channelOrder, ChannelDataType channelDataType) {
		super();
		this.channelDataType = channelDataType;
		this.channelOrder = channelOrder;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof CLImageFormat))
			return false;
		CLImageFormat f = (CLImageFormat)obj;
		if (channelOrder == null) {
			if (f.channelOrder != null)
				return false;
		} else if (!channelOrder.equals(f.channelOrder))
			return false;


		if (channelDataType == null) {
			return f.channelDataType == null;
		} else return channelDataType.equals(f.channelDataType);
	}

	@Override
	public int hashCode() {
		int h = super.hashCode();
		if (channelOrder != null)
			h ^= channelOrder.hashCode();
		if (channelDataType != null)
			h ^= channelDataType.hashCode();
		return h;
	}

	public boolean isIntBased() {
		if (channelDataType == null || channelOrder == null)
			return false;
		switch (getChannelOrder()) {
			case ARGB:
			case BGRA:
			case RGBA:
				switch (getChannelDataType()) {
					case SNormInt8:
					case SignedInt8:
					case UNormInt8:
					case UnsignedInt8:
						return true;
				}
		}
		return false;
	}
	public final ChannelOrder getChannelOrder() {
		return channelOrder;
	}
	public final ChannelDataType getChannelDataType() {
		return channelDataType;
	}

	@Override
	public String toString() {
		return "(" + channelOrder + ", " + channelDataType + ")";
	}



	public enum ChannelOrder implements ValuedEnum {
		
		/**
		 * components of channel data: (r, 0.0, 0.0, 1.0)
		 */
		R(CL_R),
		/**
		 * components of channel data: (r, 0.0, 0.0, 1.0)
		 * @since OpenCL 1.1
		 */
		Rx(CL_Rx), 
		/**
		 * components of channel data: (0.0, 0.0, 0.0, a)
		 */
		A(CL_A),
		/**
		 * components of channel data: (I, I, I, I) <br/>
		 * This format can only be used if channel data type = CL_UNORM_INT8, CL_UNORM_INT16, CL_SNORM_INT8, CL_SNORM_INT16, CL_HALF_FLOAT or CL_FLOAT.
		 */
		INTENSITY(CL_INTENSITY),
		/**
		 * components of channel data: (L, L, L, 1.0) <br/>
		 * This format can only be used if channel data type = CL_UNORM_INT8, CL_UNORM_INT16, CL_SNORM_INT8, CL_SNORM_INT16, CL_HALF_FLOAT or CL_FLOAT.
		 */
		LUMINANCE(CL_LUMINANCE),
		/**
		 * components of channel data: (r, g, 0.0, 1.0)
		 */
		RG(CL_RG),
		/**
		 * components of channel data: (r, g, 0.0, 1.0)
		 * @since OpenCL 1.1
		 */
		RGx(CL_RGx), 
		/**
		 * components of channel data: (r, 0.0, 0.0, a)
		 */
		RA(CL_RA),
		/**
		 * components of channel data: (r, g, b, 1.0) <br/>
		 * This format can only be used if channel data type = CL_UNORM_SHORT_565, CL_UNORM_SHORT_555 or CL_UNORM_INT101010.
		 */
		RGB(CL_RGB),
		/**
		 * components of channel data: (r, g, b, 1.0) <br/>
		 * This format can only be used if channel data type = CL_UNORM_SHORT_565, CL_UNORM_SHORT_555 or CL_UNORM_INT101010.
		 * @since OpenCL 1.1
		 */
		RGBx(CL_RGBx),
		/**
		 * components of channel data: (r, g, b, a)
		 */
		RGBA(CL_RGBA),
		/**
		 * components of channel data: (r, g, b, a)
		 */
		ARGB(CL_ARGB),
		/**
		 * components of channel data: (r, g, b, a) <br/>
		 * This format can only be used if channel data type = CL_UNORM_INT8, CL_SNORM_INT8, CL_SIGNED_INT8 or CL_UNSIGNED_INT8.
		 */
		BGRA(CL_BGRA);


		ChannelOrder(long value) { this.value = value; }
		long value;
		@Override
		public long value() { return value; }
		public static ChannelOrder getEnum(long v) { return EnumValues.getEnum(v, ChannelOrder.class); }

	}

	/**
	 * For example, to specify a normalized unsigned 8-bit / channel RGBA image, image_channel_order = CL_RGBA, and image_channel_data_type = CL_UNORM_INT8. The memory layout of this image format is described below:
	 */
	public enum ChannelDataType implements ValuedEnum {
		/**
		 * Each channel component is a normalized signed 8-bit integer value
		 */
		SNormInt8(CL_SNORM_INT8, 8),
		/**
		 * Each channel component is a normalized signed 16-bit integer value
		 */
		SNormInt16(CL_SNORM_INT16, 16),
		/**
		 * Each channel component is a normalized unsigned 8-bit integer value
		 */
		UNormInt8(CL_UNORM_INT8, 8),
		/**
		 * Each channel component is a normalized unsigned 16- bit integer value
		 */
		UNormInt16(CL_UNORM_INT16, 16),
		/**
		 * Represents a normalized 5-6-5 3-channel RGB image. <br/>
		 * The channel order must be CL_RGB or CL_RGBx.<br/>
		 * CL_UNORM_SHORT_565 is a special cases of packed image format where the channels of each element are packed into a single unsigned short or unsigned int. <br/>
		 * For this special packed image format, the channels are normally packed with the first channel in the most significant bits of the bitfield, and successive channels occupying progressively less significant locations.<br/>
		 * For CL_UNORM_SHORT_565, R is in bits 15:11, G is in bits 10:5 and B is in bits 4:0.
		 */
		UNormShort565(CL_UNORM_SHORT_565, 16/* ?? */),
		/**
		 * Represents a normalized x-5-5-5 4-channel xRGB image. <br/>
		 * The channel order must be CL_RGB or CL_RGBx.<br/>
		 * CL_UNORM_SHORT_555 is a special cases of packed image format where the channels of each element are packed into a single unsigned short or unsigned int. <br/>
		 * For this special packed image format, the channels are normally packed with the first channel in the most significant bits of the bitfield, and successive channels occupying progressively less significant locations.<br/>
		 * For CL_UNORM_SHORT_555, bit 15 is undefined, R is in bits 14:10, G in bits 9:5 and B in bits 4:0.
		 */
		UNormShort555(CL_UNORM_SHORT_555, 15/* ?? */),
		/**
		 * Represents a normalized x-10-10-10 4-channel xRGB image. <br/>
		 * The channel order must be CL_RGB or CL_RGBx.<br/>
		 * CL_UNORM_INT_101010 is a special cases of packed image format where the channels of each element are packed into a single unsigned short or unsigned int. <br/>
		 * For this special packed image format, the channels are normally packed with the first channel in the most significant bits of the bitfield, and successive channels occupying progressively less significant locations.<br/>
		 * For CL_UNORM_INT_101010, bits 31:30 are undefined, R is in bits 29:20, G in bits 19:10 and B in bits 9:0.
		 */
		UNormInt101010(CL_UNORM_INT_101010, 30/* TODO ?? */),
		/**
		 * Each channel component is an unnormalized signed 8- bit integer value
		 */
		SignedInt8(CL_SIGNED_INT8, 8),
		/**
		 * Each channel component is an unnormalized signed 16- bit integer value
		 */
		SignedInt16(CL_SIGNED_INT16, 16),
		/**
		 * Each channel component is an unnormalized signed 32- bit integer value
		 */
		SignedInt32(CL_SIGNED_INT32, 32),
		/**
		 * Each channel component is an unnormalized unsigned 8-bit integer value
		 */
		UnsignedInt8(CL_UNSIGNED_INT8, 8),
		/**
		 * Each channel component is an unnormalized unsigned 16-bit integer value
		 */
		UnsignedInt16(CL_UNSIGNED_INT16, 16),
		/**
		 * Each channel component is an unnormalized unsigned 32-bit integer value
		 */
		UnsignedInt32(CL_UNSIGNED_INT32, 32),
		/**
		 * Each channel component is a 16-bit half-float value
		 */
		HalfFloat(CL_HALF_FLOAT, 16),
		/**
		 * Each channel component is a single precision floating- point value
		 */
		Float(CL_FLOAT, 32);

		ChannelDataType(long value, int bits) {
			this.SIZE = bits;
			this.value = value; 
		}
		/**
		 * Size of this ChannelDataType, in bits
		 */
		public final int SIZE;
		long value;
		@Override
		public long value() { return value; }
		public static ChannelDataType getEnum(long v) { return EnumValues.getEnum(v, ChannelDataType.class); }
	}

    static CLImageFormat INT_ARGB_FORMAT = new CLImageFormat(ChannelOrder.BGRA, ChannelDataType.UNormInt8);
    //static CLImageFormat INT_ARGB_FORMAT = new CLImageFormat(ChannelOrder.RGBA, ChannelDataType.UNormInt8);
}
