#include "bridj.hpp"
#include "jni.h"
#include "Exceptions.h"

#include <string.h>
#include <errno.h>

#ifdef _WIN32
#include "windows.h"
#endif

// http://msdn.microsoft.com/en-us/library/ms679356(VS.85).aspx

extern jclass gLastErrorClass;
extern jmethodID gThrowNewLastErrorMethod;

extern jclass gSignalErrorClass;
extern jmethodID gSignalErrorThrowMethod;

extern jclass gWindowsErrorClass;
extern jmethodID gWindowsErrorThrowMethod;

void throwException(JNIEnv* env, const char* message) {
	if ((*env)->ExceptionCheck(env))
		return; // there is already a pending exception
	(*env)->ExceptionClear(env);
	(*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/RuntimeException"), message ? message : "No message (TODO)");
}

void clearLastError(JNIEnv* env) {
	errno = 0;
}

#ifdef __GNUC__
void throwSignalError(JNIEnv* env, int signal, int signalCode, jlong address) {
	(*env)->CallStaticVoidMethod(env, gSignalErrorClass, gSignalErrorThrowMethod, signal, signalCode, address);
}
#else
void throwWindowsError(JNIEnv* env, int code, jlong info, jlong address) {
	(*env)->CallStaticVoidMethod(env, gWindowsErrorClass, gWindowsErrorThrowMethod, code, info, address);
}
#endif

void throwIfLastError(JNIEnv* env) {
	int errorCode = 0;
	int en = errno;
	jstring message = NULL;
#ifdef _WIN32
	errorCode = GetLastError();
	if (errorCode) {
		// http://msdn.microsoft.com/en-us/library/ms680582(v=vs.85).aspx
#define MESSAGE_BUF_SIZE 2048
		char lpMsgBuf[MESSAGE_BUF_SIZE + 1];
		*lpMsgBuf = '\0';

		FormatMessageA(
			FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
			NULL,
			errorCode,
			MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
			lpMsgBuf,
			MESSAGE_BUF_SIZE, 
			NULL 
		);
		message = (*env)->NewStringUTF(env, lpMsgBuf);
	}
#endif
	if (!errorCode) {
		errorCode = en;
		if (errorCode) {
			const char* msg = strerror(errorCode);
			message = msg ? (*env)->NewStringUTF(env, msg) : NULL;
		}
	}
	if (errorCode)
		(*env)->CallStaticVoidMethod(env, gLastErrorClass, gThrowNewLastErrorMethod, errorCode, message);
	
		/*
	errorCode = GetLastError();
	if (!errorCode)
		errorCode = errno;
#else
	errorCode = errno;
#endif
	printf("ERRNO = %d\n", errorCode);
	(*env)->CallStaticVoidMethod(env, gLastErrorClass, gThrowNewLastErrorMethod, errorCode);
	*/
}

jboolean assertThrow(JNIEnv* env, jboolean value, const char* message) {
	if (!value)
		throwException(env, message);
	return value;
}

//#if defined(ENABLE_PROTECTED_MODE)

#ifdef __GNUC__

//Signals gSignals;


void TrapSignals(Signals* s) 
{	
	struct sigaction act;
	memset(&act, 0, sizeof(struct sigaction));
	act.sa_sigaction = UnixExceptionHandler;
	act.sa_flags = SA_SIGINFO | SA_NOCLDSTOP | SA_NOCLDWAIT;
	
#define TRAP_SIG(sig) \
	sigaction(sig, &act, &s->fOld ## sig);
		
	TRAP_SIG(SIGSEGV)
	TRAP_SIG(SIGBUS)
	TRAP_SIG(SIGFPE)
	TRAP_SIG(SIGCHLD)
	TRAP_SIG(SIGILL)
	TRAP_SIG(SIGABRT)
	//TRAP_SIG(SIGTRAP)
}
void RestoreSignals(Signals* s) {
	#define UNTRAP_SIG(sig) \
		sigaction(sig, &s->fOld ## sig, NULL);
	
	UNTRAP_SIG(SIGSEGV)
	UNTRAP_SIG(SIGBUS)
	UNTRAP_SIG(SIGFPE)
	UNTRAP_SIG(SIGCHLD)
	UNTRAP_SIG(SIGILL)
	UNTRAP_SIG(SIGABRT)
	//UNTRAP_SIG(SIGTRAP)
}

void InitProtection() {
	//TrapSignals(&gSignals);
}

void CleanupProtection() {
	//RestoreSignals(&gSignals);
}

//void UnixExceptionHandler(int sig) {
void UnixExceptionHandler(int sig, siginfo_t* si, void * ctx)
{
  JNIEnv* env = GetEnv();
  CallTempStruct* call = getCurrentTempCallStruct(env);
  if (!call)
  	  return;
  
  call->signal = sig;
  call->signalCode = si->si_code;
  call->signalAddress = PTR_TO_JLONG(si->si_addr);
  
  longjmp(call->exceptionContext, sig);
}

#else

int WinExceptionFilter(LPEXCEPTION_POINTERS ex) {
	switch (ex->ExceptionRecord->ExceptionCode) {
		case 0x40010005: // Control+C
		case 0x80000003: // Breakpoint
			return EXCEPTION_CONTINUE_SEARCH;
	}
	return EXCEPTION_EXECUTE_HANDLER;
}
void WinExceptionHandler(JNIEnv* env, LPEXCEPTION_POINTERS ex) {
	int code = ex->ExceptionRecord->ExceptionCode;
	jlong info;
	void* address;

	if ((code == EXCEPTION_ACCESS_VIOLATION || code == EXCEPTION_IN_PAGE_ERROR) && ex->ExceptionRecord->NumberParameters >= 2) {
		info = ex->ExceptionRecord->ExceptionInformation[0];
		address = (void*)ex->ExceptionRecord->ExceptionInformation[1];
	} else {
		info = 0;
		address = ex->ExceptionRecord->ExceptionAddress;
	}

	throwWindowsError(env, code, info, PTR_TO_JLONG(address));
}

//#endif //defined(ENABLE_PROTECTED_MODE)

#endif
