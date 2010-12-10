package com.nativelibs4java.opencl;

import com.nativelibs4java.opencl.CLImageFormat.ChannelOrder;
import com.nativelibs4java.opencl.CLImageFormat.ChannelDataType;
import com.nativelibs4java.util.NIOUtils;
import java.awt.Image;
import java.awt.image.*;
import java.nio.*;
import static com.nativelibs4java.opencl.ImageIOUtils.ImageInfo.*;

/**
 * No apparent correspondance with any OpenCL image type for the following BufferedImage types :
 * 	TYPE_3BYTE_BGR 
 * 	TYPE_4BYTE_ABGR (support only argb or bgra : need to see if endian-magic can help...)
 * 	TYPE_4BYTE_ABGR_PRE (no support for pre-multiplied channels)
 * 	TYPE_BYTE_BINARY 
 * 	TYPE_BYTE_INDEXED 
 * 	TYPE_CUSTOM 
 * 	TYPE_INT_ARGB_PRE (no support for pre-multiplied channels)
 * 	TYPE_INT_BGR 
 * 	TYPE_INT_RGB (no support for UnsignedInt8 channel data type)
 * 	TYPE_USHORT_555_RGB 
 * 	TYPE_USHORT_565_RGB
 */					
class ImageIOUtils {

	public static class ImageInfo<I extends Image> {
		public final int bufferedImageType;
		public final CLImageFormat clImageFormat;
        //public final int width, height;
		public final ImageDataGetter<I> dataGetter;
		public final ImageDataSetter<I> dataSetter;
        public final Class<? extends Buffer> bufferClass;
        public final int channelCount;
        public final int pixelByteSize;
		public ImageInfo(
			int bufferedImageType,
			CLImageFormat clImageFormat,
			ImageDataGetter dataGetter,
			ImageDataSetter dataSetter,
            Class<? extends Buffer> bufferClass,
            int channelCount,
            int pixelByteSize)
		{
			this.bufferedImageType = bufferedImageType;
			this.clImageFormat     = clImageFormat;
			this.dataGetter        = dataGetter;
			this.dataSetter        = dataSetter;
            this.bufferClass       = bufferClass;
            this.channelCount      = channelCount;
            this.pixelByteSize     = pixelByteSize;
		}

		public interface ImageDataGetter<I extends Image> {
			Buffer getData(I image, Buffer optionalExistingOutput, boolean directBuffer, boolean allowDeoptimizingDirectRead, ByteOrder byteOrder);
		}
		public interface ImageDataSetter<I extends Image> {
			void setData(I image, Buffer data, boolean allowDeoptimizingDirectWrite);
		}
	}
    
	public static ImageInfo<Image> getGenericImageInfo() {
		return new ImageInfo<Image>(
			0, 
			CLImageFormat.INT_ARGB_FORMAT,
			new ImageDataGetter<Image>() {
				public Buffer getData(Image image, Buffer optionalExistingOutput, boolean directBuffer, boolean allowDeoptimizingDirectRead, ByteOrder byteOrder) {
					IntBuffer output = null;
					int[] intData = null;
					int width = image.getWidth(null), height = image.getHeight(null);
                    if (image instanceof BufferedImage) {
						BufferedImage bufferedImage = (BufferedImage)image;
                        WritableRaster raster = checkWritableRaster(bufferedImage);
						if (optionalExistingOutput instanceof IntBuffer) {
							output = (IntBuffer)optionalExistingOutput;
							if (output != null && !output.isDirect() && output.array().length == width * height)
								intData = output.array();
							else
								output = null;
						}
						intData = raster.getPixels(0, 0, width, height, intData);
						
					} else {
						PixelGrabber grabber = new PixelGrabber(image, 0, 0, width, height, true);
						try {
							grabber.grabPixels();
							intData = (int[])grabber.getPixels();
						} catch (InterruptedException ex) {
							throw new RuntimeException("Pixel read operation was interrupted", ex);
						}
					}
					if (output == null)
						output = IntBuffer.wrap(intData);
					if (directBuffer)
						return NIOUtils.directCopy(output, byteOrder);
					else
						return output;
				}
			},
			new ImageDataSetter() {
				public void setData(Image image, Buffer data, boolean allowDeoptimizingDirectWrite) {
					if (!(image instanceof BufferedImage))
						throw new UnsupportedOperationException("Image must be a BufferedImage");
					
					BufferedImage bufferedImage = (BufferedImage)image;
					int width = bufferedImage.getWidth(), height = bufferedImage.getHeight();
					WritableRaster raster = checkWritableRaster(bufferedImage);
					
					IntBuffer input = checkBuffer(data, IntBuffer.class);
					int[] intData = input.array();
					if (intData == null) {
						intData = new int[width * height];
						input.get(intData);
					}
					raster.setPixels(0, 0, width, height, intData);
				}
			},
            IntBuffer.class,
            1,
            4
		);
	}
	
