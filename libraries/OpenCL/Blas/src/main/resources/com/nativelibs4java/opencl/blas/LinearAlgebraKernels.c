#pragma OPENCL EXTENSION cl_khr_fp64: enable

__kernel void mulMat(
   __global const double* a, /*size_t aRows,*/ size_t aColumns,
   __global const double* b, /*size_t bRows,*/ size_t bColumns,
   __global double* c
) {
    size_t i = get_global_id(0);
    size_t j = get_global_id(1);
    
    double total = 0;
    size_t iOff = i * aColumns;
    for (size_t k = 0; k < aColumns; k++) {
        total += a[iOff + k] * b[k * bColumns + j];
    }
    c[i * bColumns + j] = //a[i] / 10.0 + b[i] + i / 100.0 + j / 1000.0;//
    		total;
}

__kernel void mulVec(
   __global const double* a, /*size_t aRows,*/ size_t aColumns,
   __global const double* b, size_t bSize,
   __global double* c
) {
    size_t globalId = get_global_id(0);
    size_t i = globalId;

    double total = 0;
    size_t iOff = i * aColumns;
    for (size_t k = 0; k < aColumns; k++) {
        total += a[iOff + k] * b[k];
    }
    c[i] = total;
}

__kernel void transpose(
    __global const double* a, size_t aRows, size_t aColumns,
    __global double* out
) {
    size_t i = get_global_id(0);
    size_t j = get_global_id(1);

    size_t outColumns = aRows;
    out[i * outColumns + j] = a[j * aColumns + i];
}