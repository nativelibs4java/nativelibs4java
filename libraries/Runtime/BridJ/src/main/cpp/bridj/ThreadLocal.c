#include "bridj.hpp"
#include <string.h>
#include "Exceptions.h"

extern jclass gBridJClass;
extern jmethodID gGetTempCallStruct;
extern jmethodID gReleaseTempCallStruct;

#if defined(DC__OS_Win64) || defined(DC__OS_Win32)

#include <windows.h>

DWORD gTlsIndex = TLS_OUT_OF_INDEXES;

typedef struct CallTempStructNode {
	struct CallTempStruct fCallTempStruct;
	struct CallTempStructNode* fPrevious;
	struct CallTempStructNode* fNext;
	BOOL fUsed;
} CallTempStructNode;

CallTempStructNode* NewNode(CallTempStructNode* pPrevious) {
	CallTempStructNode* pNode = MALLOC_STRUCT(CallTempStructNode);
	memset(pNode, 0, sizeof(CallTempStructNode));
	pNode->fCallTempStruct.vm = dcNewCallVM(1024);
	if (pPrevious) {
		pPrevious->fNext = pNode;
		pNode->fPrevious = pPrevious;
	}
	return pNode;
}
CallTempStruct* getTempCallStruct(JNIEnv* env) {
	CallTempStructNode* pNode = (CallTempStructNode*)TlsGetValue(gTlsIndex);
	if (!pNode) {
		pNode = NewNode(NULL);
		TlsSetValue(gTlsIndex, pNode);
	}

	if (pNode->fUsed) {
		if (!pNode->fNext)
			pNode->fNext = NewNode(pNode);
		
		pNode = pNode->fNext;
		TlsSetValue(gTlsIndex, pNode);
	}
	pNode->fUsed = JNI_TRUE;
	return &pNode->fCallTempStruct;
}

void releaseTempCallStruct(JNIEnv* env, CallTempStruct* s) {
	CallTempStructNode* pNode = (CallTempStructNode*)TlsGetValue(gTlsIndex);
	if (!pNode || &pNode->fCallTempStruct != s) {
		throwException(env, "Invalid thread-local status : critical bug !");
		return;
	}
	pNode->fUsed = JNI_FALSE;
	if (pNode->fPrevious)
		TlsSetValue(gTlsIndex, pNode->fPrevious);
}

void freeCurrentThreadLocalData() {
	CallTempStructNode* pNode = (CallTempStructNode*)TlsGetValue(gTlsIndex);
	if (pNode) {
		do {
			CallTempStructNode* pNext = pNode->fNext;
			free(pNode);
			pNode = pNext;
		} while (pNode);
		TlsSetValue(gTlsIndex, NULL);
	}
}

void initThreadLocal(JNIEnv* env) {
	gTlsIndex = TlsAlloc();
	if (gTlsIndex == TLS_OUT_OF_INDEXES) {
		throwException(env, "Failed to initialize the thread-local mechanism !");
		return;
	}
}

BOOL WINAPI DllMain(HINSTANCE hinstDLL, DWORD fdwReason, LPVOID lpvReserved)                
{ 
    switch (fdwReason) 
    {
	case DLL_PROCESS_ATTACH:
		break;
	case DLL_THREAD_ATTACH:
		break;
	case DLL_THREAD_DETACH:
		freeCurrentThreadLocalData();
		break;
	case DLL_PROCESS_DETACH:
		if (gTlsIndex != TLS_OUT_OF_INDEXES) {
			freeCurrentThreadLocalData();
			TlsFree(gTlsIndex);
		}
		break;
	default:
		break;
    }
 
    return TRUE; 
    UNREFERENCED_PARAMETER(hinstDLL); 
    UNREFERENCED_PARAMETER(lpvReserved); 
}

#else

void initThreadLocal(JNIEnv* env) {
}
CallTempStruct* getTempCallStruct(JNIEnv* env) {
	jlong handle = (*env)->CallStaticLongMethod(env, gBridJClass, gGetTempCallStruct);
	return (CallTempStruct*)JLONG_TO_PTR(handle);
}
void releaseTempCallStruct(JNIEnv* env, CallTempStruct* s) {
	//s->env = NULL;
	jlong h = PTR_TO_JLONG(s);
	(*env)->CallStaticVoidMethod(env, gBridJClass, gReleaseTempCallStruct, h);
}

#endif
