/**
	Naive denoising sample : 
	Transform RGB to HSL, replace L channel by local average if the sobel operator's value is not too high and the pixel L value is not too different.
	Written by Olivier Chafik, no right reserved :-) */	

// Import LibCL functions, which sources can be browsed here :
// http://code.google.com/p/nativelibs4java/source/browse/trunk/libraries/OpenCL/LibCL/src/main/resources#resources%2FLibCL
#include "LibCL/ImageConvolution.cl"
#include "LibCL/SobelOperator.cl"
#include "LibCL/rgba2hsla.cl"
#include "LibCL/hsla2rgba.cl"

// Matrix values taken from http://en.wikipedia.org/wiki/Gaussian_blur :
__constant float gaussian7x7Matrix[] = {
	0.00000067,	0.00002292,	0.00019117,	0.00038771,	0.00019117,	0.00002292,	0.00000067,
	0.00002292,	0.00078633,	0.00655965,	0.01330373,	0.00655965,	0.00078633,	0.00002292,
	0.00019117,	0.00655965,	0.05472157,	0.11098164,	0.05472157,	0.00655965,	0.00019117,
	0.00038771,	0.01330373,	0.11098164,	0.22508352,	0.11098164,	0.01330373,	0.00038771,
	0.00019117,	0.00655965,	0.05472157,	0.11098164,	0.05472157,	0.00655965,	0.00019117,
	0.00002292,	0.00078633,	0.00655965,	0.01330373,	0.00655965,	0.00078633,	0.00002292,
	0.00000067,	0.00002292,	0.00019117,	0.00038771,	0.00019117,	0.00002292,	0.00000067
};

__kernel void imageThreshold(
	read_only image2d_t inputImage,
	write_only image2d_t outputImage)
{
	int x = get_global_id(0), y = get_global_id(1);

	const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_FILTER_NEAREST | CLK_ADDRESS_CLAMP_TO_EDGE;
	float4 pixel = read_imagef(inputImage, sampler, (int2)(x, y));

	float sobel = fabs(sobelOperatorRGBFloat(inputImage, x, y).x);
	float sobelThreshold = 0.05f;
	float averageThreshold = 0.07f;
	
	if (sobel < sobelThreshold) {
		// Get smoothed pixel (averaged from neighborhood with a gaussian) :
		float4 averagePixelHSLA = rgba2hsla(convolveFloatImagePixel(inputImage, x, y, gaussian7x7Matrix, 7));
		float4 pixelHSLA = rgba2hsla(pixel);
		
		pixelHSLA.x = averagePixelHSLA.x; // avoid chromatic aberrations : unconditionally replace Hue channel
	
		float4 d = fabs(pixelHSLA - averagePixelHSLA);
		if (d.x < averageThreshold && d.y < averageThreshold && d.z < averageThreshold)
			pixelHSLA.z = averagePixelHSLA.z; // replace Luminance channel by average if it's not too far away
		
		pixel = hsla2rgba(pixelHSLA);                                       
	}
	
	write_imagef(outputImage, (int2)(x, y), pixel);
}
