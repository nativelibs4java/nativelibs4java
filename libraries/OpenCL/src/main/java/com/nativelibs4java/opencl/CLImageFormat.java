package com.nativelibs4java.opencl;
import com.nativelibs4java.util.EnumValue;
import com.nativelibs4java.util.EnumValues;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import com.nativelibs4java.opencl.library.cl_image_format;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.nio.*;
import static com.nativelibs4java.opencl.OpenCL4Java.*;
import static com.nativelibs4java.util.NIOUtils.*;
import java.util.*;
import static com.nativelibs4java.opencl.CLException.*;

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
		return new cl_image_format((int)channelOrder.getValue(), (int)channelDataType.getValue());
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



	public enum ChannelOrder {
		/**
		 * components of channel data: (r, 0.0, 0.0, 1.0)
		 */
		@EnumValue(CL_R)		R,
		/**
		 * components of channel data: (0.0, 0.0, 0.0, a)
		 */
		@EnumValue(CL_A)		A,
		/**
		 * components of channel data: (I, I, I, I) <br/>
		 * This format can only be used if channel data type = CL_UNORM_INT8, CL_UNORM_INT16, CL_SNORM_INT8, CL_SNORM_INT16, CL_HALF_FLOAT or CL_FLOAT.
		 */
		@EnumValue(CL_INTENSITY)		INTENSITY,
		/**
		 * components of channel data: (L, L, L, 1.0) <br/>
		 * This format can only be used if channel data type = CL_UNORM_INT8, CL_UNORM_INT16, CL_SNORM_INT8, CL_SNORM_INT16, CL_HALF_FLOAT or CL_FLOAT.
		 */
		@EnumValue(CL_LUMINANCE)		LUMINANCE,
		/**
		 * components of channel data: (r, g, 0.0, 1.0)
		 */
		@EnumValue(CL_RG)		RG,
		/**
		 * components of channel data: (r, 0.0, 0.0, a)
		 */
		@EnumValue(CL_RA)		RA,
		/**
		 * components of channel data: (r, g, b, 1.0) <br/>
		 * This format can only be used if channel data type = CL_UNORM_SHORT_565, CL_UNORM_SHORT_555 or CL_UNORM_INT101010.
		 */
		@EnumValue(CL_RGB)		RGB,
		/**
		 * components of channel data: (r, g, b, a)
		 */
		@EnumValue(CL_RGBA)		RGBA,
		/**
		 * components of channel data: (r, g, b, a)
		 */
		@EnumValue(CL_ARGB)		ARGB,
		/**
		 * components of channel data: (r, g, b, a) <br/>
		 * This format can only be used if channel data type = CL_UNORM_INT8, CL_SNORM_INT8, CL_SIGNED_INT8 or CL_UNSIGNED_INT8.
		 */
		@EnumValue(CL_BGRA)		BGRA;


		public long getValue() { return EnumValues.getValue(this); }
		public static ChannelOrder getEnum(long v) { return EnumValues.getEnum(v, ChannelOrder.class); }

	}

	/**
	 * For example, to specify a normalized unsigned 8-bit / channel RGBA image, image_channel_order = CL_RGBA, and image_channel_data_type = CL_UNORM_INT8. The memory layout of this image format is described below:
	 */
	public enum ChannelDataType {
		/**
		 * Each channel component is a normalized signed 8-bit integer value
		 */
		@EnumValue(CL_SNORM_INT8)		SNormInt8(8),
		/**
		 * Each channel component is a normalized signed 16-bit integer value
		 */
		@EnumValue(CL_SNORM_INT16)		SNormInt16(16),
		/**
		 * Each channel component is a normalized unsigned 8-bit integer value
		 */
		@EnumValue(CL_UNORM_INT8)		UNormInt8(8),
		/**
		 * Each channel component is a normalized unsigned 16- bit integer value
		 */
		@EnumValue(CL_UNORM_INT16)		UNormInt16(16),
		/**
		 * Represents a normalized 5-6-5 3-channel RGB image. <br/>
		 * The channel order must be CL_RGB.<br/>
		 * CL_UNORM_SHORT_565 is a special cases of packed image format where the channels of each element are packed into a single unsigned short or unsigned int. <br/>
		 * For this special packed image format, the channels are normally packed with the first channel in the most significant bits of the bitfield, and successive channels occupying progressively less significant locations.<br/>
		 * For CL_UNORM_SHORT_565, R is in bits 15:11, G is in bits 10:5 and B is in bits 4:0.
		 */
		@EnumValue(CL_UNORM_SHORT_565)		UNormShort565(16/* ?? */),
		/**
		 * Represents a normalized x-5-5-5 4-channel xRGB image. <br/>
		 * The channel order must be CL_RGB.<br/>
		 * CL_UNORM_SHORT_555 is a special cases of packed image format where the channels of each element are packed into a single unsigned short or unsigned int. <br/>
		 * For this special packed image format, the channels are normally packed with the first channel in the most significant bits of the bitfield, and successive channels occupying progressively less significant locations.<br/>
		 * For CL_UNORM_SHORT_555, bit 15 is undefined, R is in bits 14:10, G in bits 9:5 and B in bits 4:0.
		 */
		@EnumValue(CL_UNORM_SHORT_555)		UNormShort555(15/* ?? */),
		/**
		 * Represents a normalized x-10-10-10 4-channel xRGB image. <br/>
		 * The channel order must be CL_RGB.<br/>
		 * CL_UNORM_INT_101010 is a special cases of packed image format where the channels of each element are packed into a single unsigned short or unsigned int. <br/>
		 * For this special packed image format, the channels are normally packed with the first channel in the most significant bits of the bitfield, and successive channels occupying progressively less significant locations.<br/>
		 * For CL_UNORM_INT_101010, bits 31:30 are undefined, R is in bits 29:20, G in bits 19:10 and B in bits 9:0.
		 */
		@EnumValue(CL_UNORM_INT_101010)		UNormInt101010(30/* TODO ?? */),
		/**
		 * Each channel component is an unnormalized signed 8- bit integer value
		 */
		@EnumValue(CL_SIGNED_INT8)		SignedInt8(8),
		/**
		 * Each channel component is an unnormalized signed 16- bit integer value
		 */
		@EnumValue(CL_SIGNED_INT16)		SignedInt16(16),
		/**
		 * Each channel component is an unnormalized signed 32- bit integer value
		 */
		@EnumValue(CL_SIGNED_INT32)		SignedInt32(32),
		/**
		 * Each channel component is an unnormalized unsigned 8-bit integer value
		 */
		@EnumValue(CL_UNSIGNED_INT8)		UnsignedInt8(8),
		/**
		 * Each channel component is an unnormalized unsigned 16-bit integer value
		 */
		@EnumValue(CL_UNSIGNED_INT16)		UnsignedInt16(16),
		/**
		 * Each channel component is an unnormalized unsigned 32-bit integer value
		 */
		@EnumValue(CL_UNSIGNED_INT32)		UnsignedInt32(32),
		/**
		 * Each channel component is a 16-bit half-float value
		 */
		@EnumValue(CL_HALF_FLOAT)		HalfFloat(16),
		/**
		 * Each channel component is a single precision floating- point value
		 */
		@EnumValue(CL_FLOAT)		Float(32);

		/**
		 * Size of this ChannelDataType, in bits
		 */
		public final int SIZE;
		ChannelDataType(int bits) {
			this.SIZE = bits;
		}
		public long getValue() { return EnumValues.getValue(this); }
		public static ChannelDataType getEnum(long v) { return EnumValues.getEnum(v, ChannelDataType.class); }
	}
}
