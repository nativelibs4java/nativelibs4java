#include "bridj.hpp"
#include "jni.h"

// http://msdn.microsoft.com/en-us/library/ms679356(VS.85).aspx

void throwException(JNIEnv* env, const char* message) {
	if ((*env)->ExceptionCheck(env))
		return; // there is already a pending exception
	(*env)->ExceptionClear(env);
	(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/RuntimeException"), message);
}

jboolean assertThrow(JNIEnv* env, jboolean value, const char* message) {
	if (!value)
		throwException(env, message);
	return value;
}


#ifndef __GNUC__

#include <windows.h>

int WinExceptionFilter(LPEXCEPTION_POINTERS ex) {
	switch (ex->ExceptionRecord->ExceptionCode) {
		case 0x40010005: // Control+C
		case 0x80000003: // Breakpoint
			return EXCEPTION_CONTINUE_SEARCH;
	}
	return EXCEPTION_EXECUTE_HANDLER;
}
void WinExceptionHandler(JNIEnv* env, LPEXCEPTION_POINTERS ex) {
	char msg[256];
	//printStackTrace(env);
	if (ex->ExceptionRecord)
		sprintf(msg, "Native exception (code = 0x%llX)", (unsigned long long)ex->ExceptionRecord->ExceptionCode);
	else
		sprintf(msg, "Native exception (unknown code)");

	throwException(env, msg);
	//(*env)->ExceptionClear(env);
	//(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/RuntimeException"), msg);

	//if ((*env)->ExceptionOccurred(env)) {
		(*env)->ExceptionDescribe(env);
    //}
	/*
	switch (ex->ExceptionRecord->ExceptionCode) 
	{
#define EX_CASE(name) \
	case EXCEPTION_ ## name: \
		(*env)->ExceptionClear(env); \
		(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/RuntimeException"), #name); \
		break;
    
	EX_CASE(ACCESS_VIOLATION           );
	EX_CASE(ARRAY_BOUNDS_EXCEEDED      );
	EX_CASE(BREAKPOINT                 );
	EX_CASE(DATATYPE_MISALIGNMENT      );
	EX_CASE(FLT_DENORMAL_OPERAND       );
	EX_CASE(FLT_DIVIDE_BY_ZERO         );
	EX_CASE(FLT_INEXACT_RESULT         );
	EX_CASE(FLT_INVALID_OPERATION      );
	EX_CASE(FLT_OVERFLOW               );
	EX_CASE(FLT_STACK_CHECK            );
	EX_CASE(FLT_UNDERFLOW              );
	EX_CASE(GUARD_PAGE                 );
	EX_CASE(ILLEGAL_INSTRUCTION        );
	EX_CASE(IN_PAGE_ERROR              );
	EX_CASE(INT_DIVIDE_BY_ZERO         );
	EX_CASE(INT_OVERFLOW               );
	EX_CASE(INVALID_DISPOSITION        );
	EX_CASE(INVALID_HANDLE             );
	EX_CASE(NONCONTINUABLE_EXCEPTION   );
	EX_CASE(PRIV_INSTRUCTION           );
	EX_CASE(SINGLE_STEP                );
	EX_CASE(STACK_OVERFLOW             );
	//EX_CASE(STATUS_UNWIND_CONSOLIDATE            );
	}*/
}

#endif