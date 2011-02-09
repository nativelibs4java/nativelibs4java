// Enable double-precision floating point numbers support.
// Not all platforms / devices support this, so you may have to switch to floats.
#pragma OPENCL EXTENSION cl_khr_fp64 : enable

double2 rotateValue2(double2 value, double2 sinCos) {
	return (double2)(
		dot(value, (double2)(sinCos.y, -sinCos.x)), 
		dot(value, sinCos)
	);
}

__kernel void cooleyTukeyFFTCopy(
	__global const double2* X,
	__global double2* Y,
	int N,
	__global const int* offsetsX,
	double factor)
{
	int offsetY = get_global_id(0);
	if (offsetY >= N)
		return;
		
	int offsetX = offsetsX[offsetY];
	Y[offsetY] = X[offsetX] * factor;
}

__kernel void cooleyTukeyFFTTwiddleFactors(int N, __global double2* twiddleFactors)
{
	int k = get_global_id(0);
	double param = - M_PI * 2 * k / (double)N;
	double c, s = sincos(param, &c);
	twiddleFactors[k] = (double2)(s, c);
}
__kernel void cooleyTukeyFFT(
	__global double2* Y,
	int N,
	__global double2* twiddleFactors,
	int inverse)
{
	int k = get_global_id(0);
	int halfN = N / 2;//>> 1;
	int offsetY = get_global_id(1) * N;//halfN;
	
	double2 sinCos = twiddleFactors[k];
	if (inverse)
		sinCos.x = -sinCos.x;
	
	int o1 = offsetY + k;
	int o2 = o1 + halfN;
	double2 y1 = Y[o1];
	double2 y2 = Y[o2];
   
	double2 v = rotateValue2(y2, sinCos);
	Y[o1] = y1 + v;
	Y[o2] = y1 - v;
}

