;//////////////////////////////////////////////////////////////////////////////
;
; Copyright (c) 2007-2009 Daniel Adler <dadler@uni-goettingen.de>, 
;                         Tassilo Philipp <tphilipp@potion-studios.com>
;
; Permission to use, copy, modify, and distribute this software for any
; purpose with or without fee is hereby granted, provided that the above
; copyright notice and this permission notice appear in all copies.
;
; THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
; WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
; MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
; ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
; WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
; ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
; OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
;
;//////////////////////////////////////////////////////////////////////////////

;///////////////////////////////////////////////////////////////////////
;
; Package: dyncall
; Library: dyncallback
; File: dyncallback/dyncall_callback_x86_msvc.asm
; Description: Callback Thunk - MASM implementation for x86
;
;///////////////////////////////////////////////////////////////////////


.386
.MODEL FLAT
.CODE


DCThunk_size      =  16
DCArgs_size       =  20
DCValue_size      =   8

CTX_thunk         =   0
CTX_phandler      =  16
CTX_pargsvt       =  20
CTX_stack_cleanup =  24
CTX_userdata      =  28

frame_arg0        =   8
frame_ret         =   4
frame_parent      =   0
frame_CTX         =  -4
frame_DCArgs      = -24
frame_DCValue     = -32

dcRawCallAdapterSkipTwoArgs32 PROC EXPORT

    OPTION PROLOGUE:NONE, EPILOGUE:NONE

    add  esp, 8                    ; remove JNIEnv *env and jobject *this
    call dword ptr[eax+CTX_phandler] ; call function

	sub  esp, 8
	
    ret

dcRawCallAdapterSkipTwoArgs32 ENDP

END