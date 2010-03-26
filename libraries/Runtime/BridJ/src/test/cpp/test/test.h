// Le bloc ifdef suivant est la façon standard de créer des macros qui facilitent l'exportation 
// à partir d'une DLL. Tous les fichiers contenus dans cette DLL sont compilés avec le symbole TEST_EXPORTS
// défini sur la ligne de commande. Ce symbole ne doit pas être défini dans les projets
// qui utilisent cette DLL. De cette manière, les autres projets dont les fichiers sources comprennent ce fichier considèrent les fonctions 
// TEST_API comme étant importées à partir d'une DLL, tandis que cette DLL considère les symboles
// définis avec cette macro comme étant exportés.

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
public:
	Ctest2();
	//virtual 
	~Ctest2();
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