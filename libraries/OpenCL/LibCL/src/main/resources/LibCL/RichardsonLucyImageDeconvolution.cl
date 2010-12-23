#ifndef _LIBCL_RICHARDSON_LUCY_IMAGE_DECONVOLUTION_CL_
#define _LIBCL_RICHARDSON_LUCY_IMAGE_DECONVOLUTION_CL_

#include "LibCL/ImageConvolution.cl"
#include "LibCL/ImageConvert.cl"

// Convert image to grayscale
void richardsonLucyDeconvolutionPre1(read_only image2d_t inputImage, write_only image2d_t outputImage)
{
	convertFloatRGBImageToGray(inputImage, outputImage);
}

// Put I factors in w (alpha) channel
void richardsonLucyDeconvolutionPre2(read_only image2d_t inputImage, __constant float* matrix, int matrixSize, write_only image2d_t outputImage)
{
	int x = get_global_id(0), y = get_global_id(1);
	float conv = convolveFloatImagePixelX(inputImage, x, y, matrix, matrixSize);
	
	const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_FILTER_NEAREST | CLK_ADDRESS_CLAMP_TO_EDGE;
	
	int2 coords = (int2)(x, y);
	float4 pixel = read_imagef(inputImage, sampler, coords);
	pixel.w = conv;
	write_imagef(outputImage, coords, pixel);
}

// Put x into xyz and set w to 1 (make the image look grey again !)
void richardsonLucyDeconvolutionPost(read_only image2d_t inputImage, write_only image2d_t outputImage)
{
	int x = get_global_id(0), y = get_global_id(1);
	const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_FILTER_NEAREST | CLK_ADDRESS_CLAMP_TO_EDGE;
	
	int2 coords = (int2)(x, y);
	float4 pixel = read_imagef(inputImage, sampler, coords);
	pixel.y = pixel.z = pixel.x;
	pixel.w = 1;
	write_imagef(outputImage, coords, pixel);
}

void richardsonLucyDeconvolutionPass(
	read_only image2d_t inputImage,
	__constant float* matrix, 
	int matrixSize,
	write_only image2d_t outputImage)
{
	int x = get_global_id(0), y = get_global_id(1);
	const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_FILTER_NEAREST | CLK_ADDRESS_CLAMP_TO_EDGE;
	const int matrixCenterOffset = matrixSize / 2;
	
	float total = 0;
	for (int dy = 0; dy < matrixSize; dy++) {
		int offset = dy * matrixSize;
		for (int dx = 0; dx < matrixSize; dx++) {
			float4 pixel = read_imagef(inputImage, sampler, (int2)(x + dx - matrixCenterOffset, y + dy - matrixCenterOffset));
			float factor = matrix[offset + dx];
			total += factor * pixel.x / pixel.w;
		}
	}
	
	int2 coords = (int2)(x, y);
	float4 pixel = read_imagef(inputImage, sampler, coords);
	pixel.x *= total;
	write_imagef(outputImage, coords, pixel);
}

#define RICHARDSON_LUCY_DECONVOLUTION_PRE(matrix, matrixSize) \
	__kernel void pre1(read_only image2d_t inputImage, write_only image2d_t outputImage) { richardsonLucyDeconvolutionPre1(inputImage, outputImage); } \
	__kernel void pre2(read_only image2d_t inputImage, write_only image2d_t outputImage) { richardsonLucyDeconvolutionPre2(inputImage, matrix, matrixSize, outputImage); }

#define RICHARDSON_LUCY_DECONVOLUTION_PASS(matrix, matrixSize, n) \
	__kernel void deconvolvePass ## n(read_only image2d_t inputImage, write_only image2d_t outputImage) { richardsonLucyDeconvolutionPass(inputImage, matrix, matrixSize, outputImage); }

#define RICHARDSON_LUCY_DECONVOLUTION_POST() \
	__kernel void post(read_only image2d_t inputImage, write_only image2d_t outputImage) { richardsonLucyDeconvolutionPost(inputImage, outputImage); }

#endif _LIBCL_RICHARDSON_LUCY_IMAGE_DECONVOLUTION_CL_

