#ifndef _LIBCL_BITS_CL_
#define _LIBCL_BITS_CL_

/**
 * SWAR algorithm
 * http://stackoverflow.com/questions/109023/best-algorithm-to-count-the-number-of-set-bits-in-a-32-bit-integer
 */
int countBitsInInt(int i)
{
    i = i - ((i >> 1) & 0x55555555);
    i = (i & 0x33333333) + ((i >> 2) & 0x33333333);
    return ((i + (i >> 4) & 0xF0F0F0F) * 0x1010101) >> 24;
}

#define _LIBCL_BITS_CL_
