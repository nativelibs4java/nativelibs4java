// testexe.cpp : définit le point d'entrée pour l'application console.
//

#include "stdafx.h"

#include "dynload/dynload.h"
#include "dyncall/dyncall.h"
#include "d:\Experiments\n4\libraries\Runtime\BridJ\src\main\cpp\bridj\RawNativeForwardCallback.h"

#include <Objbase.h>
#include <shobjidl.h>
#include <exdisp.h>
#include <shobjidl.h>

int f() {
	return 10;
}
int fVarArgs(int i, ...) {
	printf("i = %d\n", i);
	return i * 2;
}

__declspec(dllimport) long test_incr_int(long value);

void fOneInt(int a) {
	printf("i = %d\n", a);
}
void fTwoInts(int a, int b) {
	printf("i = %d, %d\n", a, b);
}
void fOneDouble(double a) {
	printf("i = %f\n", a);
}

int _tmain(int argc, _TCHAR* argv[])
{
	int (*fSkipped)(void*, void*, int);
	DCAdapterCallback* cb = dcRawCallAdapterSkipTwoArgs((void (*)())test_incr_int, DC_CALL_C_DEFAULT);
	fSkipped = (int (*)(void*, void*, int))cb;

	int rrr = fSkipped((void*)1, (void*)2, 3);
	int a = f();
	if (a != 0) {
		printf("ok");
	}

	int ret = CoInitialize(NULL);
	void* instance;

	int s = sizeof(GUID);
	char *cls = (char*)&CLSID_ShellWindows, *uid = (char*)&IID_IShellWindows;
	//ret = CoCreateInstance(CLSID_ShellFSFolder, NULL, 0, IID_IShellFolder, &instance);
	ret = CoCreateInstance(CLSID_ShellWindows, NULL, CLSCTX_ALL, IID_IShellWindows, &instance);

	int ok = S_OK;

	DLLib* lib = dlLoadLibrary("Ole32.dll");
	void* fCoInitialize = dlFindSymbol(lib, "CoInitialize");
	DCCallVM* vm = dcNewCallVM(1024);
	dcReset(vm);

	//dcMode(vm, DC_CALL_C_DEFAULT);
	dcMode(vm, DC_CALL_C_X86_WIN32_STD);
	dcArgInt(vm, 10);
	ret = dcCallInt(vm, fVarArgs);

	dcMode(vm, DC_CALL_C_X86_WIN32_STD);
	dcArgPointer(vm, NULL);
	ret = dcCallInt(vm, fCoInitialize);
	
	return 0;
}

