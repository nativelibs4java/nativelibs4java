#pragma OPENCL EXTENSION cl_khr_fp64: enable

__kernel void mulMat(
   __global const double* a, /*size_t aRows,*/ long aColumns,
   __global const double* b, /*size_t bRows,*/ long bColumns,
   __global double* c
) {
    size_t i = get_global_id(0);
    size_t j = get_global_id(1);
    
    double total = 0;
    //c[i, j] = sum(k, a[i, k] * b[k, j])
    
    long iOff = i * aColumns;
    for (long k = 0; k < aColumns; k++) {
        total += a[iOff + k] * b[k * bColumns + j];
    }
    c[i * bColumns + j] = //a[i] / 10.0 + b[i] + i / 100.0 + j / 1000.0;//
    		//a[i * aColumns + j];
            total;
            
    c[0] = c[1] = c[2] = c[3] = //4.81036405104E11;//
    		55;
    //c[i * bColumns + j] = a[i * bColumns + j];
    
}

__kernel void mulVec(
   __global const double* a, /*size_t aRows,*/ long aColumns,
   __global const double* b, long bSize,
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
    __global const double* a, long aRows, long aColumns,
    __global double* out
) {
    size_t i = get_global_id(0);
    size_t j = get_global_id(1);

    size_t outColumns = aRows;
    out[i * outColumns + j] = a[j * aColumns + i];
}