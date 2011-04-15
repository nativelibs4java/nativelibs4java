
#include "stdafx.h"
#include "test.h"

#include <iostream>
#include <vector>
#include <string>

using namespace std;


template <typename T>
vector<T> newVector(int n) {
	vector<T> v;
	for (int i = 0; i < n; i++)
		v.push_back(i);
	return v;
}
template <typename T>
int sizeofVector() {
	return sizeof(vector<T>);
}

TEST_API template vector<int> newVector<int>(int);
TEST_API template vector<long long> newVector<long long>(int);
TEST_API template vector<double> newVector<double>(int);
TEST_API template vector<float> newVector<float>(int);

TEST_API template int sizeofVector<int>();
TEST_API template int sizeofVector<long long>();
TEST_API template int sizeofVector<double>();
TEST_API template int sizeofVector<float>();

typedef vector<int> (*IntVecFun)(int);
typedef int (*SizeTFun)();
TEST_API IntVecFun newIntVector = newVector<int>;
TEST_API SizeTFun sizeofIntVector = sizeofVector<int>;

TEST_API void toto() {}
