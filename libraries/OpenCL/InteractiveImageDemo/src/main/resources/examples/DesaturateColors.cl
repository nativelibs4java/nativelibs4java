/**
	Color Desaturation example : make an image look more gray
	Written by Olivier Chafik, no right reserved :-) */	

void desaturate(
	float greyFactor,
	read_only image2d_t inputImage,
	write_only image2d_t outputImage)
{
	// See http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/get_image_dim.html
	int2 dimensions = get_image_dim(inputImage);
	int width = dimensions.x, height = dimensions.y;
	
	int x = get_global_id(0), y = get_global_id(1);
	
	// See http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/sampler_t.html
	const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_FILTER_NEAREST | CLK_ADDRESS_CLAMP_TO_EDGE;

	// See http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/read_imagef2d.html
	float4 pixel = read_imagef(inputImage, sampler, (int2)(x, y));
	
	// If the image is of type BGRA, UNormInt8, the pixel is in the form :
	// (float4)(red, green, blue, alpha) with each component beinge [0.0f; 1.0f] interval.
	
	// Compute pixel luminance using a dot product, equivalent to the following two lines :
	// float red = pixel.x, green = pixel.y, blue = pixel.z, alpha = pixel.w;
	// float luminance = (red + green + blue) / 3;
	float luminance = dot((float4)(1/3.f, 1/3.f, 1/3.f, 0), pixel);
	
	// Lower color saturation of pixel :
	const float colorFactor = 1.f - greyFactor;
	float4 transformedPixel = colorFactor * pixel + greyFactor * ((float4)(luminance, luminance, luminance, 1.f));
	
	// See http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/write_image.html
	write_imagef(outputImage, (int2)(x, y), transformedPixel);
}

__kernel void pass(
	read_only image2d_t inputImage,
	write_only image2d_t outputImage)
{
	desaturate(0.5f, inputImage, outputImage);
}

