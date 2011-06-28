/**
	Deconvolution sample that uses the Richardson-Lucy algorithm.
	Knowing the exact "point-spread function" that made an image blurry, this algorithm tries to restore
	the original image.
	If the point-spread function is not well known, the results aren't good !
	
	Written by Olivier Chafik, no right reserved :-) */	

// Import LibCL functions, which sources can be browsed here :
// http://code.google.com/p/nativelibs4java/source/browse/trunk/libraries/OpenCL/LibCL/src/main/resources#resources%2FLibCL
#include "LibCL/RichardsonLucyImageDeconvolution.cl"

// Gaussian point-spread function :
// Matrix values taken from http://en.wikipedia.org/wiki/Gaussian_blur :
__constant float pointSpreadFunction7x7[] = {
	0.00000067,	0.00002292,	0.00019117,	0.00038771,	0.00019117,	0.00002292,	0.00000067,
	0.00002292,	0.00078633,	0.00655965,	0.01330373,	0.00655965,	0.00078633,	0.00002292,
	0.00019117,	0.00655965,	0.05472157,	0.11098164,	0.05472157,	0.00655965,	0.00019117,
	0.00038771,	0.01330373,	0.11098164,	0.22508352,	0.11098164,	0.01330373,	0.00038771,
	0.00019117,	0.00655965,	0.05472157,	0.11098164,	0.05472157,	0.00655965,	0.00019117,
	0.00002292,	0.00078633,	0.00655965,	0.01330373,	0.00655965,	0.00078633,	0.00002292,
	0.00000067,	0.00002292,	0.00019117,	0.00038771,	0.00019117,	0.00002292,	0.00000067
};

// Blur the initial image
/*
__kernel void blur(read_only image2d_t inputImage, write_only image2d_t outputImage) { 
	convolveFloatImage(inputImage, pointSpreadFunction7x7, 7, outputImage); 
}
*/

RICHARDSON_LUCY_DECONVOLUTION_PRE(pointSpreadFunction7x7, 7);

// Chain a few deconvolution passes :
RICHARDSON_LUCY_DECONVOLUTION_PASS(pointSpreadFunction7x7, 7, 1);
RICHARDSON_LUCY_DECONVOLUTION_PASS(pointSpreadFunction7x7, 7, 2);
RICHARDSON_LUCY_DECONVOLUTION_PASS(pointSpreadFunction7x7, 7, 3);
//RICHARDSON_LUCY_DECONVOLUTION_PASS(pointSpreadFunction7x7, 7, 4);

RICHARDSON_LUCY_DECONVOLUTION_POST();
