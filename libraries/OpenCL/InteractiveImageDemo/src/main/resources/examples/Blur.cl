/**
	Blur example : naive square blur.
	This example also demonstrates the chaining of multiple kernels (see commented code at the end).
	Written by Olivier Chafik, no right reserved :-) */	

// See http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/sampler_t.html
const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_FILTER_NEAREST | CLK_ADDRESS_CLAMP_TO_EDGE;

void performBlur(
	int blurSize,
	read_only image2d_t inputImage,
	write_only image2d_t outputImage)
{
	// See http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/get_image_dim.html
	int2 dimensions = get_image_dim(inputImage);
	int width = dimensions.x, height = dimensions.y;
	
	int x = get_global_id(0), y = get_global_id(1);
	
	float4 transformedPixel = (float4)0;
	
	int minDiff = -(blurSize - 1);
	for (int dx = minDiff; dx < blurSize; dx++) {
		for (int dy = minDiff; dy < blurSize; dy++) {
			// See http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/read_imagef2d.html
			float4 pixel = read_imagef(inputImage, sampler, (int2)(x + dx, y + dy));
			transformedPixel += pixel;
		}
	}
	
	int n = (2 * blurSize - 1);
	n *= n;
	
	transformedPixel /= (float)n;
	
	// See http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/write_image.html
	write_imagef(outputImage, (int2)(x, y), transformedPixel);
}

__kernel void test(
	read_only image2d_t inputImage,
	write_only image2d_t outputImage)
{
	const float matrix[] = {
		3.f, 0.f, -3.f,
		10.f, 0.f, -10.f,
		3.f, 0.f, -3.f,
	};
	convolveImage(inputImage, matrix, 3, outputImage);
}

// Perform a blur
__kernel void pass1(read_only image2d_t inputImage, write_only image2d_t outputImage) {
	performBlur(10, inputImage, outputImage);
}

/**
	You can chain as many kernel calls as you want : 
	each new kernel takes the output of the previous one as input image. 
*/
/*
__kernel void pass2(read_only image2d_t inputImage, write_only image2d_t outputImage) {
	performBlur(10, inputImage, outputImage);
}
//*/
