/**
	Generic Convolution example with a 5x5 gaussian blur matrix.
	This sample also shows how to chain two kernels, with a color to b&w conversion pass ready to be uncommented (see below).
	Written by Olivier Chafik, no right reserved :-) */	

// Import LibCL's image convolution functions
// See sources here : http://code.google.com/p/nativelibs4java/source/browse/trunk/libraries/OpenCL/LibCL/src/main/resources#resources%2FLibCL
#include "LibCL/ImageConvolution.cl"

__constant const float gaussian5x5Matrix[] = {
	1 / 273.f,	 4 / 273.f,	 7 / 273.f,	 4 / 273.f,	 1 / 273.f,
	4 / 273.f,	 16 / 273.f,	 26 / 273.f,	 16 / 273.f,	 4 / 273.f,
	7 / 273.f,	 26 / 273.f,	 41 / 273.f,	 26 / 273.f,	 7 / 273.f,
	4 / 273.f,	 16 / 273.f,	 26 / 273.f,	 16 / 273.f,	 4 / 273.f,
	1 / 273.f,	 4 / 273.f,	 7 / 273.f,	 4 / 273.f,	 1 / 273.f
};

__kernel void convolve(
	read_only image2d_t inputImage,
	write_only image2d_t outputImage)
{
	convolveFloatImage(inputImage, gaussian5x5Matrix, 5 /* matrixSize */, outputImage);
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

