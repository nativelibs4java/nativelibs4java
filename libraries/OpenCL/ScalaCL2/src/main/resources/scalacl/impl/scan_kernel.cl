//
// File:       scan_kernel.cl
//
// Abstract:   This example shows how to perform an efficient parallel prefix sum (aka Scan)
//             using OpenCL.  Scan is a common data parallel primitive which can be used for 
//             variety of different operations -- this example uses local memory for storing
//             partial sums and avoids memory bank conflicts on architectures which serialize
//             memory operations that are serviced on the same memory bank by offsetting the
//             loads and stores based on the size of the local group and the number of
//             memory banks (see appropriate macro definition).  As a result, this example
//             requires that the local group size > 1.
//
// Version:    <1.0>
//
// Disclaimer: IMPORTANT:  This Apple software is supplied to you by Apple Inc. ("Apple")
//             in consideration of your agreement to the following terms, and your use,
//             installation, modification or redistribution of this Apple software
//             constitutes acceptance of these terms.  If you do not agree with these
//             terms, please do not use, install, modify or redistribute this Apple
//             software.
//
//             In consideration of your agreement to abide by the following terms, and
//             subject to these terms, Apple grants you a personal, non - exclusive
//             license, under Apple's copyrights in this original Apple software ( the
//             "Apple Software" ), to use, reproduce, modify and redistribute the Apple
//             Software, with or without modifications, in source and / or binary forms;
//             provided that if you redistribute the Apple Software in its entirety and
//             without modifications, you must retain this notice and the following text
//             and disclaimers in all such redistributions of the Apple Software. Neither
//             the name, trademarks, service marks or logos of Apple Inc. may be used to
//             endorse or promote products derived from the Apple Software without specific
//             prior written permission from Apple.  Except as expressly stated in this
//             notice, no other rights or licenses, express or implied, are granted by
//             Apple herein, including but not limited to any patent rights that may be
//             infringed by your derivative works or by other works in which the Apple
//             Software may be incorporated.
//
//             The Apple Software is provided by Apple on an "AS IS" basis.  APPLE MAKES NO
//             WARRANTIES, EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION THE IMPLIED
//             WARRANTIES OF NON - INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A
//             PARTICULAR PURPOSE, REGARDING THE APPLE SOFTWARE OR ITS USE AND OPERATION
//             ALONE OR IN COMBINATION WITH YOUR PRODUCTS.
//
//             IN NO EVENT SHALL APPLE BE LIABLE FOR ANY SPECIAL, INDIRECT, INCIDENTAL OR
//             CONSEQUENTIAL DAMAGES ( INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
//             SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
//             INTERRUPTION ) ARISING IN ANY WAY OUT OF THE USE, REPRODUCTION, MODIFICATION
//             AND / OR DISTRIBUTION OF THE APPLE SOFTWARE, HOWEVER CAUSED AND WHETHER
//             UNDER THEORY OF CONTRACT, TORT ( INCLUDING NEGLIGENCE ), STRICT LIABILITY OR
//             OTHERWISE, EVEN IF APPLE HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
// Copyright ( C ) 2008 Apple Inc. All Rights Reserved.
//
////////////////////////////////////////////////////////////////////////////////////////////////////

#ifndef DATA_TYPE
#define DATA_TYPE float
#endif

#define MEMORY_BANK_COUNT       (16)  // Adjust to your architecture
#define LOG2_MEMORY_BANK_COUNT   (4)  // Set to log2(MEMORY_BANK_COUNT)
#define ELIMINATE_CONFLICTS      (0)  // Enable for slow address calculation, but zero bank conflicts

////////////////////////////////////////////////////////////////////////////////////////////////////

#if (ELIMINATE_CONFLICTS)
#define MEMORY_BANK_OFFSET(index) ((index) >> LOG2_MEMORY_BANK_COUNT + (index) >> (2*LOG2_MEMORY_BANK_COUNT))
#else
#define MEMORY_BANK_OFFSET(index) ((index) >> LOG2_MEMORY_BANK_COUNT)
#endif

