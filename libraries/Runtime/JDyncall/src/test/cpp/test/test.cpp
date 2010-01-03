// test.cpp : définit les fonctions exportées pour l'application DLL.
//

#include "stdafx.h"
#include "test.h"
#include "jni.h"
#include "math.h"

#include <iostream>

// Il s'agit d'un exemple de variable exportée
TEST_API int ntest=0;

// Il s'agit d'un exemple de fonction exportée.
TEST_API double __cdecl sinInt(int d)
{
	return sin((double)d);
}

TEST_API double __cdecl testSum(const double *values, size_t n)
{
	double total = 0;
	for (size_t i = 0; i < n; i++) {
		total += values[i];
	}
	return total;
}
TEST_API double __cdecl testSumi(const double *values, int n)
{
	double total = 0;
	for (int i = 0; i < n; i++) {
		total += values[i];
	}
	return total;
}
TEST_API long long __cdecl testSumll(const double *values, int n)
{
	long long value = (long long)testSumi(values, n);
	return value;
}
TEST_API int __cdecl testSumInt(const double *values, int n)
{
	return (int)testSum(values, n);
}
TEST_API void __cdecl testInPlaceSquare(double *values, size_t n)
{
	for (size_t i = 0; i < n; i++) {
		double value = values[i];
		values[i] = value * value;
	}
}

extern "C" {

void otherFunc() {
	//std::cout << "other\n";
}
JNIEXPORT jint JNICALL Java_jdyncall_PerfTest_testAddJNI(JNIEnv *, jclass, jint a, jint b) {
	otherFunc();
	return a + b;
}
TEST_API int __cdecl testAddDyncall(int a, int b)
{
	if (true)
		testAddDyncall(a, b);
	otherFunc();
	return a + b;
}
TEST_API int __cdecl testAddJNA(int a, int b)
{
	otherFunc();
	return a + b;
}
JNIEXPORT jdouble JNICALL Java_jdyncall_PerfTest_testASinB(JNIEnv *, jclass, jint a, jint b)
{
	otherFunc();
	return a * sin((double)b);
}
TEST_API double __cdecl testASinB(int a, int b)
{
	otherFunc();
	return a * sin((double)b);
}

}

// Il s'agit du constructeur d'une classe qui a été exportée.
// consultez test.h pour la définition de la classe
Ctest::Ctest()
{
	return;
}

int Ctest::testAdd(int a, int b) {
	return a + b;
}

Ctest* createTest() {
	Ctest* test = new Ctest();
	std::cout << test->testAdd(1, 2);
	return test;
}
