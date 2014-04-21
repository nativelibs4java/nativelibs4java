typedef struct _S {
  int a, b;
} S;

kernel void f(__global S *pS) {
  pS->a = pS->a * 2 + 10;
  pS->b = pS->b * 10 + 100;
}

kernel void g(float3 floats, __global float *out) {
  out[0] = floats.x;
  out[1] = floats.y;
  out[2] = floats.z;
}