#ifndef __OPENCL_CL_JAVACL_H
#define __OPENCL_CL_JAVACL_H

#include <CL/cl_platform.h>
#include <CL/cl.h>
#include <CL/cl_gl.h>


typedef struct _cl_platform_id *    cl_platform_id;
typedef struct _cl_device_id *      cl_device_id;
typedef struct _cl_context *        cl_context;
typedef struct _cl_command_queue *  cl_command_queue;
typedef struct _cl_mem *            cl_mem;
typedef struct _cl_program *        cl_program;
typedef struct _cl_kernel *         cl_kernel;
typedef struct _cl_event *          cl_event;
typedef struct _cl_sampler *        cl_sampler;


typedef cl_uint     cl_gl_object_type;
typedef cl_uint     cl_gl_texture_info;
typedef cl_uint     cl_gl_platform_info;


typedef cl_uint             cl_bool;                     /* WARNING!  Unlike cl_ types in cl_platform.h, cl_bool is not guaranteed to be the same size as the bool in kernels. */ 
typedef cl_ulong            cl_bitfield;
typedef cl_bitfield         cl_device_type;
typedef cl_uint             cl_platform_info;
typedef cl_uint             cl_device_info;
typedef cl_bitfield         cl_device_address_info;
typedef cl_bitfield         cl_device_fp_config;
typedef cl_uint             cl_device_mem_cache_type;
typedef cl_uint             cl_device_local_mem_type;
typedef cl_bitfield         cl_device_exec_capabilities;
typedef cl_bitfield         cl_command_queue_properties;

typedef intptr_t			cl_context_properties;
typedef cl_uint             cl_context_info;
typedef cl_uint             cl_command_queue_info;
typedef cl_uint             cl_channel_order;
typedef cl_uint             cl_channel_type;
typedef cl_bitfield         cl_mem_flags;
typedef cl_uint             cl_mem_object_type;
typedef cl_uint             cl_mem_info;
typedef cl_uint             cl_image_info;
typedef cl_uint             cl_addressing_mode;
typedef cl_uint             cl_filter_mode;
typedef cl_uint             cl_sampler_info;
typedef cl_bitfield         cl_map_flags;
typedef cl_uint             cl_program_info;
typedef cl_uint             cl_program_build_info;
typedef cl_int              cl_build_status;
typedef cl_uint             cl_kernel_info;
typedef cl_uint             cl_kernel_work_group_info;
typedef cl_uint             cl_event_info;
typedef cl_uint             cl_command_type;
typedef cl_uint             cl_profiling_info;


#ifdef __cplusplus
extern "C" {
#endif

#define CL_DEVICE_COMPILER_NOT_AVAILABLE		 -3

typedef cl_uint cl_gl_context_info;

extern CL_API_ENTRY cl_mem CL_API_CALL
clCreateFromGLBuffer(cl_context     /* context */,
                     cl_mem_flags   /* flags */,
                     GLuint         /* bufobj */,
                     int *          /* errcode_ret */) CL_API_SUFFIX__VERSION_1_0;

extern CL_API_ENTRY cl_mem CL_API_CALL
clCreateFromGLTexture2D(cl_context      /* context */,
                        cl_mem_flags    /* flags */,
                        GLenum          /* target */,
                        GLint           /* miplevel */,
                        GLuint          /* texture */,
                        cl_int *        /* errcode_ret */) CL_API_SUFFIX__VERSION_1_0;

extern CL_API_ENTRY cl_mem CL_API_CALL
clCreateFromGLTexture3D(cl_context      /* context */,
                        cl_mem_flags    /* flags */,
                        GLenum          /* target */,
                        GLint           /* miplevel */,
                        GLuint          /* texture */,
                        cl_int *        /* errcode_ret */) CL_API_SUFFIX__VERSION_1_0;

extern CL_API_ENTRY cl_mem CL_API_CALL
clCreateFromGLRenderbuffer(cl_context   /* context */,
                           cl_mem_flags /* flags */,
                           GLuint       /* renderbuffer */,
                           cl_int *     /* errcode_ret */) CL_API_SUFFIX__VERSION_1_0;

extern CL_API_ENTRY cl_int CL_API_CALL
clGetGLObjectInfo(cl_mem                /* memobj */,
                  cl_gl_object_type *   /* gl_object_type */,
                  GLuint *              /* gl_object_name */) CL_API_SUFFIX__VERSION_1_0;
                  
extern CL_API_ENTRY cl_int CL_API_CALL
clGetGLTextureInfo(cl_mem               /* memobj */,
                   cl_gl_texture_info   /* param_name */,
                   size_t               /* param_value_size */,
                   void *               /* param_value */,
                   size_t *             /* param_value_size_ret */) CL_API_SUFFIX__VERSION_1_0;

extern CL_API_ENTRY cl_int CL_API_CALL
clEnqueueAcquireGLObjects(cl_command_queue      /* command_queue */,
                          cl_uint               /* num_objects */,
                          const cl_mem *        /* mem_objects */,
                          cl_uint               /* num_events_in_wait_list */,
                          const cl_event *      /* event_wait_list */,
                          cl_event *            /* event */) CL_API_SUFFIX__VERSION_1_0;

extern CL_API_ENTRY cl_int CL_API_CALL
clEnqueueReleaseGLObjects(cl_command_queue      /* command_queue */,
                          cl_uint               /* num_objects */,
                          const cl_mem *        /* mem_objects */,
                          cl_uint               /* num_events_in_wait_list */,
                          const cl_event *      /* event_wait_list */,
                          cl_event *            /* event */) CL_API_SUFFIX__VERSION_1_0;


#define CL_INVALID_GL_SHAREGROUP_REFERENCE_KHR	-1000
#define CL_CURRENT_DEVICE_FOR_GL_CONTEXT_KHR	0x2006
#define CL_DEVICES_FOR_GL_CONTEXT_KHR		0x2007
#define CL_GL_CONTEXT_KHR			0x2008
#define CL_EGL_DISPLAY_KHR			0x2009
#define CL_GLX_DISPLAY_KHR			0x200A
#define CL_WGL_HDC_KHR				0x200B
#define CL_CGL_SHAREGROUP_KHR			0x200C

cl_int clGetGLContextInfoKHR(const cl_context_properties *properties,
				 cl_gl_context_info param_name,
				 size_t param_value_size,
				 void *param_value,
				 size_t *param_value_size_ret);


cl_int	clGetGLContextInfoAPPLE( cl_context /* context */,
								  void * /* platform_gl_ctx */,
								  cl_gl_platform_info /* param_name */,
								  size_t /* param_value_size */,
								  void * /* param_value */,
								  size_t * /* param_value_size_ret */)  AVAILABLE_MAC_OS_X_VERSION_10_6_AND_LATER;  


#define CL_CONTEXT_PROPERTY_USE_CGL_SHAREGROUP_APPLE        0x10000000 /* Introduced in Mac OS X 10.6 */

#define CL_CGL_DEVICE_FOR_CURRENT_VIRTUAL_SCREEN_APPLE 		0x10000002 /* Introduced in Mac OS X 10.6 */
#define CL_CGL_DEVICES_FOR_SUPPORTED_VIRTUAL_SCREENS_APPLE	0x10000003 /* Introduced in Mac OS X 10.6 */
#define CL_INVALID_GL_CONTEXT_APPLE                         -1000      /* Introduced in Mac OS X 10.6 */


#ifdef __cplusplus
}
#endif

#endif
