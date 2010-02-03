#include "jdyncall.hpp"
#include "jni.h"

// http://msdn.microsoft.com/en-us/library/ms679356(VS.85).aspx

#ifndef __GNUC__

int WinExceptionHandler(JNIEnv* env, int exceptionCode) {
	switch (exceptionCode) 
	{
#define EX_CASE(name) \
	case EXCEPTION_ ## name: \
		(*env)->ExceptionClear(env); \
		(*env)->ThrowNew(env, RuntimeException_class, #name); \
		return EXCEPTION_CONTINUE_EXECUTION;
    
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
	}
	return EXCEPTION_CONTINUE_SEARCH;
}

#endif