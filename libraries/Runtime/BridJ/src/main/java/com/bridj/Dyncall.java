/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj;

/**
 *
 * @author Olivier
 */
public class Dyncall {
    public enum ValueType {
        eVoidValue,
        eWCharValue,
        eCLongValue,
        eSizeTValue,
        eIntValue,
        eShortValue,
        eByteValue,
    	eBooleanValue,
        eLongValue,
        eDoubleValue,
        eFloatValue,
        ePointerValue,
        eEllipsis,
        eIntFlagSet,
        eNativeObjectValue
    }
    
    public enum CallbackType {
    	eJavaCallbackToNativeFunction,
    	eNativeToJavaCallback,
    	eJavaToNativeFunction,
    	eJavaToVirtualMethod
    }    
    
    public static class CallingConvention {
    	public static final int
	    	DC_CALL_C_DEFAULT            =   0,
	    	DC_CALL_C_X86_CDECL          =   1,
	    	DC_CALL_C_X86_WIN32_STD      =   2,
	    	DC_CALL_C_X86_WIN32_FAST_MS  =   3,
	    	DC_CALL_C_X86_WIN32_FAST_GNU =   4,
	    	DC_CALL_C_X86_WIN32_THIS_MS  =   5,
	    	DC_CALL_C_X86_WIN32_THIS_GNU =   6,
	    	DC_CALL_C_X64_WIN64          =   7,
	    	DC_CALL_C_X64_SYSV           =   8,
	    	DC_CALL_C_PPC32_DARWIN       =   9,
	    	DC_CALL_C_PPC32_OSX          =   9, /* alias for DC_CALL_C_PPC32_DARWIN */
	    	DC_CALL_C_ARM_ARM_EABI       =  10,
	    	DC_CALL_C_ARM_THUMB_EABI     =  11,
	    	DC_CALL_C_MIPS32_EABI        =  12,
	    	DC_CALL_C_MIPS32_PSPSDK      =  12, /* alias for DC_CALL_C_MIPS32_EABI */
	    	DC_CALL_C_PPC32_SYSV         =  13,
	    	DC_CALL_C_PPC32_LINUX        =  13, /* alias for DC_CALL_C_PPC32_SYSV */
	    	DC_CALL_C_ARM_ARM            =  14,
	    	DC_CALL_C_ARM_THUMB          =  15;
    }
    public static class SignatureChars {
	    public static final char
	        DC_SIGCHAR_VOID         = 'v',
	        DC_SIGCHAR_BOOL         = 'B',
	        DC_SIGCHAR_CHAR         = 'c',
	        DC_SIGCHAR_UCHAR        = 'C',
	        DC_SIGCHAR_SHORT        = 's',
	        DC_SIGCHAR_USHORT       = 'S',
	        DC_SIGCHAR_INT          = 'i',
	        DC_SIGCHAR_UINT         = 'I',
	        DC_SIGCHAR_LONG         = 'j',
	        DC_SIGCHAR_ULONG        = 'J',
	        DC_SIGCHAR_LONGLONG     = 'l',
	        DC_SIGCHAR_ULONGLONG    = 'L',
	        DC_SIGCHAR_FLOAT        = 'f',
	        DC_SIGCHAR_DOUBLE       = 'd',
	        DC_SIGCHAR_POINTER      = 'p',
	        DC_SIGCHAR_STRING       = 'Z',
	        DC_SIGCHAR_ENDARG       = ')',
	
	        DC_SIGCHAR_CC_PREFIX        = '_',
	        DC_SIGCHAR_CC_STDCALL       = 's',
	        DC_SIGCHAR_CC_FASTCALL_GNU  = 'f',
	        DC_SIGCHAR_CC_FASTCALL_MS   = 'F';
    }
    public static class Options {
        boolean bIsWideChar;
        boolean bIsConst;
        boolean bIsPointer;
        boolean bIsByValue;
        boolean bIsSizeT;
        boolean bIsCLong;
        int virtualIndex;
    }
}
