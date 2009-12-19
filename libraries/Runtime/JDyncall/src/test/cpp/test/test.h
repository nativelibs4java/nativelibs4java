// Le bloc ifdef suivant est la façon standard de créer des macros qui facilitent l'exportation 
// à partir d'une DLL. Tous les fichiers contenus dans cette DLL sont compilés avec le symbole TEST_EXPORTS
// défini sur la ligne de commande. Ce symbole ne doit pas être défini dans les projets
// qui utilisent cette DLL. De cette manière, les autres projets dont les fichiers sources comprennent ce fichier considèrent les fonctions 
// TEST_API comme étant importées à partir d'une DLL, tandis que cette DLL considère les symboles
// définis avec cette macro comme étant exportés.
#ifdef TEST_EXPORTS
#define TEST_API __declspec(dllexport)
#else
#define TEST_API __declspec(dllimport)
#endif

// Cette classe est exportée de test.dll
class TEST_API Ctest {
public:
	Ctest(void);
	virtual int testAdd(int a, int b);
	// TODO : ajoutez ici vos méthodes.
};

extern TEST_API int ntest;
TEST_API Ctest* createTest();

TEST_API double _cdecl sinInt(int);
TEST_API double _cdecl testSum(const double *values, size_t n);
TEST_API double _cdecl testSumi(const double *values, int n);
TEST_API long long _cdecl testSumll(const double *values, int n);
TEST_API int _cdecl testSumInt(const double *values, int n);
TEST_API void _cdecl testInPlaceSquare(double *values, size_t n);