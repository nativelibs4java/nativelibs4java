/**
	Identity example : do nothing
	(written by Olivier Chafik, no right reserved :-)) */	

// See http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/sampler_t.html
const sampler_t sampler =
	CLK_NORMALIZED_COORDS_FALSE |
	CLK_FILTER_NEAREST |
	CLK_ADDRESS_CLAMP;

__kernel void test(
	__read_only image2d_t inputImage,
	__write_only image2d_t outputImage)
{
	// See http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/get_image_dim.html
	int2 dimensions = get_image_dim(inputImage);
	int width = dimensions.x, height = dimensions.y;
	
	int x = get_global_id(0), y = get_global_id(1);
	
	// See http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/read_imagef2d.html
	float4 pixel = read_imagef(inputImage, sampler, (int2)(x, y));
	
	// If the image is of type BGRA / UNormInt8, each component is in the [0.0f; 1.0f] interval :
	float red = pixel.x, green = pixel.y, blue = pixel.z, alpha = pixel.w;
	
	// Perform no transformation :
	// float4 transformedPixel = pixel;
	float4 transformedPixel = (float4)(red, green, blue, alpha);
	
	// See http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/write_image.html
	write_imagef(outputImage, (int2)(x, y), transformedPixel);
}
                    