////////////////////////////////////////////////////////////////////////////////////////////////////

uint4 
GetAddressMapping(int index)
{
    const uint local_id = get_local_id(0);
    const uint group_id = get_global_id(0) / get_local_size(0);
    const uint group_size = get_local_size(0);

    uint2 global_index;
    global_index.x = index + local_id;
    global_index.y = global_index.x + group_size;

    uint2 local_index;
    local_index.x = local_id;
    local_index.y = local_id + group_size;

    return (uint4)(global_index.x, global_index.y, local_index.x, local_index.y);
}

void 
LoadLocalFromGlobal(
    __local DATA_TYPE *shared_data,
    __global const DATA_TYPE *input_data, 
    const uint4 address_pair,
    const uint n)
{
    const uint global_index_a = address_pair.x; 
    const uint global_index_b = address_pair.y; 

    const uint local_index_a = address_pair.z; 
    const uint local_index_b = address_pair.w; 

    const uint bank_offset_a = MEMORY_BANK_OFFSET(local_index_a);
    const uint bank_offset_b = MEMORY_BANK_OFFSET(local_index_b);

    shared_data[local_index_a + bank_offset_a] = input_data[global_index_a]; 
    shared_data[local_index_b + bank_offset_b] = input_data[global_index_b]; 
}

void 
LoadLocalFromGlobalNonPowerOfTwo(
    __local DATA_TYPE *shared_data,
    __global const DATA_TYPE *input_data, 
    const uint4 address_pair,
    const uint n)
{
    const uint global_index_a = address_pair.x; 
    const uint global_index_b = address_pair.y; 

    const uint local_index_a = address_pair.z; 
    const uint local_index_b = address_pair.w; 

    const uint bank_offset_a = MEMORY_BANK_OFFSET(local_index_a);
    const uint bank_offset_b = MEMORY_BANK_OFFSET(local_index_b);

    shared_data[local_index_a + bank_offset_a] = input_data[global_index_a]; 
    shared_data[local_index_b + bank_offset_b] = (local_index_b < n) ? input_data[global_index_b] : 0; 
	
	barrier(CLK_LOCAL_MEM_FENCE);
}

void 
StoreLocalToGlobal(
    __global DATA_TYPE* output_data, 
    __local const DATA_TYPE* shared_data,
    const uint4 address_pair,
    const uint n)
{
    barrier(CLK_LOCAL_MEM_FENCE);

    const uint global_index_a = address_pair.x; 
    const uint global_index_b = address_pair.y; 

    const uint local_index_a = address_pair.z; 
    const uint local_index_b = address_pair.w; 

    const uint bank_offset_a = MEMORY_BANK_OFFSET(local_index_a);
    const uint bank_offset_b = MEMORY_BANK_OFFSET(local_index_b);

    output_data[global_index_a] = shared_data[local_index_a + bank_offset_a]; 
    output_data[global_index_b] = shared_data[local_index_b + bank_offset_b]; 
}

void 
StoreLocalToGlobalNonPowerOfTwo(
    __global DATA_TYPE* output_data, 
    __local const DATA_TYPE* shared_data,
    const uint4 address_pair,
    const uint n)
{
    barrier(CLK_LOCAL_MEM_FENCE);

    const uint global_index_a = address_pair.x; 
    const uint global_index_b = address_pair.y; 

    const uint local_index_a = address_pair.z; 
    const uint local_index_b = address_pair.w; 

    const uint bank_offset_a = MEMORY_BANK_OFFSET(local_index_a);
    const uint bank_offset_b = MEMORY_BANK_OFFSET(local_index_b);

    output_data[global_index_a] = shared_data[local_index_a + bank_offset_a]; 
    if(local_index_b < n)
        output_data[global_index_b] = shared_data[local_index_b + bank_offset_b]; 
}

////////////////////////////////////////////////////////////////////////////////////////////////////

