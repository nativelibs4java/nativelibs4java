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
 * For instance, look at the following binding of the C-library <a href="http://www.cplusplus.com/reference/clibrary/cstdlib/strtoul/">strtoul</a> function :
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
     * Native error description (as returned by <a href="http://www.cplusplus.com/reference/clibrary/cstring/strerror/">strerror</a> or <a href="http://msdn.microsoft.com/en-us/library/ms680582(v=vs.85).aspx">FormatMessage</a>.
     */
    public String getDescription() {
        return description;
    }
    
    static void throwNewInstance(int code, String description) {
        if (code == 0)
            return;
        
        throw new LastError(code, description);
    }
}
