#ifndef _LIBCL_IMAGE_CONVERT_CL_
#define _LIBCL_IMAGE_CONVERT_CL_

__constant float4 luminanceDot = ((float4)(1 / 3.f, 1 / 3.f, 1 / 3.f, 0));

void convertFloatRGBImageToGray(
	read_only image2d_t inputImage,
	write_only image2d_t outputImage)
{
	const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_FILTER_NEAREST | CLK_ADDRESS_CLAMP_TO_EDGE;
	int x = get_global_id(0), y = get_global_id(1);
	
	int2 coords = (int2)(x, y);
	float4 pixel = read_imagef(inputImage, sampler, coords);
	float luminance = dot(luminanceDot, pixel);
    	write_imagef(outputImage, coords, (float4)(luminance, luminance, luminance, 1)); 
}

#endif // _LIBCL_IMAGE_CONVERT_CL_
