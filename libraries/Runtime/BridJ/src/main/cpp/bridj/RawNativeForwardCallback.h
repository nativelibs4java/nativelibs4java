#ifndef RawNativeForwardCallback_H
#define RawNativeForwardCallback_H

#include "../dyncallback/dyncall_callback.h"

#ifdef __cplusplus
extern "C" {
#endif 

typedef struct DCAdapterCallback DCAdapterCallback;
DC_API DCAdapterCallback* dcRawCallAdapterSkipTwoArgs(void (*handler)());

#ifdef __cplusplus
}
#endif 

#endif // DYNCALL_CALLBACK_H

