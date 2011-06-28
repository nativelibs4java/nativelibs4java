#define _mul_(tot, x) tot *= x;
#define _add_(tot, x) tot += x;
#define _min_(tot, x) tot = min(tot, x);
#define _max_(tot, x) tot = max(tot, x);

#ifndef SEED
#	error "No aggregation SEED defined !"
#endif

#ifndef OPERATION
#	error "No OPERATION defined !"
#endif

#ifndef OPERAND_TYPE
#define OPERAND_TYPE int
#endif

#ifndef OUTPUT_TYPE
#define OUTPUT_TYPE OPERAND_TYPE
#endif

__kernel void reduce(                  
   __global const OPERAND_TYPE* input,
   long blocks,
   long dataLength,
   long blockLength,
   __global OUTPUT_TYPE* output)            
{
	long block = get_global_id(0);
	if (block >= blocks)
		return;
	
	long inputStart = block * blockLength;
	long inputEnd = min(inputStart + blockLength, dataLength);
	
	OUTPUT_TYPE total = (OUTPUT_TYPE)SEED;
	for (int inputOffset = inputStart; inputOffset < inputEnd; inputOffset++)
		OPERATION(total, input[inputOffset]);
	
	output[block] = total;
}
