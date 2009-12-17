/**
 * Copied and adapted from Bob Boothby's code :
 * http://bbboblog.blogspot.com/2009/10/gpgpu-mandelbrot-with-opencl-and-java.html
 */
__kernel void mandelbrot(
	const float2 delta,
	const float2 minimum,
	const unsigned int maxIter,
	const unsigned int magicNumber,
	const unsigned int hRes,
	__global int* outputi
)
{
	int2 id = (int2)(get_global_id(0), get_global_id(1));
	
	float2 pos = minimum + delta * (float2)(id.x, id.y);
	float2 squared = pos * pos;
	float2 val = pos;
	
	int iter = 0;
	while ( (iter < maxIter) && ((squared.x + squared.y) < magicNumber) )
	{
		val.y = (2 * (val.x * val.y));
		val.x = squared.x - squared.y;
		val += pos;
		squared = val * val;
	
		iter++;
	}
	if(iter >= maxIter)
		iter = 0;
	
	outputi[id.y * hRes + id.x] = iter;
}