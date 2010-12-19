/**
	Generic Convolution example with a 5x5 gaussian blur matrix.
	This sample also shows how to chain two kernels, with a color to b&w conversion pass ready to be uncommented (see below).
	Written by Olivier Chafik, no right reserved :-) */	

// Import LibCL's image convolution functions
// See sources here : http://code.google.com/p/nativelibs4java/source/browse/trunk/libraries/OpenCL/LibCL/src/main/resources#resources%2FLibCL
#include "LibCL/ImageConvolution.cl"

// Matrix values taken from http://en.wikipedia.org/wiki/Gaussian_blur :
__constant const float gaussian7x7Matrix[] = {
	0.00000067,	0.00002292,	0.00019117,	0.00038771,	0.00019117,	0.00002292,	0.00000067,
	0.00002292,	0.00078633,	0.00655965,	0.01330373,	0.00655965,	0.00078633,	0.00002292,
	0.00019117,	0.00655965,	0.05472157,	0.11098164,	0.05472157,	0.00655965,	0.00019117,
	0.00038771,	0.01330373,	0.11098164,	0.22508352,	0.11098164,	0.01330373,	0.00038771,
	0.00019117,	0.00655965,	0.05472157,	0.11098164,	0.05472157,	0.00655965,	0.00019117,
	0.00002292,	0.00078633,	0.00655965,	0.01330373,	0.00655965,	0.00078633,	0.00002292,
	0.00000067,	0.00002292,	0.00019117,	0.00038771,	0.00019117,	0.00002292,	0.00000067
};

__kernel void convolve(
	read_only image2d_t inputImage,
	write_only image2d_t outputImage)
{
	convolveFloatImage(inputImage, gaussian7x7Matrix, 7 /* matrixSize */, outputImage);
}

// Uncomment this kernel to add a pass that transforms the image from color to gray levels :
/*
__kernel void toGray(
	read_only image2d_t inputImage,
	write_only image2d_t outputImage)
{
	int x = get_global_id(0), y = get_global_id(1);
	
	// See http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/sampler_t.html
	const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_FILTER_NEAREST | CLK_ADDRESS_CLAMP_TO_EDGE;

	float4 pixel = read_imagef(inputImage, sampler, (int2)(x, y));
	float luminance = dot(pixel, (float4)(1, 1, 1, 0)) / 3;
    	write_imagef(outputImage, (int2)(x, y), (float4)(luminance, luminance, luminance, 1));
}
//*/

