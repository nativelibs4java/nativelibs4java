/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.runtime;

/**
 *
 * @author Olivier
 */
public class JDyncallLibrary {
    public enum ValueType {
        eVoidValue, // = 0,
        eArrayValue,
        eWCharValue,
        eCallbackValue,
        eAddressableValue,
        eLongPtrValue,
        eCLongValue,
        eSizeTValue,
        eBufferValue,
        eIntValue,
        eShortValue,
        eByteValue,
        eLongValue,
        eDoubleValue,
        eFloatValue,
        eIntArrayValue,
        eShortArrayValue,
        eByteArrayValue,
        eLongArrayValue,
        eDoubleArrayValue,
        eFloatArrayValue,
        eBooleanArrayValue,
        eCharArrayValue
    }
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

    static class OptimizationHints {
        boolean isAdaptableAsRaw;
    }
    static class Options {
        boolean bIsWideChar;
        boolean bIsConst;
        boolean bIsPointer;
        boolean bIsByValue;
        boolean bIsSizeT;
        boolean bIsCLong;
        int virtualIndex;
    }
}
