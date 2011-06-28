
inline float2 rotateValue2(float2 value, float c, float s) {
	return (float2)(
		dot(value, (float2)(c, -s)), 
		dot(value, (float2)(s, c))
	);
}

__kernel void cooleyTukeyFFTCopy(
	__global const float2* X,
	__global float2* Y,
	int N,
	__global const int* offsetsX,
	float factor)
{
	int offsetY = get_global_id(0);
	if (offsetY >= N)
		return;
		
	int offsetX = offsetsX[offsetY];
	Y[offsetY] = X[offsetX] * factor;
}

__kernel void cooleyTukeyFFTTwiddleFactors(int N, __global float2* twiddleFactors)
{
	int k = get_global_id(0);
	float param = - 3.14159265359f * 2 * k / (float)N;
	float c, s = sincos(param, &c);
	twiddleFactors[k] = (float2)(c, s);
}
__kernel void cooleyTukeyFFT(
	__global float2* Y,
	int N,
	__global float2* twiddleFactors,
	int inverse)
{
	int k = get_global_id(0);
	int halfN = N / 2;//>> 1;
	int offsetY = get_global_id(1) * N;//halfN;
	
	float2 tf = twiddleFactors[k];
	float c = tf.x, s = tf.y;
	if (inverse)
		s = -s;
	
	int o1 = offsetY + k;
	int o2 = o1 + halfN;
	float2 y1 = Y[o1];
	float2 y2 = Y[o2];
   
	float2 v = rotateValue2(y2, c, s);
	Y[o1] = y1 + v;
	Y[o2] = y1 - v;
}

