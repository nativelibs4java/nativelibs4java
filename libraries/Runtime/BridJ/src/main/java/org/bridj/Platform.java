package org.bridj;

import java.io.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.Buffer;
import static org.bridj.Dyncall.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.bridj.JNI.*;

public class Platform {
    static final String osName = System.getProperty("os.name", "");

    public static final int POINTER_SIZE, WCHAR_T_SIZE, SIZE_T_SIZE, CLONG_SIZE;
    interface FunInt {
        int apply();
    }
    static int tryInt(FunInt f, int defaultValue) {
        try {
	        return f.apply();
        } catch (Throwable th) {
            return defaultValue;
        }
    }
    static {
        int p = -1, c = -1, s = -1, l = -1;
        try {
            p = sizeOf_ptrdiff_t();
	        c = sizeOf_wchar_t();
	        l = sizeOf_long();
	        s = sizeOf_size_t();
        } catch (Throwable th) {}
        POINTER_SIZE = p;
        WCHAR_T_SIZE = c;
        SIZE_T_SIZE = s;
        CLONG_SIZE = l;
    }
    public static boolean isLinux() {
    	return isUnix() && osName.toLowerCase().contains("linux");
    }
    public static boolean isMacOSX() {
    	return isUnix() && (osName.startsWith("Mac") || osName.startsWith("Darwin"));
    }
    public static boolean isSolaris() {
    	return isUnix() && (osName.startsWith("SunOS") || osName.startsWith("Solaris"));
    }
    public static boolean isBSD() {
    	return isUnix() && (osName.contains("BSD") || isMacOSX());
    }
    public static boolean isUnix() {
    	return File.separatorChar == '/';
    }
    public static boolean isWindows() {
    	return File.separatorChar == '\\';
    }

    public static boolean isWindows7() {
    	return osName.equals("Windows 7");
    }
    public static boolean is64Bits() {
    	String arch = System.getProperty("sun.arch.data.model");
        if (arch == null)
            arch = System.getProperty("os.arch");
        return
    		arch.contains("64") ||
    		arch.equalsIgnoreCase("sparcv9");
    }

    static String getEmbeddedLibraryResource(String name) {
    	if (isWindows())
    		return (is64Bits() ? "win64/" : "win32/") + name + ".dll";
    	if (isMacOSX())
    		return "darwin_universal/lib" + name + ".dylib";
    	if (isLinux())
    		return (is64Bits() ? "linux_x64/" : "linux_x86/") + name + ".so";

    	throw new RuntimeException("Platform not supported ! (os.name='" + osName + "', os.arch='" + System.getProperty("os.arch") + "')");
    }
    public static File extractEmbeddedLibraryResource(String name) throws IOException {
    	String libraryResource = getEmbeddedLibraryResource(name);
        int i = libraryResource.lastIndexOf('.');
        String ext = i < 0 ? "" : libraryResource.substring(i);
        int len;
        byte[] b = new byte[8196];
        InputStream in = JNI.class.getClassLoader().getResourceAsStream(libraryResource);
        if (in == null) {
        	File f = new File(libraryResource);
        	if (!f.exists())
        		f = new File(f.getName());
        	if (f.exists())
        		return f.getCanonicalFile();
        //f = BridJ.getNativeLibraryFile(name);
        //    if (f.exists())
        //        return f.getCanonicalFile();
        	throw new FileNotFoundException(libraryResource);
        }
        File libFile = File.createTempFile(new File(libraryResource).getName(), ext);
        libFile.deleteOnExit();
        OutputStream out = new BufferedOutputStream(new FileOutputStream(libFile));
        while ((len = in.read(b)) > 0)
        	out.write(b, 0, len);
        out.close();
        in.close();

        return libFile;
    }
}
