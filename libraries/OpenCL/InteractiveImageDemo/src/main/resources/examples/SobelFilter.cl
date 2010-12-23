/**
	Sobel filter example : naive edge detection
	This sample includes Sobel and Scharr operators, please see below.
	See http://en.wikipedia.org/wiki/Sobel_operator
	Written by Olivier Chafik, no right reserved :-) */	

// Import LibCL functions, which sources can be browsed here :
// http://code.google.com/p/nativelibs4java/source/browse/trunk/libraries/OpenCL/LibCL/src/main/resources#resources%2FLibCL
#include "LibCL/ImageConvolution.cl"

__constant float2 sobel3x3MatrixXY[] = {
	(float2)(-1, -1), (float2)(0, -2), (float2)(1, -1),
	(float2)(-2, 0), (float2)(0, 0), (float2)(2, 0),
	(float2)(-1, 1), (float2)(0, 2), (float2)(1, 1)
};
__constant float2 scharr3x3MatrixXY[] = {
	(float2)(3, 3), (float2)(0, 10), (float2)(-3, 3),
	(float2)(10, 0), (float2)(0, 0), (float2)(-10, 0),
	(float2)(3, -3), (float2)(0, -10), (float2)(-3, -3)
};

__kernel void test(
	read_only image2d_t inputImage,
	write_only image2d_t outputImage)
{
	int x = get_global_id(0), y = get_global_id(1);
	
	float scaling = 0.3f; // you can adjust this
	
	// Sobel operator
	float2 total = scaling * convolveFloatImagePixelGray2(inputImage, x, y, sobel3x3MatrixXY, 3);
	
	// Scharr operator (better rotational symmetry) :
	// float2 total = scaling * convolveFloatImagePixelGray2(inputImage, x, y, scharr3x3MatrixXY, 3);

#if 1
    	float gradient = fast_length(total);
    	float value = gradient;
#else
    	float direction = atan2(total.y, total.x);
    	float value = direction;
#endif
    	
    	write_imagef(outputImage, (int2)(x, y), (float4)(value, value, value, 1.f)); 
}
