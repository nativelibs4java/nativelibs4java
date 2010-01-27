#ifndef NUMBERS_COUNT
#define NUMBERS_COUNT 0
#endif

#ifndef WORK_ITEMS_COUNT
#define WORK_ITEMS_COUNT get_global_size(0)
#endif

/**
 * Logic copied from http://en.wikipedia.org/wiki/Xorshift
 * Requires 4 initial random seeds for each work item
 */
__kernel void gen_numbers(__global uint4* seeds, /*size_t nNumbersArg, */__global uint* output)
{
	const uint iWorkItem = get_global_id(0);
#if 1
#define seedsOffset iWorkItem
#define nNumbers NUMBERS_COUNT
#define nWorkItems WORK_ITEMS_COUNT
#define nNumbersByWorkItem (nNumbers / nWorkItems)
#define REMAINDER (nNumbers - nNumbersByWorkItem * WORK_ITEMS_COUNT)
	uint nNumbersInThisWorkItem = nNumbersByWorkItem;
	if (iWorkItem == nWorkItems - 1)
		nNumbersInThisWorkItem += REMAINDER;
#else
	const uint seedsOffset = iWorkItem;
	const uint nNumbers = nNumbersArg;
	const y nWorkItems = get_global_size(0);
	const uint nNumbersByWorkItem = nNumbers / nWorkItems;
	uint nNumbersInThisWorkItem = nNumbersByWorkItem;
	if (iWorkItem == nWorkItems - 1)
		nNumbersInThisWorkItem += nNumbers - nNumbersByWorkItem * nWorkItems;
#endif
	
	output += iWorkItem * nNumbersByWorkItem;//outputOffset;
	
	//seeds += seedsOffset;
	//uint4 seed = *seeds;
	uint4 seed = seeds[seedsOffset];
#if 1
	uint x = seed.x, y = seed.y, z = seed.z, w = seed.w;
	for (uint i = 0; i < nNumbersInThisWorkItem; i++) {
	//for (uint i = nNumbersInThisWorkItem; i--;) {
		uint t = x ^ (x << 11);
		x = y; y = z; z = w;
		//output[outputOffset + i] =
		*(output++) = 
			w = (w ^ (w >> 19)) ^ (t ^ (t >> 8));
	}
	//*seeds = (uint4)(x, y, z, w);
	seeds[seedsOffset] = (uint4)(x, y, z, w);
#else
	for (uint i = 0; i < nNumbersInThisWorkItem; i++) {
		uint t = seed.x ^ (seed.x << 11);
		seed.xyz = seed.yzw;
		*(output++) = seed.w = (seed.w ^ (seed.w >> 19)) ^ (t ^ (t >> 8));
	}
	seeds[seedsOffset] = seed;
#endif
}

#undef seedsOffset
#undef nNumbers
#undef nWorkItems
#undef nNumbersByWorkItem
#undef REMAINDER

