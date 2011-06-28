#pragma OPENCL EXTENSION cl_khr_fp64: enable

__kernel void mulMatDouble(
   __global const double* a, /*size_t aRows,*/ int aColumns,
   __global const double* b, /*size_t bRows,*/ int bColumns,
   __global double* c
) {
    int i = get_global_id(0);
    int j = get_global_id(1);
    
    double total = 0;
    // c[i, j] = sum(a[i, k] * b[k, j])
    int iAOff = i * aColumns;
    for (int k = 0; k < aColumns; k++) {
        total += a[iAOff + k] * b[k * bColumns + j];
    }
    c[i * bColumns + j] = total;
}

__kernel void mulVecDouble(
   __global const double* a, /*size_t aRows,*/ int aColumns,
   __global const double* b, int bSize,
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

__kernel void transposeDouble(
    __global const double* a, int aRows, int aColumns,
    __global double* out
) {
    int i = get_global_id(0);
    int j = get_global_id(1);

    int outColumns = aRows;
    out[i * outColumns + j] = a[j * aColumns + i];
}

__kernel void mulMatFloat(
   __global const float* a, /*size_t aRows,*/ int aColumns,
   __global const float* b, /*size_t bRows,*/ int bColumns,
   __global float* c
) {
    int i = get_global_id(0);
    int j = get_global_id(1);
    
    float total = 0;
    int iOff = i * aColumns;
    for (int k = 0; k < aColumns; k++) {
        total += a[iOff + k] * b[k * bColumns + j];
    }
    c[i * bColumns + j] = total;
    // c[0] = a[0];//total;
    // c[1] = a[1];//total;
    // c[2] = a[2];//total;
    // c[3] = a[3];//total;
}

__kernel void mulVecFloat(
   __global const float* a, /*size_t aRows,*/ int aColumns,
   __global const float* b, int bSize,
   __global float* c
) {
    int globalId = get_global_id(0);
    int i = globalId;

    float total = 0;
    int iOff = i * aColumns;
    for (size_t k = 0; k < aColumns; k++) {
        total += a[iOff + k] * b[k];
    }
    c[i] = total;
}

__kernel void transposeFloat(
    __global const float* a, int aRows, int aColumns,
    __global float* out
) {
    int i = get_global_id(0);
    int j = get_global_id(1);

    int outColumns = aRows;
    out[i * outColumns + j] = a[j * aColumns + i];
}