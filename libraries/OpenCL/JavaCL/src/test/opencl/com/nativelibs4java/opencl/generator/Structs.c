typedef struct _S {
  int a, b;
} S;

kernel void f(__global S *pS) {
  pS->a = pS->a * 2 + 10;
  pS->b = pS->b * 10 + 100;
}
