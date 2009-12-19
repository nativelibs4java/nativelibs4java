
#include "RawNativeForwardCallback.h"
#include "dyncallback/dyncall_callback_x64.h"
#include "dyncallback/dyncall_args_x64.h"

#include "dyncallback/dyncall_alloc_wx.h"
#include "dyncall/dyncall_signature.h"

extern "C" {
extern void dcRawCallAdapterSkipTwoArgs64();
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

	dcThunkInit(&pcb->thunk, dcRawCallAdapterSkipTwoArgs64);
	pcb->handler = handler;
	return pcb;
#else
	return NULL;
#endif
}