/* ============================================================

Copyright (c) 2009 Advanced Micro Devices, Inc.  All rights reserved.

Redistribution and use of this material is permitted under the following
conditions:

Redistributions must retain the above copyright notice and all terms of this
license.

In no event shall anyone redistributing or accessing or using this material
commence or participate in any arbitration or legal action relating to this
material against Advanced Micro Devices, Inc. or any copyright holders or
contributors. The foregoing shall survive any expiration or termination of
this license or any agreement or access or use related to this material.

ANY BREACH OF ANY TERM OF THIS LICENSE SHALL RESULT IN THE IMMEDIATE REVOCATION
OF ALL RIGHTS TO REDISTRIBUTE, ACCESS OR USE THIS MATERIAL.

THIS MATERIAL IS PROVIDED BY ADVANCED MICRO DEVICES, INC. AND ANY COPYRIGHT
HOLDERS AND CONTRIBUTORS "AS IS" IN ITS CURRENT CONDITION AND WITHOUT ANY
REPRESENTATIONS, GUARANTEE, OR WARRANTY OF ANY KIND OR IN ANY WAY RELATED TO
SUPPORT, INDEMNITY, ERROR FREE OR UNINTERRUPTED OPERATION, OR THAT IT IS FREE
FROM DEFECTS OR VIRUSES.  ALL OBLIGATIONS ARE HEREBY DISCLAIMED - WHETHER
EXPRESS, IMPLIED, OR STATUTORY - INCLUDING, BUT NOT LIMITED TO, ANY IMPLIED
WARRANTIES OF TITLE, MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE,
ACCURACY, COMPLETENESS, OPERABILITY, QUALITY OF SERVICE, OR NON-INFRINGEMENT.
IN NO EVENT SHALL ADVANCED MICRO DEVICES, INC. OR ANY COPYRIGHT HOLDERS OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, PUNITIVE,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, REVENUE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED OR BASED ON ANY THEORY OF LIABILITY
ARISING IN ANY WAY RELATED TO THIS MATERIAL, EVEN IF ADVISED OF THE POSSIBILITY
OF SUCH DAMAGE. THE ENTIRE AND AGGREGATE LIABILITY OF ADVANCED MICRO DEVICES,
INC. AND ANY COPYRIGHT HOLDERS AND CONTRIBUTORS SHALL NOT EXCEED TEN DOLLARS
(US $10.00). ANYONE REDISTRIBUTING OR ACCESSING OR USING THIS MATERIAL ACCEPTS
THIS ALLOCATION OF RISK AND AGREES TO RELEASE ADVANCED MICRO DEVICES, INC. AND
ANY COPYRIGHT HOLDERS AND CONTRIBUTORS FROM ANY AND ALL LIABILITIES,
OBLIGATIONS, CLAIMS, OR DEMANDS IN EXCESS OF TEN DOLLARS (US $10.00). THE
FOREGOING ARE ESSENTIAL TERMS OF THIS LICENSE AND, IF ANY OF THESE TERMS ARE
CONSTRUED AS UNENFORCEABLE, FAIL IN ESSENTIAL PURPOSE, OR BECOME VOID OR
DETRIMENTAL TO ADVANCED MICRO DEVICES, INC. OR ANY COPYRIGHT HOLDERS OR
CONTRIBUTORS FOR ANY REASON, THEN ALL RIGHTS TO REDISTRIBUTE, ACCESS OR USE
THIS MATERIAL SHALL TERMINATE IMMEDIATELY. MOREOVER, THE FOREGOING SHALL
SURVIVE ANY EXPIRATION OR TERMINATION OF THIS LICENSE OR ANY AGREEMENT OR
ACCESS OR USE RELATED TO THIS MATERIAL.

NOTICE IS HEREBY PROVIDED, AND BY REDISTRIBUTING OR ACCESSING OR USING THIS
MATERIAL SUCH NOTICE IS ACKNOWLEDGED, THAT THIS MATERIAL MAY BE SUBJECT TO
RESTRICTIONS UNDER THE LAWS AND REGULATIONS OF THE UNITED STATES OR OTHER
COUNTRIES, WHICH INCLUDE BUT ARE NOT LIMITED TO, U.S. EXPORT CONTROL LAWS SUCH
AS THE EXPORT ADMINISTRATION REGULATIONS AND NATIONAL SECURITY CONTROLS AS
DEFINED THEREUNDER, AS WELL AS STATE DEPARTMENT CONTROLS UNDER THE U.S.
MUNITIONS LIST. THIS MATERIAL MAY NOT BE USED, RELEASED, TRANSFERRED, IMPORTED,
EXPORTED AND/OR RE-EXPORTED IN ANY MANNER PROHIBITED UNDER ANY APPLICABLE LAWS,
INCLUDING U.S. EXPORT CONTROL LAWS REGARDING SPECIFICALLY DESIGNATED PERSONS,
COUNTRIES AND NATIONALS OF COUNTRIES SUBJECT TO NATIONAL SECURITY CONTROLS.
MOREOVER, THE FOREGOING SHALL SURVIVE ANY EXPIRATION OR TERMINATION OF ANY
LICENSE OR AGREEMENT OR ACCESS OR USE RELATED TO THIS MATERIAL.

NOTICE REGARDING THE U.S. GOVERNMENT AND DOD AGENCIES: This material is
provided with "RESTRICTED RIGHTS" and/or "LIMITED RIGHTS" as applicable to
computer software and technical data, respectively. Use, duplication,
distribution or disclosure by the U.S. Government and/or DOD agencies is
subject to the full extent of restrictions in all applicable regulations,
including those found at FAR52.227 and DFARS252.227 et seq. and any successor
regulations thereof. Use of this material by the U.S. Government and/or DOD
agencies is acknowledgment of the proprietary rights of any copyright holders
and contributors, including those of Advanced Micro Devices, Inc., as well as
the provisions of FAR52.227-14 through 23 regarding privately developed and/or
commercial computer software.

This license forms the entire agreement regarding the subject matter hereof and
supersedes all proposals and prior discussions and writings between the parties
with respect thereto. This license does not affect any ownership, rights, title,
or interest in, or relating to, this material. No terms of this license can be
modified or waived, and no breach of this license can be excused, unless done
so in a writing signed by all affected parties. Each term of this license is
separately enforceable. If any term of this license is determined to be or
becomes unenforceable or illegal, such term shall be reformed to the minimum
extent necessary in order for this license to remain in effect in accordance

with its terms as modified by such reformation. This license shall be governed
by and construed in accordance with the laws of the State of Texas without
regard to rules on conflicts of law of any state or jurisdiction or the United
Nations Convention on the International Sale of Goods. All disputes arising out
of this license shall be subject to the jurisdiction of the federal and state
courts in Austin, Texas, and all defenses are hereby waived concerning personal
jurisdiction and venue of these courts.

============================================================ */

