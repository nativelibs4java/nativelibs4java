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

    ; http://www.arl.wustl.edu/~lockwood/class/cs306/books/artofasm/toc.html
	add  esp, 12                    	; remove JNIEnv *env and jobject *this
    push dword ptr[esp - 12]     ; copy return address
    add  esp, 4                    	
    call dword ptr[eax + CTX_phandler] ; call function
	sub  esp, 12
	push dword ptr[esp + 12]		; copy return address
    add  esp, 4
	
    ret
    
_dcRawCallAdapterSkipTwoArgs32_cdecl ENDP

END