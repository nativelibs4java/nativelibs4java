/**
 * Logic copied from http://en.wikipedia.org/wiki/Xorshift
 * Requires 4 initial random seeds for each work item
 */
__kernel void gen_numbers(__global uint4* seeds, size_t nNumbers, __global uint* output)
{
	size_t iWorkItem = get_global_id(0);
	size_t nWorkItems = get_global_size(0);
	size_t nNumbersByWorkItem = nNumbers / nWorkItems;
	size_t seedsOffset = iWorkItem;
	size_t outputOffset = iWorkItem * nNumbersByWorkItem;
	size_t nNumbersInThisWorkItem = nNumbersByWorkItem;
	if (iWorkItem == nWorkItems - 1)
		nNumbersInThisWorkItem += nNumbers - nNumbersByWorkItem * nWorkItems;
	
	uint4 seed = seeds[seedsOffset];
	for (int i = 0; i < nNumbersInThisWorkItem; i++) {
		uint t = seed.x ^ (seed.x << 11);
		seed.xyz = seed.yzw;
		output[outputOffset + i] = seed.w = (seed.w ^ (seed.w >> 19)) ^ (t ^ (t >> 8));
	}
	seeds[seedsOffset] = seed;
	
}