#ifndef CL_PLATFORM_H_
#define CL_PLATFORM_H_

#if !defined(_WIN32)
# include <stdint.h>
#endif /* !_WIN32 */
#include <stddef.h>
#include <float.h>

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#ifdef _MSC_VER
# define __CL_IMPORT __declspec(dllimport)
# define __CL_EXPORT
# define __CL_CALL   __stdcall
# define __CL_SUFFIX
#else
# define __CL_IMPORT
# define __CL_EXPORT
# define __CL_CALL
# define __CL_SUFFIX
#endif

#define CL_API_CALL   __CL_CALL
#define CL_API_SUFFIX __CL_SUFFIX
#define CL_API_SUFFIX__VERSION_1_0 CL_API_SUFFIX
#define CL_EXTENSION_WEAK_LINK

#ifdef OPENCL_EXPORTS
# define CL_API_ENTRY __CL_EXPORT
#else /* !OPENCL_EXPORTS */
# define CL_API_ENTRY __CL_IMPORT
#endif /* !OPENCL_EXPORTS */

#if defined(_WIN32)
typedef signed   __int8   cl_char;
typedef unsigned __int8   cl_uchar;
typedef signed   __int16  cl_short;
typedef unsigned __int16  cl_ushort;
typedef signed   __int32  cl_int;
typedef unsigned __int32  cl_uint;
typedef signed   __int64  cl_long;
typedef unsigned __int64  cl_ulong;
typedef unsigned __int16  cl_half;
#else /* !_WIN32 */
typedef int8_t    cl_char;
typedef uint8_t   cl_uchar;
typedef int16_t   cl_short;
typedef uint16_t  cl_ushort;
typedef int32_t   cl_int;
typedef uint32_t  cl_uint;
typedef int64_t   cl_long;
typedef uint64_t  cl_ulong;
typedef uint16_t  cl_half;
#endif /* !_WIN32 */
typedef float     cl_float;
typedef double    cl_double;

