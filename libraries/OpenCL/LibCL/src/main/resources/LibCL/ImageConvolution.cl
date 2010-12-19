#ifndef _LIBCL_IMAGE_CONVOLUTION_CL_
#define _LIBCL_IMAGE_CONVOLUTION_CL_

/**
 * Compute convolution result for pixel (x, y) separately on each of the 4 pixel channels.
 * @param x X coordinate of the resulting convolved pixel
 * @param y Y coordinate of the resulting convolved pixel
 * @param matrix Pointer to the convolution matrix
 * @param matrixSize Width and height of the convolution matrix (3 for a 3x3 matrix)
 * @returns Convolution result
 */
float4 convolveFloatImagePixel(
	read_only image2d_t inputImage,
	int x, int y,
	__constant const float* matrix, 
	int matrixSize)
{
	const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_FILTER_NEAREST | CLK_ADDRESS_CLAMP_TO_EDGE;
	
	float4 total = (float4)0;
	int matrixCenterOffset = matrixSize / 2;
	
	for (int dy = 0; dy < matrixSize; dy++) {
		for (int dx = 0; dx < matrixSize; dx++) {
			float4 pixel = read_imagef(inputImage, sampler, (int2)(x + dx - matrixCenterOffset, y + dy - matrixCenterOffset));
			int offset = dy * matrixSize + dx;
			float factor = matrix[offset];
			total += factor * pixel;
		}
	}
	return total; 
}

/**
 * Compute convolution result for pixel (x, y) with a different convolution matrix on all 4 channels.
 * @param x X coordinate of the resulting convolved pixel
 * @param y Y coordinate of the resulting convolved pixel
 * @param matrixX Pointer to the convolution matrix for channel x
 * @param matrixY Pointer to the convolution matrix for channel y
 * @param matrixZ Pointer to the convolution matrix for channel z
 * @param matrixW Pointer to the convolution matrix for channel w
 * @param matrixSize Width and height of the convolution matrices (3 for a 3x3 matrix)
 * @returns Convolution result
 */
float4 convolveFloatImagePixelChannels(
	read_only image2d_t inputImage,
	int x, int y,
	__constant const float* matrixX, 
	__constant const float* matrixY, 
	__constant const float* matrixZ, 
	__constant const float* matrixW, 
	int matrixSize)
{
	const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_FILTER_NEAREST | CLK_ADDRESS_CLAMP_TO_EDGE;
	
	float4 total = (float4)0;
	int matrixCenterOffset = matrixSize / 2;
	
	for (int dy = 0; dy < matrixSize; dy++) {
		for (int dx = 0; dx < matrixSize; dx++) {
			float4 pixel = read_imagef(inputImage, sampler, (int2)(x + dx - matrixCenterOffset, y + dy - matrixCenterOffset));
			int offset = dy * matrixSize + dx;
			float4 factors = (float4)(matrixX[offset], matrixY[offset], matrixZ[offset], matrixW[offset]);
			total += factors * pixel;
		}
	}
	return total; 
}

/**
 * Compute convolution result for pixel (x, y), using only the x component of all input pixels.
 * @param x X coordinate of the resulting convolved pixel
 * @param y Y coordinate of the resulting convolved pixel
 * @param matrix Pointer to the convolution matrix
 * @param matrixSize Width and height of the convolution matrix (3 for a 3x3 matrix)
 * @returns Convolution result
 */
float convolveFloatImagePixelX(
	read_only image2d_t inputImage,
	int x, int y,
	__constant const float* matrix, 
	int matrixSize)
{
	const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_FILTER_NEAREST | CLK_ADDRESS_CLAMP_TO_EDGE;
	
	float total = 0;
	int matrixCenterOffset = matrixSize / 2;
	
	for (int dy = 0; dy < matrixSize; dy++) {
		for (int dx = 0; dx < matrixSize; dx++) {
			float4 pixel = read_imagef(inputImage, sampler, (int2)(x + dx - matrixCenterOffset, y + dy - matrixCenterOffset));
			int offset = dy * matrixSize + dx;
			float factor = matrix[offset];
			total += factor * pixel.x;
		}
	}
	return total; 
}

/**
 * Compute the convolution of an image with a convolution matrix.
 * The resulting image will have a constant w channel (typically, alpha) set to 1.
 * @param matrix Pointer to the convolution matrix
 * @param matrixSize Width and height of the convolution matrix (3 for a 3x3 matrix)
 */
void convolveFloatImage(
	read_only image2d_t inputImage,
	__constant const float* matrix, 
	int matrixSize,
	write_only image2d_t outputImage)
{
	int x = get_global_id(0), y = get_global_id(1);
	
	float4 total = convolveFloatImagePixel(inputImage, x, y, matrix, matrixSize);
	
	// Transparent images are not very useful !
	total.w = 1; 
    	write_imagef(outputImage, (int2)(x, y), total); 
}

/**
 * Compute the convolution of an image with a different convolution matrix on each of the 4 pixel channels.
 * @param matrixX Pointer to the convolution matrix for channel x
 * @param matrixY Pointer to the convolution matrix for channel y
 * @param matrixZ Pointer to the convolution matrix for channel z
 * @param matrixW Pointer to the convolution matrix for channel w
 * @param matrixSize Width and height of the convolution matrix (3 for a 3x3 matrix)
 */
void convolveFloatImageChannels(
	read_only image2d_t inputImage,
	__constant const float* matrixX, 
	__constant const float* matrixY, 
	__constant const float* matrixZ, 
	__constant const float* matrixW, 
	int matrixSize,
	write_only image2d_t outputImage)
{
	int x = get_global_id(0), y = get_global_id(1);
	
	float4 total = convolveFloatImagePixelChannels(inputImage, x, y, matrixX, matrixY, matrixZ, matrixW, matrixSize);
	total.w = 1;
    	write_imagef(outputImage, (int2)(x, y), total); 
}

#endif // _LIBCL_IMAGE_CONVOLUTION_CL_
