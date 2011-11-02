#pragma once
#ifndef _BRIDJ_EXCEPTIONS_H
#define _BRIDJ_EXCEPTIONS_H

#include <jni.h>

#define ENABLE_PROTECTED_MODE
#if defined(ENABLE_PROTECTED_MODE)

#include "bridj.hpp"
//#include "Protected.h"

#if defined(__GNUC__)

extern jboolean gProtected;
void throwSignalError(JNIEnv* env, int signal, int signalCode, jlong address);

inline jboolean DoTrapSignals(CallTempStruct* call) {
	//call->signal = call->signalCode = 0;
	//call->signalAddress = 0;
	TrapSignals(&call->signals);
	return JNI_TRUE;
}

#define BEGIN_TRY_BASE(env, call, prot) \
	if (!prot || !DoTrapSignals(call) || (call->signal = setjmp(call->exceptionContext)) == 0) \
	{
		
#define END_TRY_BASE(env, call, prot, ifProt) \
	} else { \
		throwSignalError(env, call->signal, call->signalCode, call->signalAddress); \
	} \
	if (prot) { \
		RestoreSignals(&call->signals); \
		ifProt \
	}
	
#define BEGIN_TRY(env, call) BEGIN_TRY_BASE(env, call, gProtected)
		
#define BEGIN_TRY_CALL(env) \
	{ \
		jboolean _protected = gProtected; \
		{ \
			CallTempStruct* call = _protected ? getTempCallStruct(env) : NULL; \
			BEGIN_TRY_BASE(env, call, _protected);

#define END_TRY(env, call) END_TRY_BASE(env, call, gProtected, )

#define END_TRY_CALL(env) \
			END_TRY_BASE(env, call, _protected, releaseTempCallStruct(env, call);) \
		} \
	}

#else

// WINDOWS
#define BEGIN_TRY(env, call) { LPEXCEPTION_POINTERS exceptionPointers = NULL; __try {
#define END_TRY(env, call) } __except (WinExceptionFilter(exceptionPointers = GetExceptionInformation())) { WinExceptionHandler(env, exceptionPointers); } }

#define BEGIN_TRY_CALL(env) { 
#define END_TRY_CALL(env) } 

#endif

#else

#define BEGIN_TRY(env, call) {
#define END_TRY(env, call) }

#define BEGIN_TRY_CALL(env) { 
#define END_TRY_CALL(env) } 

#endif // defined(ENABLE_PROTECTED_MODE)

void clearLastError(JNIEnv* env);
void throwIfLastError(JNIEnv* env);

#endif // _BRIDJ_EXCEPTIONS_H