void 
ClearLastElement(
    __local DATA_TYPE* shared_data, 
    int group_index)
{
    const uint local_id = get_local_id(0);
    const uint group_id = get_global_id(0) / get_local_size(0);
    const uint group_size = get_local_size(0);

    if (local_id == 0)
    {
        int index = (group_size << 1) - 1;
        index += MEMORY_BANK_OFFSET(index);
        shared_data[index] = 0;
    }
}

void 
ClearLastElementStoreSum(
    __local DATA_TYPE* shared_data, 
    __global DATA_TYPE *partial_sums, 
    int group_index)
{
    const uint group_id = get_global_id(0) / get_local_size(0);
    const uint group_size = get_local_size(0);
    const uint local_id = get_local_id(0); 

    if (local_id == 0)
    {
        int index = (group_size << 1) - 1;
        index += MEMORY_BANK_OFFSET(index);
        partial_sums[group_index] = shared_data[index];
        shared_data[index] = 0;
    }
}

////////////////////////////////////////////////////////////////////////////////////////////////////

uint 
BuildPartialSum(
    __local DATA_TYPE *shared_data)
{
    const uint local_id = get_local_id(0);
    const uint group_size = get_local_size(0);
    const uint two = 2;
    uint stride = 1;
    
    for (uint j = group_size; j > 0; j >>= 1)
    {
        barrier(CLK_LOCAL_MEM_FENCE);

        if (local_id < j)      
        {
            int i  = mul24(mul24(two, stride), local_id);

            uint local_index_a = i + stride - 1;
            uint local_index_b = local_index_a + stride;

            local_index_a += MEMORY_BANK_OFFSET(local_index_a);
            local_index_b += MEMORY_BANK_OFFSET(local_index_b);

            shared_data[local_index_b] += shared_data[local_index_a];
        }

        stride *= two;
    }

    return stride;
}

void 
ScanRootToLeaves(
    __local DATA_TYPE *shared_data, 
    uint stride)
{
    const uint local_id = get_local_id(0);
    const uint group_id = get_global_id(0) / get_local_size(0);
    const uint group_size = get_local_size(0);
    const uint two = 2;
    
    for (uint j = 1; j <= group_size; j *= two)
    {
        stride >>= 1;

        barrier(CLK_LOCAL_MEM_FENCE);

        if (local_id < j)
        {
            int i  = mul24(mul24(two, stride), local_id);

            uint local_index_a = i + stride - 1;
            uint local_index_b = local_index_a + stride;

            local_index_a += MEMORY_BANK_OFFSET(local_index_a);
            local_index_b += MEMORY_BANK_OFFSET(local_index_b);

            DATA_TYPE t = shared_data[local_index_a];
            shared_data[local_index_a] = shared_data[local_index_b];
            shared_data[local_index_b] += t;
        }
    }
}

void 
PreScanGroup(
    __local DATA_TYPE *shared_data, 
    int group_index)
{
    const uint group_id = get_global_id(0) / get_local_size(0);

    int stride = BuildPartialSum(shared_data);               
    ClearLastElement(shared_data, (group_index == 0) ? group_id : group_index);
    ScanRootToLeaves(shared_data, stride);             
}

void 
PreScanGroupStoreSum(
    __global DATA_TYPE *partial_sums,
    __local DATA_TYPE *shared_data, 
    int group_index) 
{
    const uint group_id = get_global_id(0) / get_local_size(0);

    int stride = BuildPartialSum(shared_data);               
    ClearLastElementStoreSum(shared_data, partial_sums, (group_index == 0) ? group_id : group_index);
    ScanRootToLeaves(shared_data, stride);             
}

////////////////////////////////////////////////////////////////////////////////////////////////////

