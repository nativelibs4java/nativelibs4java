// Enable double-precision floating point numbers support.
// Not all platforms / devices support this, so you may have to switch to floats.
#pragma OPENCL EXTENSION cl_khr_fp64 : enable

__kernel void dft(
	__global const double2 *in, // complex values input
	__global double2 *out,      // complex values output
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
	double2 tot = 0;
	double param = (-2 * sign * i) * M_PI / (double)length;
	
	for (int k = 0; k < length; k++) {
		double2 value = in[k];
		
		// Compute sin and cos in a single call : 
		double c;
		double s = sincos(k * param, &c);
		
		// This adds (value.x * c - value.y * s, value.x * s + value.y * c) to the sum :
		tot += (double2)(
			dot(value, (double2)(c, -s)), 
			dot(value, (double2)(s, c))
		);
	}
	
	if (sign == 1) {
		// forward transform (space -> frequential)
		out[i] = tot;
	} else {
		// backward transform (frequential -> space)
		out[i] = tot / (double)length;
	}
}
