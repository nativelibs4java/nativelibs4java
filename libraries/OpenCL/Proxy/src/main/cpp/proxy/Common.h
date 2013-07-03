#ifndef __JAVACL_PROXY_COMMON_H
#define __JAVACL_PROXY_COMMON_H

// /System/Library/Frameworks/OpenCL.framework/Headers/cl.h
// http://manpages.ubuntu.com/manpages/raring/man7/libOpenCL.7.html
// http://www.khronos.org/registry/cl/extensions/khr/cl_khr_icd.txt
// git clone https://forge.imag.fr/anonscm/git/ocl-icd/ocl-icd.git

#include <jni.h>

#ifdef __GNUC__
	#define JAVACL_PROXY_API
	#define __cdecl
	#define __stdcall
#else
	#ifdef JAVACL_PROXY_EXPORTS
		#define JAVACL_PROXY_API __declspec(dllexport)
	#else
		#define JAVACL_PROXY_API __declspec(dllimport)
	#endif
#endif

JNIEnv* GetEnv();

#define GLOBAL_REF(v) (*env)->NewGlobalRef(env, v)
#define DEL_GLOBAL_REF(v) (*env)->DeleteGlobalRef(env, v)

#define PROXY_INTERFACE_SIG "com/nativelibs4java/opencl/library/IOpenCLLibrary"

struct _cl_icd_dispatch;

#endif // __JAVACL_PROXY_COMMON_H
