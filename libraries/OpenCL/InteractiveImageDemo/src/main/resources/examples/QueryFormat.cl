/**
	QueryFormat example : query image format dynamically inside the kernel
	Written by Olivier Chafik, no right reserved :-) */	

// See http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/sampler_t.html
const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_FILTER_NEAREST | CLK_ADDRESS_CLAMP_TO_EDGE;

__kernel void test(
	read_only image2d_t inputImage,
	write_only image2d_t outputImage)
{
   // See http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/get_image_dim.html
   int2 dimensions = get_image_dim(inputImage);
   int width = dimensions.x, height = dimensions.y;
   
   // See http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/get_image_channel_data_type.html
   int channelDataType = get_image_channel_data_type(inputImage);
   
   // See http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/get_image_channel_order.html
   int channelOrder = get_image_channel_order(inputImage);
   
   int x = get_global_id(0), y = get_global_id(1);
   int2 coordinates = (int2)(x, y);
   
   // See http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/read_imagef2d.html
   //float4 pixel = read_imagef(inputImage, sampler, coordinates);
   int4 pixel = read_imagei(inputImage, sampler, coordinates);
   int4 transformedPixel = pixel;
   //float4 transformedPixel = (float4)(0);//pixel;
   
   // See http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/write_image.html
   write_imagei(outputImage, coordinates, transformedPixel);
}
                    
