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
/*
__kernel void cooleyTukeyFFTTwiddleFactors4(int N, __global double2* twiddleFactors)
{
	int k = get_global_id(0);
	double param2 = - M_PI * 2 * k / (double)N;
	double param3 = param2 * 2;
	double param4 = param2 * 3;
	double c2, s2 = sincos(param2, &c2);
	double c3, s3 = sincos(param3, &c3);
	double c4, s4 = sincos(param4, &c4);
	int offset = k * 3;
	twiddleFactors[offset++] = (double2)(s2, c2);
	twiddleFactors[offset++] = (double2)(s3, c3);
	twiddleFactors[offset++] = (double2)(s4, c4);
}
__kernel void cooleyTukeyFFT4(
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
*/
