;//////////////////////////////////////////////////////////////////////////////
;/// 
;/// Copyright (c) 2009-2010 Olivier Chafik
;/// 
;//////////////////////////////////////////////////////////////////////////////

.386
.MODEL FLAT
.CODE

CTX_phandler      =  16

_dcRawCallAdapterSkipTwoArgs32_cdecl PROC ; EXPORT

    OPTION PROLOGUE:NONE, EPILOGUE:NONE

	add  esp, 8                    	; remove JNIEnv *env and jobject *this
    lea  esp, dword ptr[esp - 8]     ; copy return address
    call dword ptr[eax + CTX_phandler] ; call function
	sub  esp, 8
	lea  esp, dword ptr[esp + 8]		; copy return address
    
    ret
    
_dcRawCallAdapterSkipTwoArgs32_cdecl ENDP

END