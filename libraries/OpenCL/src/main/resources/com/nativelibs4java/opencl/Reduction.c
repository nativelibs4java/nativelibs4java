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
   size_t dataLength,
   size_t blockLength,
   __global OUTPUT_TYPE* output)            
{
	size_t globalId = get_global_id(0);
	size_t block = globalId / blockLength;
	
	size_t inputStart = block * blockLength;
	size_t inputEnd = min(inputStart + blockLength, dataLength);
	
	OUTPUT_TYPE total = (OUTPUT_TYPE)SEED;
	for (int inputOffset = inputStart; inputOffset < inputEnd; inputOffset++)
		OPERATION(total, input[inputOffset]);
	
	output[block] = total;
}
