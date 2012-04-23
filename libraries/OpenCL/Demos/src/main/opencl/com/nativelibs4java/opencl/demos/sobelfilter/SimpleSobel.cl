
sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP | CLK_FILTER_NEAREST;

#define MX_NW 	1
#define MX_N 	2
#define MX_NE 	1
#define MX_W 	0
#define MX_C 	0
#define MX_E 	0
#define MX_SW 	-1
#define MX_S 	-2
#define MX_SE 	-1

#define MY_NW 	1
#define MY_N 	0
#define MY_NE 	-1
#define MY_W 	2
#define MY_C 	0
#define MY_E 	-2
#define MY_SW 	1
#define MY_S 	0
#define MY_SE 	-1

#define PIXEL_TO_INTENSITIES(pixel, coefX, coefY) (uint2)(coefX, coefY) * (uint2)((pixel.x + pixel.y + pixel.z) * pixel.w / 255 / 3) 

__kernel void simpleSobel(
		__read_only image2d_t input,
		uint width, uint height,
		__global float* gradientOutput,
		__global float* directionOutput
) {
    int x = get_global_id(0), y = get_global_id(1);
	if (x >= width || y >= height)
		return;
		
    int i = y * width + x;
    
    uint2 total = (uint2)0;
    
	//total += PIXEL_TO_INTENSITIES(input[i], 1, 1);
	
    int in = i - width, is = i + width;
    bool allowEast = x < width - 1, allowSouth = y < height - 1, allowNorth = y, allowWest = x;
    if (allowNorth) {
    		if (allowWest)
    			total += PIXEL_TO_INTENSITIES(read_imageui(input, sampler, (int2)(x-1, y-1)), MX_NW, MY_NW);
    		total += PIXEL_TO_INTENSITIES(read_imageui(input, sampler, (int2)(x, y-1)), MX_N, MY_N);
    		if (allowEast)
    			total += PIXEL_TO_INTENSITIES(read_imageui(input, sampler, (int2)(x+1, y-1)), MX_NE, MY_NE);
    }
    
    if (allowWest)
		total += PIXEL_TO_INTENSITIES(read_imageui(input, sampler, (int2)(x-1, y)), MX_W, MY_W);
	total += PIXEL_TO_INTENSITIES(read_imageui(input, sampler, (int2)(x, y)), MX_C, MY_C);
	if (allowEast)
		total += PIXEL_TO_INTENSITIES(read_imageui(input, sampler, (int2)(x+1, y)), MX_E, MY_E);
    	
    if (allowSouth) {
    		if (allowWest)
    			total += PIXEL_TO_INTENSITIES(read_imageui(input, sampler, (int2)(x-1, y+1)), MX_SW, MY_SW);
    		total += PIXEL_TO_INTENSITIES(read_imageui(input, sampler, (int2)(x, y+1)), MX_S, MY_S);
    		if (allowEast)
    			total += PIXEL_TO_INTENSITIES(read_imageui(input, sampler, (int2)(x+1, y+1)), MX_SE, MY_SE);
    }
    
    uint2 square = total * total;
    gradientOutput[i] = sqrt((float)(square.x + square.y));
    directionOutput[i] = atan2((float)total.y, (float)total.x);
}

__kernel void normalizeImage(__global const float* input, float maxValue, __global uchar4* output) {
	int i = get_global_id(0);
	uchar v = input[i] / maxValue * 255;
	output[i] = (uchar4)(v, v, v, 255);
}
