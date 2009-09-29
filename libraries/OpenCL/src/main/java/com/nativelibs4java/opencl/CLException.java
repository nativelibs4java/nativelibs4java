package com.nativelibs4java.opencl;
import static com.nativelibs4java.opencl.library.OpenCLLibrary.*;
import com.nativelibs4java.opencl.library.OpenCLLibrary;
import com.ochafik.util.string.StringUtils;
import java.util.*;
import java.lang.reflect.*;

/**
 * OpenCL error
 * @author ochafik
 */
public class CLException extends RuntimeException {
    final int code;
    public CLException(String message, int code) {
        super(message);
        this.code = code;
    }
    public int getCode() {
        return code;
    }



    static String errorString(int err) {
        if (err == CL_SUCCESS)
            return null;

        List<String> candidates = new ArrayList<String>();
        for (Field f : OpenCLLibrary.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
                continue;
            }
            if (f.getType().equals(Integer.TYPE)) {
                try {
                    int i = (Integer) f.get(null);
                    if (i == err) {
                        candidates.add(f.getName());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return StringUtils.implode(candidates, " or ");
    }

    static void error(int err) {
        String str = errorString(err);
        if (str == null)
            return;

        throw new CLException("OpenCL Error : " + str, err);
    }
}