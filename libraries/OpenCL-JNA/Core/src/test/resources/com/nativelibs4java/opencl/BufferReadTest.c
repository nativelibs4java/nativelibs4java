__kernel void testLongRead(     __global const long* input,
                                                               __global long* output) {
       int i = get_global_id(0);

       output[i] = input[i] + 1;
}
