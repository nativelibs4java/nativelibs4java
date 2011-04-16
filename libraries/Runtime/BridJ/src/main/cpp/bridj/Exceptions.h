#pragma once
#ifndef _BRIDJ_EXCEPTIONS_H
#define _BRIDJ_EXCEPTIONS_H

#define ENABLE_PROTECTED_MODE

#if !defined(__GNUC__) && defined(ENABLE_PROTECTED_MODE)

#include <windows.h>
#include <jni.h>

#define BEGIN_TRY() { LPEXCEPTION_POINTERS exceptionPointers = NULL; __try {
#define END_TRY_BASE(env, ret) } __except ((exceptionPointers = GetExceptionInformation()) ? EXCEPTION_EXECUTE_HANDLER : EXCEPTION_CONTINUE_SEARCH) { WinExceptionHandler(env, exceptionPointers); ret; } }
#define END_TRY_RET(env, ret) END_TRY_BASE(env, return ret)
#define END_TRY(env) END_TRY_BASE(env, )

void WinExceptionHandler(JNIEnv* env, LPEXCEPTION_POINTERS ex);

#else

#define BEGIN_TRY() {
#define END_TRY(env) }
#define END_TRY_RET(env, ret) }

#endif

#endif // _BRIDJ_EXCEPTIONS_H