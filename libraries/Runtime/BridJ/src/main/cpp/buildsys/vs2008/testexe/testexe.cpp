// testexe.cpp : définit le point d'entrée pour l'application console.
//

#include "stdafx.h"

#include "dynload/dynload.h"
#include "dyncall/dyncall.h"

#include <Objbase.h>
#include <shobjidl.h>
#include <exdisp.h>

int f() {
	return 10;
}
int _tmain(int argc, _TCHAR* argv[])
{
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
	dcArgPointer(vm, NULL);
	ret = dcCallInt(vm, fCoInitialize);
	
	return 0;
}