	static void checkSinglePixelPackedSampleModel(Raster raster) {
		if (raster.getNumDataElements() != 1)
			throw new IllegalArgumentException("Raster has " + raster.getNumBands() + " data elements, should have only 1 !");
		
		//SampleModel sampleModel = raster.getSampleModel();
		//if (!(sampleModel instanceof SinglePixelPackedSampleModel))
		//	throw new IllegalArgumentException("Expected SinglePixelPackedSampleModel, got " + sampleModel.getClass().getName());
		
		//return (SinglePixelPackedSampleModel)sampleModel;
	}
	static <DB extends DataBuffer> DB checkDataBuffer(Raster raster, Class<DB> dbType) {
		DataBuffer dataBuffer = raster.getDataBuffer();
		if (!dbType.isInstance(dataBuffer))
			throw new IllegalArgumentException("Expected " + dbType.getName() + ", got " + (dataBuffer == null ? null : dataBuffer.getClass().getName()));
		
		return (DB)dataBuffer;
	}
	static <B extends Buffer> B checkBuffer(Buffer buffer, Class<B> bType) {
		if (!bType.isInstance(buffer))
			throw new IllegalArgumentException("Expected " + bType.getName() + ", got " + (buffer == null ? null : buffer.getClass().getName()));
		
		return (B)buffer;
	}
	static WritableRaster checkWritableRaster(BufferedImage image) {
		Raster raster = image.getRaster();
		if (!(raster instanceof WritableRaster))
			throw new UnsupportedOperationException("Image data is not writable");
		
		return (WritableRaster)raster;
	}
	public static ImageInfo<BufferedImage> getShortGrayImageInfo() {
		return new ImageInfo<BufferedImage>(
			BufferedImage.TYPE_USHORT_GRAY,
			new CLImageFormat(CLImageFormat.ChannelOrder.LUMINANCE, CLImageFormat.ChannelDataType.UNormInt16),
			new ImageDataGetter<BufferedImage>() {
				public Buffer getData(BufferedImage image, Buffer optionalExistingOutput, boolean directBuffer, boolean allowDeoptimizingDirectRead, ByteOrder byteOrder) {
					int width = image.getWidth(), height = image.getHeight();
					WritableRaster raster = checkWritableRaster(image);
                    checkSinglePixelPackedSampleModel(raster);

					short[] existingArray = getIndirectArray(optionalExistingOutput, width * height, short[].class);
                    short[] array;
					ShortBuffer output = null;
                    if (!allowDeoptimizingDirectRead)
                        array = (short[])raster.getDataElements(0, 0, width, height, existingArray);
                    else {
                        array = checkDataBuffer(raster, DataBufferShort.class).getData();
                        if (optionalExistingOutput instanceof ShortBuffer) {
                            output = (ShortBuffer)optionalExistingOutput;
                            if (output != null && output.capacity() == width * height) {
                                if (!output.isDirect())
                                    System.arraycopy(array, 0, output.array(), 0, width * height);
                                else {
                                    output.duplicate().put(array);
                                }
                            }
                        }
                    }
					if (output == null)
						output = ShortBuffer.wrap(array);
					return directBuffer && !output.isDirect() ? NIOUtils.directCopy(output, byteOrder) : output;
				}
			},
			new ImageDataSetter<BufferedImage>() {
				public void setData(BufferedImage image, Buffer inputBuffer, boolean allowDeoptimizingDirectWrite) {
					int width = image.getWidth(), height = image.getHeight();
					WritableRaster raster = checkWritableRaster(image);
					checkSinglePixelPackedSampleModel(raster);
					ShortBuffer input = checkBuffer(inputBuffer, ShortBuffer.class);
                    short[] inputArray = input.isDirect() ? null : input.array();
                    if (allowDeoptimizingDirectWrite) {
                        if (input.isDirect()) {
                            short[] outputArray = checkDataBuffer(raster, DataBufferShort.class).getData();
                            ShortBuffer.wrap(outputArray).put(input.duplicate());
                            return;
                        }
                    }
                    if (inputArray == null) {
                        inputArray = new short[width * height];
                        input.duplicate().get(inputArray);
                    }
                    raster.setDataElements(0, 0, width, height, inputArray);
				}
			},
            ShortBuffer.class,
            1,
            2
		);
	}

