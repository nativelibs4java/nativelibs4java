__kernel void vec_add(	__global const float *a,
						__global const float *b,
						__global float *c)
{
	int gid = get_global_id(0);
	c[gid] = a[gid] + b[gid];
}
