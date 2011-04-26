#pragma once
#ifndef _BRIDJ_EXCEPTIONS_H
#define _BRIDJ_EXCEPTIONS_H

#define ENABLE_PROTECTED_MODE

#if defined(ENABLE_PROTECTED_MODE)
#if defined(__GNUC__)

#include <signal.h>
#include <sys/signal.h>
#include <setjmp.h>

typedef struct Signals {
	void* fSIGSEGV;
	void* fSIGBUS;
	void* fSIGABRT;
	void* fSIGFPE; 
	void* fSIGILL; 
} Signals;

void TrapSignals(Signals* s);
void RestoreSignals(Signals* s);
void UnixExceptionHandler(int sig);

#define BEGIN_TRY(env, call) { \
	jboolean _ex_jmpError; \
	Signals _ex_signals; \
	call->throwMessage = NULL; \
	TrapSignals(&_ex_signals); \
	if ((_ex_jmpError = setjmp(call->exceptionContext)) != 0) { \
		goto _ex_catch; \
	} \
	{
		
#define END_TRY_BASE(env, call, ret) } \
	if (!_ex_jmpError) { \
		RestoreSignals(&_ex_signals); \
		goto _ex_end; \
	} \
	_ex_catch: \
		RestoreSignals(&_ex_signals); \
		if (call->throwMessage) \
			throwException(env, call->throwMessage); \
		ret; \
	_ex_end: \
		_ex_jmpError = _ex_jmpError; \
	}
	
#define END_TRY_RET(env, call, ret) END_TRY_BASE(env, call, return ret)

#else

#include <windows.h>
#include <jni.h>

#define BEGIN_TRY(env, call) { LPEXCEPTION_POINTERS exceptionPointers = NULL; __try {
#define END_TRY_BASE(env, call, ret) } __except (WinExceptionFilter(exceptionPointers = GetExceptionInformation())) { WinExceptionHandler(env, exceptionPointers); ret; } }
#define END_TRY_RET(env, call, ret) END_TRY_BASE(env, call, return ret)

void WinExceptionHandler(JNIEnv* env, LPEXCEPTION_POINTERS ex);
int WinExceptionFilter(LPEXCEPTION_POINTERS ex);

#endif

#define END_TRY(env, call) END_TRY_BASE(env, call, )
#define END_TRY_CALL(env) END_TRY_CALL_BASE(env, )

#define BEGIN_TRY_CALL(env) { \
	CallTempStruct* _try_call = getTempCallStruct(env); \
	BEGIN_TRY(env, _try_call);

#define END_TRY_CALL_BASE(env, ret) \
	cleanupCallHandler(_try_call); \
	END_TRY_BASE(env, _try_call, cleanupCallHandler(_try_call); ret); \
	}
	
#define END_TRY_CALL_RET(env, ret) \
	END_TRY_CALL_BASE(env, return ret);


#else

#define BEGIN_TRY(env) {
#define END_TRY(env) }
#define END_TRY_RET(env, ret) }

#define BEGIN_TRY_CALL(env) { 
#define END_TRY_CALL_BASE(env, ret) } 
#define END_TRY_CALL_RET(env, ret) } 



#endif // defined(ENABLE_PROTECTED_MODE)

#endif // _BRIDJ_EXCEPTIONS_H