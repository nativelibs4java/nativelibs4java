#ifndef __JAVACL_PROXY_API_H
#define __JAVACL_PROXY_API_H

#ifdef __APPLE__
#include <OpenCL/cl.h>
#else
#include <CL/cl.h>
#endif	

#include "Common.h"

extern jclass gOpenCLProxyInterface;
extern jmethodID gclGetPlatformIDsMethod;
extern jobject gOpenCLProxyImplementation;

void bindJavaAPI(void *instanceData);
void unbindJavaAPI();

#endif // __JAVACL_PROXY_API_H
