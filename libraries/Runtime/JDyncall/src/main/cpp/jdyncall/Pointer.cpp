#include "com_nativelibs4java_runtime_Pointer.h"

#include <string.h>
#include <wchar.h>
#include <stdlib.h>
#include "Exceptions.h"
#include "jni.h"

#ifndef __GNUC__
#pragma warning(disable: 4715)
#endif

#define FUNC_VOID_3(name, t1, t2, t3, nt1, nt2, nt3) \
void JNICALL Java_com_nativelibs4java_runtime_Pointer_ ## name(JNIEnv *env, jclass, t1 a1, t2 a2, t3 a3) \
{ \
	BEGIN_TRY(); \
	name((nt1)a1, (nt2)a2, (nt3)a3); \
	END_TRY(env); \
}

#define FUNC_3(ret, name, t1, t2, t3, nt1, nt2, nt3) \
ret JNICALL Java_com_nativelibs4java_runtime_Pointer_ ## name(JNIEnv *env, jclass, t1 a1, t2 a2, t3 a3) \
{ \
	BEGIN_TRY(); \
	return (ret)name((nt1)a1, (nt2)a2, (nt3)a3); \
	END_TRY(env); \
}

#define FUNC_VOID_1(name, t1, nt1) \
void JNICALL Java_com_nativelibs4java_runtime_Pointer_ ## name(JNIEnv *env, jclass, t1 a1) \
{ \
	BEGIN_TRY(); \
	name((nt1)a1); \
	END_TRY(env); \
}

#define FUNC_1(ret, name, t1, nt1) \
ret JNICALL Java_com_nativelibs4java_runtime_Pointer_ ## name(JNIEnv *env, jclass, t1 a1) \
{ \
	BEGIN_TRY(); \
	return (ret)name((nt1)a1); \
	END_TRY(env); \
}

FUNC_1(jlong, malloc, jlong, size_t)
FUNC_VOID_1(free, jlong, void*)

FUNC_1(jlong, strlen, jlong, char*)
FUNC_1(jlong, wcslen, jlong, wchar_t*)

FUNC_VOID_3(memcpy, jlong, jlong, jlong, void*, void*, size_t)
FUNC_VOID_3(memmove, jlong, jlong, jlong, void*, void*, size_t)

FUNC_VOID_3(wmemcpy, jlong, jlong, jlong, wchar_t*, wchar_t*, size_t)
FUNC_VOID_3(wmemmove, jlong, jlong, jlong, wchar_t*, wchar_t*, size_t)

FUNC_3(jlong, memchr, jlong, jbyte, jlong, void*, unsigned char, size_t)
FUNC_3(jint, memcmp, jlong, jlong, jlong, void*, void*, size_t)
FUNC_VOID_3(memset, jlong, jbyte, jlong, void*, unsigned char, size_t)

#include "PrimDefs_int.h"
#include "Pointer_prim.h"

#include "PrimDefs_long.h"
#include "Pointer_prim.h"

#include "PrimDefs_short.h"
#include "Pointer_prim.h"

#include "PrimDefs_byte.h"
#include "Pointer_prim.h"

#include "PrimDefs_char.h"
#include "Pointer_prim.h"

#include "PrimDefs_boolean.h"
#include "Pointer_prim.h"

#include "PrimDefs_float.h"
#include "Pointer_prim.h"

#include "PrimDefs_double.h"
#include "Pointer_prim.h"
