#pragma once
#ifndef _BRIDJ_EXCEPTIONS_H
#define _BRIDJ_EXCEPTIONS_H

#ifndef __GNUC__

#include <windows.h>
#include <jni.h>
#define BEGIN_TRY() __try {
#define END_TRY(env) } __except (WinExceptionHandler(env, GetExceptionCode())) {}
#define END_TRY_RET(env, ret) } __except (WinExceptionHandler(env, GetExceptionCode())) { return ret; }

int WinExceptionHandler(JNIEnv* env, int exceptionCode);

#else

#define BEGIN_TRY()
#define END_TRY(env)

#endif

#endif // _BRIDJ_EXCEPTIONS_H