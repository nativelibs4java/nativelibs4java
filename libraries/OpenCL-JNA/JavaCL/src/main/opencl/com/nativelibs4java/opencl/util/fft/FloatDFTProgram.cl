__kernel void dft(
	__global const float2 *in, // complex values input
	__global float2 *out,      // complex values output
	int length,                 // number of input and output values
	int sign)                   // sign modifier in the exponential :
	                            // 1 for forward transform, -1 for backward.
{
	// Get the varying parameter of the parallel execution :
	int i = get_global_id(0);
	
	// In case we're executed "too much", check bounds :
	if (i >= length)
		return;
	
	// Initialize sum and inner arguments
	float2 tot = 0;
	float param = (-2 * sign * i) * 3.141593f / (float)length;
	
	for (int k = 0; k < length; k++) {
		float2 value = in[k];
		
		// Compute sin and cos in a single call : 
		float c;
		float s = sincos(k * param, &c);
		
		// This adds (value.x * c - value.y * s, value.x * s + value.y * c) to the sum :
		tot += (float2)(
			dot(value, (float2)(c, -s)), 
			dot(value, (float2)(s, c))
		);
	}
	
	if (sign == 1) {
		// forward transform (space -> frequential)
		out[i] = tot;
	} else {
		// backward transform (frequential -> space)
		out[i] = tot / (float)length;
	}
}
