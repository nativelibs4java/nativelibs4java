package org.bridj;

import static org.bridj.SignalConstants.*;
import static java.lang.Long.toHexString;
/**
 * Native error encapsulated as a Java error.
 * @author ochafik
 */
public abstract class NativeError extends Error {
    protected NativeError(String message) {
        super(message);
    }
    public static void throwSignalError(int signal, int code, long address) {
        throw new SignalError(signal, code, address);
    }

    /**
     * POSIX signal error, such as "Segmentation fault" or "Bus error".<br>
     * Not public on purpose.
     */
    static class SignalError extends NativeError {
        final int signal, code;
        final long address;

        private SignalError(int signal, int code, long address) {
            super(getFullSignalMessage(signal, code, address));
            this.signal = signal;
            this.code = code;
            this.address = address;
        }

        /**
         * POSIX signal associated with this error
         */
        public int getSignal() {
            return signal;
        }

        /**
         * POSIX signal code associated with this error
         */
        public int getCode() {
            return code;
        }

        /**
         * Memory address that caused the SIGBUS or SIGSEGV signal, or zero for other signals
         */
        public long getAddress() {
            return address;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SignalError))
                return false;

            SignalError e = (SignalError)obj;
            return signal == e.signal && code == e.code;
        }

        @Override
        public int hashCode() {
            return ((Integer)signal).hashCode() ^ ((Integer)code).hashCode() ^ ((Long)address).hashCode();
        }

        public static String getFullSignalMessage(int signal, int code, long address) {
            String simple = getSignalMessage(signal, 0, address);
            if (code == 0)
                return simple;

            String sub = getSignalMessage(signal, code, address);
            if (sub.equals(simple))
                return simple;

            return simple + " (" + sub + ")";
        }
        static String toHex(long address) {
            return "0x" + toHexString(address);
        }
        /**
         * http://pubs.opengroup.org/onlinepubs/7908799/xsh/signal.h.html
         */
        public static String getSignalMessage(int signal, int code, long address) {
            switch (signal) {
                case SIGSEGV:
                    switch (code) {
                        case SEGV_MAPERR: return "Address not mapped to object";
                        case SEGV_ACCERR: return "Invalid permission for mapped object";
                        default:
                            return "Segmentation fault : " + toHex(address);
                    }
                case SIGBUS:
                    switch (code) {
                        case BUS_ADRALN: return "Invalid address alignment";
                        case BUS_ADRERR: return "Nonexistent physical address";
                        case BUS_OBJERR: return "Object-specific HW error";
                        default:
                            return "Bus error : " + toHex(address);
                    }
                case SIGABRT:
                    return "Native exception (call to abort())";
                case SIGFPE:
                    switch (code) {
                        case FPE_INTDIV: return "Integer divide by zero";
                        case FPE_INTOVF: return "Integer overflow";
                        case FPE_FLTDIV: return "Floating point divide by zero";
                        case FPE_FLTOVF: return "Floating point overflow";
                        case FPE_FLTUND: return "Floating point underflow";
                        case FPE_FLTRES: return "Floating point inexact result";
                        case FPE_FLTINV: return "Invalid floating point operation";
                        case FPE_FLTSUB: return "Subscript out of range";
                        default:
                            return "Floating point error";
                    }
                case SIGSYS:
                    return "Bad argument to system call";
                case SIGTRAP:
                    switch (code) {
                        case TRAP_BRKPT: return "Process breakpoint";
                        case TRAP_TRACE: return "Process trace trap";
                        default:
                            return "Trace trap";
                    }
                case SIGILL:
                    switch (code) {
                        case ILL_ILLOPC: return "Illegal opcode";
                        case ILL_ILLTRP: return "Illegal trap";
                        case ILL_PRVOPC: return "Privileged opcode";
                        case ILL_ILLOPN: return "Illegal operand";
                        case ILL_ILLADR: return "Illegal addressing mode";
                        case ILL_PRVREG: return "Privileged register";
                        case ILL_COPROC: return "Coprocessor error";
                        case ILL_BADSTK: return "Internal stack error";
                        default:
                            return "Illegal instruction";
                    }
                default:
                    return "Native error";
            }
        }
    }
}
