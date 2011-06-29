package org.bridj;

import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bridj.ann.*;

//import static org.bridj.LastError.Windows.*;
//import static org.bridj.LastError.Unix.*;
import static org.bridj.Pointer.*;

/**
 * Native error that correspond to the <a href="http://en.wikipedia.org/wiki/Errno.h">errno</a> or <a href="http://msdn.microsoft.com/en-us/library/ms679360(v=vs.85).aspx">GetLastError()</a> mechanism.<br>
 * To have C function bindings throw this error whenever an error is marked after the function is called, simply make it throw this error explicitly.<br>
 * For instance, look at the following binding of the C-library strtoul function :
 * <pre>{@code
 * @Library("c")
 * public static native long strtoul(Pointer<Byte> str, Pointer<Pointer<Byte>> endptr, int base) throws LastError;
 * }</pre> 
 * @author Olivier Chafik
 */
public class LastError extends Error {
    final int code;
    final String description;
    
    LastError(int code, String description) {
    		super((description == null ? "?" : description) + " (error code = " + code + ")");//toString(code));
    		this.code = code;
            this.description = description;
            if (BridJ.verbose)
                BridJ.log(Level.INFO, "Last error detected : " + getMessage());
    }

    /**
     * Native error code
     */
    public int getCode() {
        return code;
    }

    /**
     * Native error description
     */
    public String getDescription() {
        return description;
    }
    
    static void throwNewInstance(int code, String description) {
        if (code == 0)
            return;
        
        throw new LastError(code, description);
    }
    
    /*
    /// Unix bindings useful for last error handling
	@Library("c")
    @Optional
    public static class Unix {
        static {
            BridJ.register();
        }
		public static native Pointer<Byte> strerror(int errnum);
		
		static volatile Pointer<Integer> errno;
		public static synchronized Pointer<Integer> errno() {
			if (errno == null) {
                try {
                    errno = BridJ.getNativeLibrary("c").getSymbolPointer("errno").as(int.class);
                } catch (Throwable ex) {
                    throw new RuntimeException("Failed to initialize errno : " + ex, ex);
                }
			}
			return errno;
		}
	}
    
	/// Windows bindings useful for last error handling
	@Optional
	@Library("kernel32")
	@Convention(Convention.Style.StdCall)
    public static class Windows {
		static {
            BridJ.register();
        }
		static final int 
			FORMAT_MESSAGE_FROM_SYSTEM = 0x00001000,
			FORMAT_MESSAGE_ALLOCATE_BUFFER = 0x00000100,
			FORMAT_MESSAGE_IGNORE_INSERTS = 0x00000200;
		
		/// http://msdn.microsoft.com/en-us/library/ms679360(v=vs.85).aspx
		public static native int GetLastError();
		
		/// http://msdn.microsoft.com/en-us/library/aa366730(v=vs.85).aspx
		public static native Pointer<Pointer<?>> LocalFree(Pointer<Pointer<?>> hMem);
		
		/// http://msdn.microsoft.com/en-us/library/aa366723(v=vs.85).aspx
		public static native Pointer<Pointer<?>> LocalAlloc(int flags, @Ptr long length);
		
		public static int MAKELANGID(short primaryLanguageCode, short subLanguageCode) {
			return (subLanguageCode << 10) | primaryLanguageCode;
		}
		public static final short LANG_NEUTRAL = (short)0x0000, SUBLANG_DEFAULT = (short)0x0400;
		public static native int FormatMessage(
			int dwFlags,
			Pointer<?> lpSource,
			int dwMessageId,
			int dwLanguageId,
			Pointer<?> lpBuffer,
			int nSize,
			Object... args
		);
	}
    
    static String toString(int code) {
    		return getErrorDescription(code) + " (code " + code + ")";
    }
    public static void throwLastError(int code) {
    		//int code = getLastErrorCode();
    		if (code == 0)
    			return;
    		
    		throw new LastError(code);
    	}
    public static int getLastErrorCode() {
    		if (Platform.isWindows())
    			return GetLastError();
    		else
    			return errno().get();
    }
    public static String getErrorDescription(int code) {
        if (Platform.isWindows()) {
            // http://msdn.microsoft.com/en-us/library/ms680582(v=vs.85).aspx
            int n = 1024;
            Pointer<Pointer<?>> lpBuffer = allocatePointer(); 
            FormatMessage(
                FORMAT_MESSAGE_ALLOCATE_BUFFER | 
                FORMAT_MESSAGE_FROM_SYSTEM |
                FORMAT_MESSAGE_IGNORE_INSERTS,
                null,
                code,
                MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
                lpBuffer,
                0,
                null
            );
            String msg = lpBuffer.get().getCString();
            LocalFree(lpBuffer);
            return msg;
        } else {
            return strerror(code).getCString();
        }
    }*/
}
