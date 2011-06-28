/**
	Image luminance threshold example : hide pixels that are not bright enough (with gaussian smoothing)
	Written by Olivier Chafik, no right reserved :-) */	

// Import LibCL functions, which sources can be browsed here :
// http://code.google.com/p/nativelibs4java/source/browse/trunk/libraries/OpenCL/LibCL/src/main/resources#resources%2FLibCL
#include "LibCL/ImageConvolution.cl"
#include "LibCL/SobelOperator.cl"
#include "LibCL/Gaussian7x7.cl"

__kernel void imageThreshold(
	read_only image2d_t inputImage,
	write_only image2d_t outputImage)
{
	int x = get_global_id(0), y = get_global_id(1);

	// See http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/sampler_t.html
	const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_FILTER_NEAREST | CLK_ADDRESS_CLAMP_TO_EDGE;
	float4 pixel = read_imagef(inputImage, sampler, (int2)(x, y));
	
	// Get smoothed luminance (averaged from neighborhood with a gaussian) :
	float luminance = convolveFloatImagePixelGray(inputImage, x, y, gaussian7x7Matrix, 7);
	// Get exact local luminance :
	// float luminance	= dot((float4)(1/3.f, 1/3.f, 1/3.f, 0), pixel);
	
	float2 sobel = sobelOperatorRGBFloat(inputImage, x, y);
	
	float threshold = 0.3f;
	if (luminance < threshold && (luminance * sobel.x) < threshold)
		pixel.w = 0; // make these pixels transparent
		                                                                    
	write_imagef(outputImage, (int2)(x, y), pixel);
}
