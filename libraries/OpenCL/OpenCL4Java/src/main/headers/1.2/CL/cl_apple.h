/* Context GL sharing
 *
 * Please check for the "cl_APPLE_gl_sharing" extension using clGetDeviceInfo(CL_DEVICE_EXTENSIONS)
 * before using these extensions.

 * Apple extension for creating a CL context from a CGL share group
 *
 * This enumerated value can be specified as part of the <properties> argument passed to clCreateContext 
 * to allow OpenCL compliant devices in an existing CGL share group to be used as the devices in 
 * the newly created CL context. GL objects that were allocated in the given CGL share group can 
 * now be shared between CL and GL.
 *
 * If the <num_devices> and <devices> argument values to clCreateContext are 0 and NULL respectively,
 * all CL compliant devices in the CGL share group will be used to create the context.
 * Additional CL devices can also be specified using the <num_devices> and <devices> arguments.
 * These, however, cannot be GPU devices. On Mac OS X, you can add the CPU to the list of CL devices
 * (in addition to the CL compliant devices in the CGL share group) used to create the CL context. 
 * Note that if a CPU device is specified, the CGL share group must also include the GL float renderer; 
 * Otherwise CL_INVALID_DEVICE will be returned.
 *
 * NOTE:  Make sure that appropriate cl_gl.h header file is included separately
 */
#define CL_CONTEXT_PROPERTY_USE_CGL_SHAREGROUP_APPLE        0x10000000 /* Introduced in Mac OS /* Error code returned by clGetGLContextInfoAPPLE if an invalid platform_gl_ctx is provided           */
#define CL_INVALID_GL_CONTEXT_APPLE                         -1000      /* Introduced in Mac OS 
