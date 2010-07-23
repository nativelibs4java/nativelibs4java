#ifdef __GNUC__
	#define TEST_API
	#define __cdecl
	#define __stdcall
#else
	#ifdef TEST_EXPORTS
		#define TEST_API __declspec(dllexport)
	#else
		#define TEST_API __declspec(dllimport)
	#endif
#endif

#include <string>

typedef enum ETest {
	eFirst,
	eSecond,
	eThird
} ETest;

class TEST_API Ctest {
public:
	int firstField;
	int secondField;
	Ctest();
	//virtual 
	~Ctest();
	virtual int testVirtualAdd(int a, int b);
	int testAdd(int a, int b);
	virtual int __stdcall testVirtualAddStdCall(void* ptr, int a, int b);
	int __stdcall testAddStdCall(void* ptr, int a, int b);
	
	static void static_void();
};

class TEST_API Ctest2 : public Ctest {
	int* fState;
	int fDestructedState;
public:
	Ctest2();
	//virtual 
	~Ctest2();
	void setState(int* pState);
	void setDestructedState(int destructedState);
	virtual int testVirtualAdd(int a, int b);
	int testAdd(int a, int b);
	const std::string& toString();
};

extern TEST_API int ntest;
TEST_API Ctest* createTest();

TEST_API ETest testEnum(ETest e);
TEST_API ETest testVoidEnum();
TEST_API ETest testIntEnum(int i, ETest e);
TEST_API void __cdecl voidTest();
TEST_API double __cdecl sinInt(int);
TEST_API double __cdecl testSum(const double *values, size_t n);
TEST_API double __cdecl testSumi(const double *values, int n);
TEST_API long long __cdecl testSumll(const double *values, int n);
TEST_API int __cdecl testSumInt(const double *values, int n);
TEST_API void __cdecl testInPlaceSquare(double *values, size_t n);