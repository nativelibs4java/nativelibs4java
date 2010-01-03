
#include "RawNativeForwardCallback.h"

#ifdef _WIN64
#include "dyncallback/dyncall_callback_x64.h"
#include "dyncallback/dyncall_args_x64.h"
#else
#ifdef _WIN32
#include "dyncallback/dyncall_callback_x86.h"
#include "dyncallback/dyncall_args_x86.h"
#endif
#endif

#include "dyncallback/dyncall_alloc_wx.h"
#include "dyncallback/dyncall_thunk.h"
#include "dyncall/dyncall_signature.h"

extern "C" {
extern void dcRawCallAdapterSkipTwoArgs64();
extern void dcRawCallAdapterSkipTwoArgs32_cdecl();
}

struct DCAdapterCallback
{
	DCThunk  	         thunk;    // offset 0,  size 24
	void (*handler)();
};

DCAdapterCallback* dcRawCallAdapterSkipTwoArgs(void (*handler)())
{
#ifdef _WIN64
	int err;
	DCAdapterCallback* pcb;
	err = dcAllocWX(sizeof(DCAdapterCallback), (void**) &pcb);
	if (err != 0) 
		return 0;

	dcInitThunk(&pcb->thunk, dcRawCallAdapterSkipTwoArgs64);
	pcb->handler = handler;
	return pcb;
#else
#ifdef _WIN32
	int err;
	DCAdapterCallback* pcb;
	err = dcAllocWX(sizeof(DCAdapterCallback), (void**) &pcb);
	if (err != 0) 
		return 0;

	dcInitThunk(&pcb->thunk, dcRawCallAdapterSkipTwoArgs32_cdecl);
	pcb->handler = handler;
	return pcb;
#else
	return NULL;
#endif
#endif
}