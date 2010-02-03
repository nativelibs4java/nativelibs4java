#pragma once
#ifndef _JDYNCALL_EXCEPTIONS_H
#define _JDYNCALL_EXCEPTIONS_H

#ifndef __GNUC__

#include <windows.h>
#include <jni.h>
#define BEGIN_TRY() __try {
#define END_TRY(env) } __except (WinExceptionHandler(env, GetExceptionCode())) {}

int WinExceptionHandler(JNIEnv* env, int exceptionCode);

#else

#define BEGIN_TRY()
#define END_TRY(env)

#endif

#endif // _JDYNCALL_EXCEPTIONS_H