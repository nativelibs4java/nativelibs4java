/**
	Sobel filter example : naive edge detection
	See http://en.wikipedia.org/wiki/Sobel_operator
	Written by Olivier Chafik, no right reserved :-) */	

// See http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/sampler_t.html
const sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_FILTER_NEAREST | CLK_ADDRESS_CLAMP_TO_EDGE;

__kernel void test(
	read_only image2d_t inputImage,
	write_only image2d_t outputImage)
{
	int x = get_global_id(0), y = get_global_id(1);
	
#define PIXEL_CONTRIB(dx, dy, coefX, coefY) \
	{\
		float4 pixel = read_imagef(inputImage, sampler, (int2)(x + dx, y + dy)); \
		total += (float2)(coefX, coefY) * (float2)(((pixel.x + (int)pixel.y + (int)pixel.z) / 3.f) * pixel.w); \
	}

	float2 total = (float2)0;
	
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

	PIXEL_CONTRIB(-1, -1, MX_NW, MY_NW);
	PIXEL_CONTRIB(0, -1, MX_N, MY_N);
	PIXEL_CONTRIB(1, -1, MX_NE, MY_NE);
	PIXEL_CONTRIB(-1, 0, MX_W, MY_W);
	PIXEL_CONTRIB(0, 0, MX_C, MY_C);
	PIXEL_CONTRIB(1, 0, MX_E, MY_E);
	PIXEL_CONTRIB(-1, 1, MX_SW, MY_SW);
	PIXEL_CONTRIB(0, 1, MX_S, MY_S);
	PIXEL_CONTRIB(1, 1, MX_SE, MY_SE);
    
// Change to 'if 0' to get an output map of directions instead of gradients
#if 1
    	float gradient = length(total);
    	float value = gradient;
#else
    	float direction = atan2(total.y, total.x);
    	float value = direction;
#endif
    	
    	write_imagef(outputImage, (int2)(x, y), (float4)(value, value, value, 1.f)); 
}