__kernel void 
PreScanKernel(
    __global DATA_TYPE *output_data, 
    __global const DATA_TYPE *input_data, 
    __local DATA_TYPE* shared_data,
    const uint  group_index, 
    const uint  base_index,
    const uint  n)
{
    const uint group_id = get_global_id(0) / get_local_size(0);
    const uint group_size = get_local_size(0);
    
    uint local_index = (base_index == 0) ? mul24(group_id, (group_size << 1)) : base_index;
    uint4 address_pair = GetAddressMapping(local_index);
    
    LoadLocalFromGlobal(shared_data, input_data, address_pair, n); 
    PreScanGroup(shared_data, group_index); 
    StoreLocalToGlobal(output_data, shared_data, address_pair, n);
}

__kernel void 
PreScanStoreSumKernel(
    __global DATA_TYPE *output_data, 
    __global const DATA_TYPE *input_data, 
    __global DATA_TYPE *partial_sums, 
    __local DATA_TYPE* shared_data,
    const uint group_index, 
    const uint base_index,
    const uint n)
{
    const uint group_id = get_global_id(0) / get_local_size(0);
    const uint group_size = get_local_size(0);

    uint local_index = (base_index == 0) ? mul24(group_id, (group_size << 1)) : base_index;
    uint4 address_pair = GetAddressMapping(local_index);
    
    LoadLocalFromGlobal(shared_data, input_data, address_pair, n); 
    PreScanGroupStoreSum(partial_sums, shared_data, group_index); 
    StoreLocalToGlobal(output_data, shared_data, address_pair, n);
}

__kernel void 
PreScanStoreSumNonPowerOfTwoKernel(
    __global DATA_TYPE *output_data, 
    __global const DATA_TYPE *input_data, 
    __global DATA_TYPE *partial_sums, 
    __local DATA_TYPE* shared_data,
    const uint group_index, 
    const uint base_index,
    const uint n) 
{
    const uint local_id = get_local_id(0);
    const uint group_id = get_global_id(0) / get_local_size(0);
    const uint group_size = get_local_size(0);

    uint local_index = (base_index == 0) ? mul24(group_id, (group_size << 1)) : base_index;
    uint4 address_pair = GetAddressMapping(local_index);
    
    LoadLocalFromGlobalNonPowerOfTwo(shared_data, input_data, address_pair, n); 
    PreScanGroupStoreSum(partial_sums, shared_data, group_index); 
    StoreLocalToGlobalNonPowerOfTwo(output_data, shared_data, address_pair, n);
}

__kernel void 
PreScanNonPowerOfTwoKernel(
    __global DATA_TYPE *output_data, 
    __global const DATA_TYPE *input_data, 
    __local DATA_TYPE* shared_data,
    const uint group_index, 
    const uint base_index,
    const uint n)
{
    const uint local_id = get_local_id(0);
    const uint group_id = get_global_id(0) / get_local_size(0);
    const uint group_size = get_local_size(0);

    uint local_index = (base_index == 0) ? mul24(group_id, (group_size << 1)) : base_index;
    uint4 address_pair = GetAddressMapping(local_index);
    
    LoadLocalFromGlobalNonPowerOfTwo(shared_data, input_data, address_pair, n); 
    PreScanGroup(shared_data, group_index); 
    StoreLocalToGlobalNonPowerOfTwo(output_data, shared_data, address_pair, n);
}

////////////////////////////////////////////////////////////////////////////////////////////////////

__kernel void UniformAddKernel(
    __global DATA_TYPE *output_data, 
    __global DATA_TYPE *input_data, 
    __local DATA_TYPE *shared_data,
    const uint group_offset, 
    const uint base_index,
    const uint n)
{
    const uint local_id = get_local_id(0);
    const uint group_id = get_global_id(0) / get_local_size(0);
    const uint group_size = get_local_size(0);

    if (local_id == 0)
        shared_data[0] = input_data[group_id + group_offset];
    
    barrier(CLK_LOCAL_MEM_FENCE);
    
    uint address = mul24(group_id, (group_size << 1)) + base_index + local_id;
    
    output_data[address]              += shared_data[0];
	if( (local_id + group_size) < n)
		output_data[address + group_size] += shared_data[0];
}

////////////////////////////////////////////////////////////////////////////////////////////////////

