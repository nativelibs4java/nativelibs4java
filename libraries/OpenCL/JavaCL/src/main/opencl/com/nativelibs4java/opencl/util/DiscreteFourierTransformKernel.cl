#pragma OPENCL EXTENSION cl_khr_fp64 : enable

__kernel void dft(
	__global const double2 *in, 
	__global double2 *out,
	int length,
	int sign)
{
	int i = get_global_id(0);
	if (i >= length)
		return;
	
	double2 tot = 0;
	double arg = (-2 * sign * i) * M_PI / (double)length;
	
	for (int k = 0; k < length; k++) {
		double2 v = in[k];
		
		double c;
		double s = sincos(k * arg, &c);
		tot += (double2)(
			dot(v, (double2)(c, -s)), 
			dot(v, (double2)(s, c))
		);
	}
	
	if (sign == 1) {
		out[i] = tot;
	} else {
		out[i] = tot / (double)length;
	}
}