typedef union cl_half2    { cl_half  i16[2]; }   cl_half2;
typedef union cl_half4    { cl_half  i16[4]; }   cl_half4;
#if defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))
typedef cl_half   cl_half8    __attribute__ ((__vector_size__ (16)));
typedef cl_half   cl_half16   __attribute__ ((__vector_size__ (32)));
#else /*defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))*/
typedef union cl_half8    { cl_half  i16[8]; }   cl_half8;
typedef union cl_half16   { cl_half  i16[16]; }  cl_half16;
#endif /*defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))*/
typedef union cl_char2    { cl_char  i8[2]; }    cl_char2;
typedef union cl_char4    { cl_char  i8[4]; }    cl_char4;
typedef union cl_char8    { cl_char  i8[8]; }    cl_char8;
#if defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))
typedef cl_char   cl_char16   __attribute__ ((__vector_size__ (16)));
#else /*defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))*/
typedef union cl_char16   { cl_char  i8[16]; }   cl_char16;
#endif /*defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))*/
typedef union cl_uchar2   { cl_uchar u8[2]; }    cl_uchar2;
typedef union cl_uchar4   { cl_uchar u8[4]; }    cl_uchar4;
typedef union cl_uchar8   { cl_uchar u8[8]; }    cl_uchar8;
#if defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))
typedef cl_uchar  cl_uchar16  __attribute__ ((__vector_size__ (16)));
#else /*defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))*/
typedef union cl_uchar16  { cl_uchar u8[16]; }   cl_uchar16;
#endif /*defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))*/
typedef union cl_short2   { cl_short  i16[2]; }  cl_short2;
typedef union cl_short4   { cl_short  i16[4]; }  cl_short4;
#if defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))
typedef cl_short  cl_short8   __attribute__ ((__vector_size__ (16)));
typedef cl_short  cl_short16  __attribute__ ((__vector_size__ (32)));
#else /*defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))*/
typedef union cl_short8   { cl_short  i16[8]; }  cl_short8;
typedef union cl_short16  { cl_short  i16[16]; } cl_short16;
#endif /*defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))*/
typedef union cl_ushort2  { cl_ushort u16[2]; }  cl_ushort2;
typedef union cl_ushort4  { cl_ushort u16[4]; }  cl_ushort4;
#if defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))
typedef cl_ushort cl_ushort8  __attribute__ ((__vector_size__ (16)));
typedef cl_ushort cl_ushort16 __attribute__ ((__vector_size__ (32)));
#else /*defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))*/
typedef union cl_ushort8  { cl_ushort u16[8]; }  cl_ushort8;
typedef union cl_ushort16 { cl_ushort u16[16]; } cl_ushort16;
#endif /*defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))*/
typedef union cl_int2     { cl_int  i32[2]; }    cl_int2;
#if defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))
typedef cl_int    cl_int4     __attribute__ ((__vector_size__ (16)));
typedef cl_int    cl_int8     __attribute__ ((__vector_size__ (32)));
typedef cl_int    cl_int16    __attribute__ ((__vector_size__ (64)));
#else /*defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))*/
typedef union cl_int4     { cl_int  i32[4]; }    cl_int4;
typedef union cl_int8     { cl_int  i32[8]; }    cl_int8;
typedef union cl_int16    { cl_int  i32[16]; }   cl_int16;
#endif /*defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))*/
typedef union cl_uint2    { cl_uint u32[2]; }    cl_uint2;
#if defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))
typedef cl_uint   cl_uint4    __attribute__ ((__vector_size__ (16)));
typedef cl_uint   cl_uint8    __attribute__ ((__vector_size__ (32)));
typedef cl_uint   cl_uint16   __attribute__ ((__vector_size__ (64)));
#else /*defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))*/
typedef union cl_uint4    { cl_uint u32[4]; }    cl_uint4;
typedef union cl_uint8    { cl_uint u32[8]; }    cl_uint8;
typedef union cl_uint16   { cl_uint u32[16]; }   cl_uint16;
#endif /*defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))*/
#if defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))
typedef cl_long   cl_long2    __attribute__ ((__vector_size__ (16)));
typedef cl_long   cl_long4    __attribute__ ((__vector_size__ (32)));
typedef cl_long   cl_long8    __attribute__ ((__vector_size__ (64)));
typedef cl_long   cl_long16   __attribute__ ((__vector_size__ (128)));
#else /*defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))*/
typedef union cl_long2    { cl_long  i64[2]; }   cl_long2;
typedef union cl_long4    { cl_long  i64[4]; }   cl_long4;
typedef union cl_long8    { cl_long  i64[8]; }   cl_long8;
typedef union cl_long16   { cl_long  i64[16]; }  cl_long16;
#endif /*defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))*/
#if defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))
typedef cl_ulong  cl_ulong2   __attribute__ ((__vector_size__ (16)));
typedef cl_ulong  cl_ulong4   __attribute__ ((__vector_size__ (32)));
typedef cl_ulong  cl_ulong8   __attribute__ ((__vector_size__ (64)));
typedef cl_ulong  cl_ulong16  __attribute__ ((__vector_size__ (128)));
#else /*defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))*/
typedef union cl_ulong2   { cl_ulong u64[2]; }   cl_ulong2;
typedef union cl_ulong4   { cl_ulong u64[4]; }   cl_ulong4;
typedef union cl_ulong8   { cl_ulong u64[8]; }   cl_ulong8;
typedef union cl_ulong16  { cl_ulong u64[16]; }  cl_ulong16;
#endif /*defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))*/
typedef union cl_float2   { cl_float  f32[2]; }  cl_float2;
#if defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))
typedef cl_float  cl_float4   __attribute__ ((__vector_size__ (16)));
typedef cl_float  cl_float8   __attribute__ ((__vector_size__ (32)));
typedef cl_float  cl_float16  __attribute__ ((__vector_size__ (64)));
#else /*defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))*/
typedef union cl_float4   { cl_float  f32[4]; }  cl_float4;
typedef union cl_float8   { cl_float  f32[8]; }  cl_float8;
typedef union cl_float16  { cl_float  f32[16]; } cl_float16;
#endif /*defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))*/
#if defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))
typedef cl_double cl_double2  __attribute__ ((__vector_size__ (16)));
typedef cl_double cl_double4  __attribute__ ((__vector_size__ (32)));
typedef cl_double cl_double8  __attribute__ ((__vector_size__ (64)));
typedef cl_double cl_double16 __attribute__ ((__vector_size__ (128)));
#else /*defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))*/
typedef union cl_double2  { cl_double f64[2]; }  cl_double2;
typedef union cl_double4  { cl_double f64[4]; }  cl_double4;
typedef union cl_double8  { cl_double f64[8]; }  cl_double8;
typedef union cl_double16 { cl_double f64[16]; } cl_double16;
#endif /*defined(__SSE__) && (defined(__GNUC__) || defined(__INTEL_COMPILER))*/

