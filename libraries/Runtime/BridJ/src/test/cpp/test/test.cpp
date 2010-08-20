
#include "stdafx.h"
#include "test.h"
#include "jni.h"
#include "math.h"

#include <iostream>
#include <string>

using namespace std;

TEST_API int ntest=0;

TEST_API void __cdecl voidTest()
{
	//printf("ok\n");
	//cout << "Ok\n";
}
TEST_API double __cdecl sinInt(int d)
{
	return d;//sin((double)d);
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
	//cout << "other\n";
}
JNIEXPORT jint JNICALL Java_org_bridj_PerfLib_testAddJNI(JNIEnv *, jclass, jint a, jint b) {
	otherFunc();
	return a + b;
}
TEST_API int __cdecl testAddDyncall(int a, int b)
{
	//if (true)
	//	testAddDyncall(a, b);
	otherFunc();
	return a + b;
}
TEST_API int __cdecl testAddJNA(int a, int b)
{
	otherFunc();
	return a + b;
}
JNIEXPORT jdouble JNICALL Java_org_bridj_PerfLib_testASinB(JNIEnv *, jclass, jint a, jint b)
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

#if defined(DC__Arch_Intel_x86)
#include <dlfcn.h>
#include <objc/objc.h>
#include <objc/message.h>
#endif

Ctest::Ctest()
{
	cout << "Constructing Ctest instance\n";
	
	
#if defined(DC__Arch_Intel_x86)
/*
	dlopen("/System/Library/Frameworks/Foundation.framework/Foundation", RTLD_LAZY);
	{
	id clsPool = objc_getClass("NSAutoreleasePool");
	id poolInst = objc_msgSend(clsPool, sel_registerName("new"));
	printf("#\n# poolInst in Ctest::Ctest : %ld\n#\n", (long int)poolInst);
	}
	*/
#endif

	//printf("Ctest::Ctest() (this = %ld)\n", (long int)(size_t)this);
}
Ctest::~Ctest()
{
	cout << "Destructor of Ctest is called !\n";
}

const string& Ctest2::toString() {
	static string s = "";
	return s;
}

TEST_API size_t __cdecl sizeOfCtest() {
	return sizeof(Ctest);
}
TEST_API size_t __cdecl sizeOfCtest2() {
	return sizeof(Ctest2);
}
int Ctest::testVirtualAdd(int a, int b) {
	//printf("Ctest::testVirtualAdd(%d, %d) (this = %ld)\n", a, b, (long int)(size_t)this);
	return a + b;
}
int Ctest::testAdd(int a, int b) {
	//printf("Ctest::testAdd(%d, %d) (this = %ld)\n", a, b, (long int)(size_t)this);
	return a + b;
}

int Ctest::testAddStdCall(void* ptr, int a, int b) {
	//printf("Ctest::testAddStdCall(ptr, %d, %d) (this = %ld, ptr = %ld)\n", a, b, (long int)(size_t)this, (long int)(size_t)ptr);
	if (ptr)
		return 0;
	return a + b;
}

int Ctest::testVirtualAddStdCall(void* ptr, int a, int b) {
	//printf("Ctest::testVirtualAddStdCall(ptr, %d, %d) (this = %ld, ptr = %ld)\n", a, b, (long int)(size_t)this, (long int)(size_t)ptr);
	if (ptr)
		return 0;
	return a + b;
}

ETest testEnum(ETest e)
{
	return e;	
}
ETest testVoidEnum()
{
	return (ETest)0;	
}
ETest testIntEnum(int i, ETest e)
{
	return e;	
}

void Ctest::static_void() {
	
}

Ctest* createTest() {
	Ctest* test = new Ctest();
	return test;
}

Ctest2::Ctest2() : Ctest(), fState(NULL), fDestructedState(0)
{
	cout << "Constructing Ctest2 instance\n";
	//printf("Ctest2::Ctest2() (this = %ld)\n", (long int)(size_t)this);
}
Ctest2::~Ctest2()
{
	if (fState)
		*fState = fDestructedState;
	
	cout << "Destructing Ctest2 instance\n";
	
}
void Ctest2::setState(int* pState) {
	fState = pState;
}
void Ctest2::setDestructedState(int destructedState) {
	fDestructedState = destructedState;
}
	
int Ctest2::testVirtualAdd(int a, int b) {
	int ret = a + b * 2;
	//printf("Ctest2::testVirtualAdd(%d, %d) = %d (this = %ld)\n", a, b, ret, (long int)(size_t)this);
	return ret;
}
int Ctest2::testAdd(int a, int b) {
	int ret = a + b * 2;
	//printf("Ctest2::testAdd(%d, %d) = %d (this = %ld)\n", a, b, ret, (long int)(size_t)this);
	return ret;
}

TEST_API void* test_pvoid() { return NULL; }
TEST_API int* test_pint() { return NULL; }
TEST_API int test_int() { return 0; }
TEST_API void test_void_double() { return; }
TEST_API int test_int_double() { return 0; }
TEST_API int test_int_float(float) { return 0; }
TEST_API void test_void() { }

typedef void (*fun_void)();
typedef void (*fun_void_int)(int);
typedef int (*fun_int)();
typedef int (*fun_int_float)(float);
TEST_API fun_void test_fun_void() { return NULL; }
TEST_API fun_void test_fun_void__double(double) { return NULL; }
TEST_API fun_void_int test_fun_void_int() { return NULL; }
TEST_API fun_int test_fun_int() { return NULL; }
TEST_API fun_int_float test_fun_int_float() { return NULL; }
TEST_API fun_int_float test_fun_int_float__double(double) { return NULL; }


TEST_API int test_int_short(short s) { return 0; }

typedef int (*fun_iii)(int, int);
int add(int a, int b) {
	return a + b;
}
TEST_API void* getAdder_pvoid() {
	return (void*)(size_t)10;
}
TEST_API jlong getAdder_long() {
	return (jlong)10;
}
TEST_API fun_iii getAdder() {
	return add;
}
TEST_API fun_iii getAdder_raw() {
	return add;
}
TEST_API int forwardCall(fun_iii f, int a, int b) {
	int res = f(a, b);
	return res;
}

TEST_API std::string* newString() {
	return new std::string();
}
TEST_API void appendToString(std::string* s, const char* a) {
	(*s) += a;
}
TEST_API void reserveString(std::string* s, size_t reservedSize) {
	s->reserve(reservedSize);
}
TEST_API void resizeString(std::string* s, size_t newSize) {
	s->resize(newSize);
}
TEST_API void deleteString(std::string* s) {
	delete s;
}
TEST_API const char* stringCStr(std::string* s) {
	s->c_str();
}

TEST_API std::wstring* newWString() {
	return new std::wstring();
}
TEST_API void appendToWString(std::wstring* s, const wchar_t* a) {
	(*s) += a;
}
TEST_API void reserveWString(std::wstring* s, size_t reservedSize) {
	s->reserve(reservedSize);
}
TEST_API void resizeString(std::wstring* s, size_t newSize) {
	s->resize(newSize);
}
TEST_API void deleteString(std::wstring* s) {
	delete s;
}
TEST_API const char* wstringCStr(std::wstring* s) {
	s->c_str();
}

#include "../../../../target/generated-sources/test/org/bridj/CallTest.cpp"
