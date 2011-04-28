
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
		v.push_back((T)i);
	return v;
}
template <typename T>
int sizeofVector() {
	return sizeof(vector<T>);
}

template vector<int> TEST_API newVector<int>(int);
template vector<long long> TEST_API newVector<long long>(int);
template vector<double> TEST_API newVector<double>(int);
template vector<float> TEST_API newVector<float>(int);

template int TEST_API sizeofVector<int>();
template int TEST_API sizeofVector<long long>();
template int TEST_API sizeofVector<double>();
template int TEST_API sizeofVector<float>();

typedef vector<int> (*IntVecFun)(int);
typedef int (*SizeTFun)();

extern "C" {
TEST_API IntVecFun newIntVector = newVector<int>;
TEST_API SizeTFun sizeofIntVector = sizeofVector<int>;
}

TEST_API void toto() {}
