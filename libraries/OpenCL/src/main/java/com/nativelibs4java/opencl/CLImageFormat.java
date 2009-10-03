package com.nativelibs4java.opencl;
import com.nativelibs4java.util.EnumValue;
import com.nativelibs4java.util.EnumValues;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.sun.jna.*;
import com.sun.jna.ptr.*;
import java.nio.*;
import static com.nativelibs4java.opencl.OpenCL4Java.*;
import static com.nativelibs4java.util.NIOUtils.*;
import java.util.*;
import static com.nativelibs4java.opencl.CLException.*;

public class CLImageFormat {

	final ChannelOrder channelOrder;
	final ChannelDataType channelDataType;

	public CLImageFormat(ChannelOrder channelOrder, ChannelDataType channelDataType) {
		super();
		this.channelDataType = channelDataType;
		this.channelOrder = channelOrder;
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
		@EnumValue(CL_SNORM_INT8)		SNormInt8,
		/**
		 * Each channel component is a normalized signed 16-bit integer value
		 */
		@EnumValue(CL_SNORM_INT16)		SNormInt16,
		/**
		 * Each channel component is a normalized unsigned 8-bit integer value
		 */
		@EnumValue(CL_UNORM_INT8)		UNormInt8,
		/**
		 * Each channel component is a normalized unsigned 16- bit integer value
		 */
		@EnumValue(CL_UNORM_INT16)		UNormInt16,
		/**
		 * Represents a normalized 5-6-5 3-channel RGB image. The channel order must be CL_RGB.
		 */
		@EnumValue(CL_UNORM_SHORT_565)		UNormShort565,
		/**
		 * Represents a normalized x-5-5-5 4-channel xRGB image. The channel order must be CL_RGB.
		 */
		@EnumValue(CL_UNORM_SHORT_555)		UNormShort555,
		/**
		 * Represents a normalized x-10-10-10 4-channel xRGB image. The channel order must be CL_RGB.
		 */
		@EnumValue(CL_UNORM_INT_101010)		UNormInt101010,
		/**
		 * Each channel component is an unnormalized signed 8- bit integer value
		 */
		@EnumValue(CL_SIGNED_INT8)		SignedInt8,
		/**
		 * Each channel component is an unnormalized signed 16- bit integer value
		 */
		@EnumValue(CL_SIGNED_INT16)		SignedInt16,
		/**
		 * Each channel component is an unnormalized signed 32- bit integer value
		 */
		@EnumValue(CL_SIGNED_INT32)		SignedInt32,
		/**
		 * Each channel component is an unnormalized unsigned 8-bit integer value
		 */
		@EnumValue(CL_UNSIGNED_INT8)		UnsignedInt8,
		/**
		 * Each channel component is an unnormalized unsigned 16-bit integer value
		 */
		@EnumValue(CL_UNSIGNED_INT16)		UnsignedInt16,
		/**
		 * Each channel component is an unnormalized unsigned 32-bit integer value
		 */
		@EnumValue(CL_UNSIGNED_INT32)		UnsignedInt32,
		/**
		 * Each channel component is a 16-bit half-float value
		 */
		@EnumValue(CL_HALF_FLOAT)		HalfFloat,
		/**
		 * Each channel component is a single precision floating- point value
		 */
		@EnumValue(CL_FLOAT)		Float;


		public long getValue() { return EnumValues.getValue(this); }
		public static ChannelDataType getEnum(long v) { return EnumValues.getEnum(v, ChannelDataType.class); }
	}
}