    /**
     * Image stored in TYPE_USHORT_GRAY BufferedImages but in ChannelOrder.RGBA/BGRA + ChannelDataType.short
     * @return
     */
	public static ImageInfo<BufferedImage> getARGBShortGrayImageInfo(CLImageFormat format) {
		return new ImageInfo<BufferedImage>(
			BufferedImage.TYPE_USHORT_GRAY,
			format,
			new ImageDataGetter<BufferedImage>() {
				public Buffer getData(BufferedImage image, Buffer optionalExistingOutput, boolean directBuffer, boolean allowDeoptimizingDirectRead, ByteOrder byteOrder) {
					int width = image.getWidth(), height = image.getHeight();
					WritableRaster raster = checkWritableRaster(image);
                    checkSinglePixelPackedSampleModel(raster);

                    int length = width * height;
					short[] existingArray = getIndirectArray(optionalExistingOutput, length, short[].class);
                    short[] array = (short[])raster.getDataElements(0, 0, width, height, existingArray);

					ShortBuffer output = null;
                    if (optionalExistingOutput instanceof ShortBuffer) {
                        output = (ShortBuffer)optionalExistingOutput;
                        if (output.capacity() != length * 4)
                            output = null;
                    }
                    if (output == null)
                        output = NIOUtils.directShorts(length * 4, byteOrder);

                    for (int i = 0; i < length; i++) {
                        int offset = i * 4;
                        short value = array[i];
                        output.put(offset, value);
                        output.put(offset + 1, value);
                        output.put(offset + 2, value);
                        output.put(offset + 3, (short)0xffff);
                    }

                    return output;
				}
			},
			new ImageDataSetter<BufferedImage>() {
				public void setData(BufferedImage image, Buffer inputBuffer, boolean allowDeoptimizingDirectWrite) {
					int width = image.getWidth(), height = image.getHeight();
					WritableRaster raster = checkWritableRaster(image);
					checkSinglePixelPackedSampleModel(raster);
					ShortBuffer input = checkBuffer(inputBuffer, ShortBuffer.class);
                    int length = width * height;
                    short[] data = new short[length];
                    short[] four = new short[4];
                    for (int i = 0; i < length; i++) {
                        int offset = i * 4;
                        int a = input.get(offset);
                        int b = input.get(offset + 1);
                        int c = input.get(offset + 2);
                        int alpha = input.get(offset + 3); // TODO multiply by ALPHA ???
                        data[i] = (short)((a + b + c) / 3);
                    }
                    raster.setDataElements(0, 0, width, height, data);
				}
			},
            ShortBuffer.class,
            4,
            4 * 2
		);
	}
	
