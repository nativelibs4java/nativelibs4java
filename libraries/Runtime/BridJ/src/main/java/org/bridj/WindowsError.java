package org.bridj;
import static org.bridj.WinExceptionsConstants.*;

/**
 * Native Windows error as caught by a <code>__try { ... } __except (...) { ... }</code> block.
 * Not public yet.
 * @author Olivier
 */
class WindowsError extends NativeError {
    final int code;
    final long info, address;
    WindowsError(int code, long info, long address) {
        super(computeMessage(code, info, address));
        this.code = code;
        this.info = info;
        this.address = address;
    }
    public static void throwNew(int code, long info, long address) {
        throw new WindowsError(code, info, address);
    }
    static String subMessage(long info) {
        switch ((int)info) {
            case 0: return "Attempted to read from an inaccessible address";
            case 1: return "Attempted to write to an inaccessible address";
            case 8: return "Attempted to execute memory that's not executable  (DEP violation)";
            default: return "?";
        }
    }
    public static String computeMessage(int code, long info, long address) {
        switch (code) {
            case EXCEPTION_ACCESS_VIOLATION:
                return "Access violation : " + toHex(address) + " (" + subMessage(info) + ")";
            case EXCEPTION_IN_PAGE_ERROR:
                return "In page error : " + toHex(address) + " (" + subMessage(info) + ")";
            case EXCEPTION_FLT_DIVIDE_BY_ZERO:
                return "Divided by zero";
            case EXCEPTION_PRIV_INSTRUCTION:
                return "Privileged instruction : attempted to executed an instruction with an operation that is not allowed in the current computer mode";
        }
        return "Windows native error (code = " + code + ", info = " + info + ", address = " + address + ") !";
    }
}
