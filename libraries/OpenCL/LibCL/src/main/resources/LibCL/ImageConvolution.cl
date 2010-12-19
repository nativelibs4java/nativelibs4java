#ifndef _LIBCL_IMAGE_CONVOLUTION_CL_
#define _LIBCL_IMAGE_CONVOLUTION_CL_

float4 convolveImagePixel(
	read_only image2d_t inputImage,
	int x, int y,
	const float* matrix, 
	int matrixSize)
{
	const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_FILTER_NEAREST | CLK_ADDRESS_CLAMP_TO_EDGE;
	
	float4 total = (float4)0;
	int matrixCenterOffset = matrixSize / 2;
	
	for (int dy = 0; dy < matrixSize; dy++) {
		for (int dx = 0; dx < matrixSize; dx++) {
			float4 pixel = read_imagef(inputImage, sampler, (int2)(x + dx - matrixCenterOffset, y + dy - matrixCenterOffset));
			float factor = matrix[dy * matrixSize + dx];
			total += factor * pixel;
		}
	}
	return total; 
}

void convolveImage(
	read_only image2d_t inputImage,
	const float* matrix, 
	int matrixSize,
	write_only image2d_t outputImage)
{
	const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_FILTER_NEAREST | CLK_ADDRESS_CLAMP_TO_EDGE;
	int x = get_global_id(0), y = get_global_id(1);
	
	float4 total = convolveImagePixel(inputImage, x, y, matrix, matrixSize);
	total.w = 1;
    	write_imagef(outputImage, (int2)(x, y), total); 
}

#endif // _LIBCL_IMAGE_CONVOLUTION_CL_