	public static ImageInfo<BufferedImage> getByteGrayImageInfo() {
		return new ImageInfo<BufferedImage>(
			BufferedImage.TYPE_BYTE_GRAY, 
			new CLImageFormat(CLImageFormat.ChannelOrder.LUMINANCE, CLImageFormat.ChannelDataType.SignedInt8),
			new ImageDataGetter<BufferedImage>() {
				public Buffer getData(BufferedImage image, Buffer optionalExistingOutput, boolean directBuffer, boolean allowDeoptimizingDirectRead, ByteOrder byteOrder) {
					int width = image.getWidth(), height = image.getHeight();
					WritableRaster raster = checkWritableRaster(image);
                    checkSinglePixelPackedSampleModel(raster);
					
					byte[] existingArray = getIndirectArray(optionalExistingOutput, width * height, byte[].class);
                    byte[] array;
					ByteBuffer output = null;
                    if (!allowDeoptimizingDirectRead)
                        array = (byte[])raster.getDataElements(0, 0, width, height, existingArray);
                    else {
                        array = checkDataBuffer(raster, DataBufferByte.class).getData();
                        if (optionalExistingOutput instanceof ByteBuffer) {
                            output = (ByteBuffer)optionalExistingOutput;
                            if (output != null && output.capacity() == width * height) {
                                if (!output.isDirect())
                                    System.arraycopy(array, 0, output.array(), 0, width * height);
                                else {
                                    output.duplicate().put(array);
                                }	
                            }
                        }
                    }
					if (output == null)
						output = ByteBuffer.wrap(array);
					return directBuffer && !output.isDirect() ? NIOUtils.directCopy(output, byteOrder) : output;
				}
			},
			new ImageDataSetter<BufferedImage>() {
				public void setData(BufferedImage image, Buffer inputBuffer, boolean allowDeoptimizingDirectWrite) {
					int width = image.getWidth(), height = image.getHeight();
					WritableRaster raster = checkWritableRaster(image);
					checkSinglePixelPackedSampleModel(raster);
					ByteBuffer input = checkBuffer(inputBuffer, ByteBuffer.class);
                    byte[] inputArray = input.isDirect() ? null : input.array();
                    if (allowDeoptimizingDirectWrite) {
                        if (input.isDirect()) {
                            byte[] outputArray = checkDataBuffer(raster, DataBufferByte.class).getData();
                            ByteBuffer.wrap(outputArray).put(input.duplicate());
                            return;
                        }
                    }
                    if (inputArray == null) {
                        inputArray = new byte[width * height];
                        input.duplicate().get(inputArray);
                    }
                    raster.setDataElements(0, 0, width, height, inputArray);
				}
			},
            ByteBuffer.class,
            1,
            1
		);
	}
    static <A> A getIndirectArray(Buffer buffer, int length, Class<A> arrayClass) {
        if (buffer instanceof IntBuffer && arrayClass == int[].class)
            return (A)((IntBuffer)buffer).array();
        if (buffer instanceof ShortBuffer && arrayClass == short[].class)
            return (A)((ShortBuffer)buffer).array();
        if (buffer instanceof ByteBuffer && arrayClass == byte[].class)
            return (A)((ByteBuffer)buffer).array();
        if (buffer instanceof FloatBuffer && arrayClass == float[].class)
            return (A)((FloatBuffer)buffer).array();
        if (buffer instanceof LongBuffer && arrayClass == long[].class)
            return (A)((LongBuffer)buffer).array();
        if (buffer instanceof DoubleBuffer && arrayClass == double[].class)
            return (A)((DoubleBuffer)buffer).array();
        if (buffer instanceof CharBuffer && arrayClass == char[].class)
            return (A)((CharBuffer)buffer).array();
        return null;
    }
	public static ImageInfo<BufferedImage> getIntARGBImageInfo() {
		return new ImageInfo<BufferedImage>(
			BufferedImage.TYPE_INT_ARGB, 
			CLImageFormat.INT_ARGB_FORMAT,
			new ImageDataGetter<BufferedImage>() {
				public Buffer getData(BufferedImage image, Buffer optionalExistingOutput, boolean directBuffer, boolean allowDeoptimizingDirectRead, ByteOrder byteOrder) {
					int width = image.getWidth(), height = image.getHeight();
					WritableRaster raster = checkWritableRaster(image);
                    checkSinglePixelPackedSampleModel(raster);
                    int[] existingArray = getIndirectArray(optionalExistingOutput, width * height, int[].class);
                    int[] array;
					IntBuffer output = null;
                    if (!allowDeoptimizingDirectRead)
                        array = (int[])raster.getDataElements(0, 0, width, height, existingArray);
                    else {
                        array = checkDataBuffer(raster, DataBufferInt.class).getData();
                        if (optionalExistingOutput instanceof IntBuffer) {
                            output = (IntBuffer)optionalExistingOutput;
                            if (output != null && output.capacity() == width * height) {
                                if (output.array() != null)
                                    System.arraycopy(array, 0, output.array(), 0, width * height);
                                else {
                                    output.duplicate().put(array);
                                }	
                            }
                        }
                    }
					if (output == null)
						output = IntBuffer.wrap(array);
					return directBuffer && !output.isDirect() ? NIOUtils.directCopy(output, byteOrder) : output;
				}
			},
			new ImageDataSetter<BufferedImage>() {
				public void setData(BufferedImage image, Buffer inputBuffer, boolean allowDeoptimizingDirectWrite) {
					int width = image.getWidth(), height = image.getHeight();
					WritableRaster raster = checkWritableRaster(image);
					checkSinglePixelPackedSampleModel(raster);
					IntBuffer input = checkBuffer(inputBuffer, IntBuffer.class);
                    int[] inputArray = input.isDirect() ? null : input.array();
                    if (allowDeoptimizingDirectWrite) {
                        if (input.isDirect()) {
                            int[] outputArray = checkDataBuffer(raster, DataBufferInt.class).getData();
                            IntBuffer.wrap(outputArray).put(input.duplicate());
                            return;
                        }
                    }
                    if (inputArray == null) {
                        inputArray = new int[width * height];
                        input.duplicate().get(inputArray);
                    }
                    raster.setDataElements(0, 0, width, height, inputArray);
				}
			},
            IntBuffer.class,
            1,
            4
		);
	}
	