#define CL_CHAR_BIT         8
#define CL_SCHAR_MAX        127
#define CL_SCHAR_MIN        (-127-1)
#define CL_CHAR_MAX         CL_SCHAR_MAX
#define CL_CHAR_MIN         CL_SCHAR_MIN
#define CL_UCHAR_MAX        255
#define CL_SHRT_MAX         32767
#define CL_SHRT_MIN         (-32767-1)
#define CL_USHRT_MAX        65535
#define CL_INT_MAX          2147483647
#define CL_INT_MIN          (-2147483647-1)
#define CL_UINT_MAX         0xffffffffU
#define CL_LONG_MAX         ((cl_long) 0x7FFFFFFFFFFFFFFFLL)
#define CL_LONG_MIN         ((cl_long) -0x7FFFFFFFFFFFFFFFLL - 1LL)
#define CL_ULONG_MAX        ((cl_ulong) 0xFFFFFFFFFFFFFFFFULL)

#define CL_FLT_DIG          6
#define CL_FLT_MANT_DIG     24
#define CL_FLT_MAX_10_EXP   +38
#define CL_FLT_MAX_EXP      +128
#define CL_FLT_MIN_10_EXP   -37
#define CL_FLT_MIN_EXP      -125
#define CL_FLT_RADIX        2
#define CL_FLT_MAX          FLT_MAX
#define CL_FLT_MIN          FLT_MIN
#define CL_FLT_EPSILON      FLT_EPSILON

#define CL_DBL_DIG          15
#define CL_DBL_MANT_DIG     53
#define CL_DBL_MAX_10_EXP   +308
#define CL_DBL_MAX_EXP      +1024
#define CL_DBL_MIN_10_EXP   -307
#define CL_DBL_MIN_EXP      -1021
#define CL_DBL_RADIX        2
#define CL_DBL_MAX          DBL_MAX
#define CL_DBL_MIN          DBL_MIN
#define CL_DBL_EPSILON      DBL_EPSILON

#ifdef __cplusplus
} /* extern "C" */
#endif /* __cplusplus */

#endif /* CL_PLATFORM_H_ */