    public static ImageInfo<?> getImageInfo(Image image) {
        if (image instanceof BufferedImage)
            return getBufferedImageInfo(((BufferedImage)image).getType());
        return getGenericImageInfo();
    }
	public static ImageInfo<BufferedImage> getBufferedImageInfo(int bufferedImageType) {
		switch (bufferedImageType) {
		case BufferedImage.TYPE_INT_ARGB:
			return getIntARGBImageInfo();
		case BufferedImage.TYPE_BYTE_GRAY:
			return getByteGrayImageInfo();
		case BufferedImage.TYPE_USHORT_GRAY:
			return getShortGrayImageInfo();
		default:
			return (ImageInfo)getGenericImageInfo();
		}
	}
	public static ImageInfo<BufferedImage> getBufferedImageInfo(CLImageFormat imageFormat) {
        if (imageFormat == null || imageFormat.getChannelOrder() == null || imageFormat.getChannelDataType() == null)
			return null;

		switch (imageFormat.getChannelOrder()) {
		case BGRA:
        case RGBA:
			switch (imageFormat.getChannelDataType()) {
            case UNormInt16:
            case UnsignedInt16:
			case SignedInt16:
                return getARGBShortGrayImageInfo(imageFormat);
            }
        }
		return getBufferedImageInfo(getBufferedImageType(imageFormat));
	}
	static int getBufferedImageType(CLImageFormat imageFormat) {
		if (imageFormat == null || imageFormat.getChannelOrder() == null || imageFormat.getChannelDataType() == null)
			return 0;
		
		switch (imageFormat.getChannelOrder()) {
		case INTENSITY:
		case LUMINANCE:
			switch (imageFormat.getChannelDataType()) {
            case UNormInt8:
            case UnsignedInt8:
			case SignedInt8:
				return BufferedImage.TYPE_BYTE_GRAY;
			case UnsignedInt16:
            case UNormInt16:
            case SignedInt16:
				return BufferedImage.TYPE_USHORT_GRAY;
			default:
                return 0;
			}
		case ARGB:
		case BGRA:
		case RGBA:
            switch (imageFormat.getChannelDataType()) {
            case UNormInt8:
            case UnsignedInt8:
			case SignedInt8:
				return BufferedImage.TYPE_INT_ARGB;
            default:
				return 0;
            }
		case RGB:
            switch (imageFormat.getChannelDataType()) {
            case UNormInt8:
            case UnsignedInt8:
			case SignedInt8:
				return BufferedImage.TYPE_INT_BGR;
            default:
				return 0;
            }
		case RGBx:
		default:
			return 0;
		}
	}
}
