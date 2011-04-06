package org.bridj;

import java.io.*;
import java.net.URL;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.Buffer;
import static org.bridj.Dyncall.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Collections;
import java.util.Collection;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import static org.bridj.JNI.*;

/**
 * Information about the execution platform (OS, architecture, native sizes...) and platform-specific actions.
 * <ul>
 * <li>To know if the JVM platform is 32 bits or 64 bits, use {@link Platform#is64Bits()}
 * </li><li>To know if the OS is an Unix-like system, use {@link Platform#isUnix()}
 * </li><li>To open files and URLs in a platform-specific way, use {@link Platform#open(File)}, {@link Platform#open(URL)}, {@link Platform#show(File)}
 * </li></ul>
 * @author ochafik
 */
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
	private static volatile String arch;
	private static synchronized String getArch() {
		if (arch == null) {
			arch = System.getProperty("os.arch");
			if (arch == null)
				arch = System.getProperty("sun.arch.data.model");
		}
		return arch;
	}
	public static boolean isSparc() {
    	String arch = getArch();
		return 
			"sparc".equals(arch) ||
			"sparcv9".equals(arch);
	}
    public static boolean is64Bits() {
    	String arch = getArch();
        return
    		arch.contains("64") ||
    		arch.equalsIgnoreCase("sparcv9");
    }
    public static boolean isAmd64Arch() {
    		String arch = getArch();
        return arch.equals("x86_64");
    }

    static Collection<String> getEmbeddedLibraryResource(String name) {
    	if (isWindows())
    		return Collections.singletonList((is64Bits() ? "win64/" : "win32/") + name + ".dll");
    	if (isMacOSX()) {
    		String generic = "darwin_universal/lib" + name + ".dylib";
    		if (isAmd64Arch())
    			return Arrays.asList("darwin_x64/lib" + name + ".dylib", generic);
    		else
    			return Collections.singletonList(generic);
    }
    	if (isLinux())
    		return Collections.singletonList((is64Bits() ? "linux_x64/" : "linux_x86/") + name + ".so");
    	if (isSolaris()) {
    		if (isSparc()) {	
    			return Collections.singletonList((is64Bits() ? "sunos_sparc64/" : "sunos_sparc/") + name + ".so");
    		} else {
    			return Collections.singletonList((is64Bits() ? "sunos_x64/" : "sunos_x86/") + name + ".so");
    		}	
		}
    	throw new RuntimeException("Platform not supported ! (os.name='" + osName + "', os.arch='" + System.getProperty("os.arch") + "')");
    }
    static File extractEmbeddedLibraryResource(String name) throws IOException {
    		String firstLibraryResource = null;
		for (String libraryResource : getEmbeddedLibraryResource(name)) {
			if (firstLibraryResource == null)
				firstLibraryResource = libraryResource;
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
				continue;
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
		throw new FileNotFoundException(firstLibraryResource);
    }
    
    /**
     * Opens an URL with the default system action.
     * @param url url to open
     * @throws NoSuchMethodException if opening an URL on the current platform is not supported
     */
	public static final void open(URL url) throws NoSuchMethodException {
        if (url.getProtocol().equals("file")) {
            open(new File(url.getFile()));
        } else {
            if (Platform.isMacOSX()) {
                execArgs("open", url.toString());
            } else if (Platform.isWindows()) {
                execArgs("rundll32", "url.dll,FileProtocolHandler", url.toString());
            } else if (Platform.isUnix() && hasUnixCommand("gnome-open")) {
                execArgs("gnome-open", url.toString());
            } else if (Platform.isUnix() && hasUnixCommand("konqueror")) {
                execArgs("konqueror", url.toString());
            } else if (Platform.isUnix() && hasUnixCommand("mozilla")) {
                execArgs("mozilla", url.toString());
            } else {
                throw new NoSuchMethodException("Cannot open urls on this platform");
		}
	}
    }

    /**
     * Opens a file with the default system action.
     * @param file file to open
     * @throws NoSuchMethodException if opening a file on the current platform is not supported
     */
	public static final void open(File file) throws NoSuchMethodException {
        if (Platform.isMacOSX()) {
			execArgs("open", file.getAbsolutePath());
        } else if (Platform.isWindows()) {
            if (file.isDirectory()) {
                execArgs("explorer", file.getAbsolutePath());
            } else {
                execArgs("start", file.getAbsolutePath());
        }
        }
        if (Platform.isUnix() && hasUnixCommand("gnome-open")) {
            execArgs("gnome-open", file.toString());
        } else if (Platform.isUnix() && hasUnixCommand("konqueror")) {
            execArgs("konqueror", file.toString());
        } else if (Platform.isSolaris() && file.isDirectory()) {
            execArgs("/usr/dt/bin/dtfile", "-folder", file.getAbsolutePath());
        } else {
            throw new NoSuchMethodException("Cannot open files on this platform");
	}
    }

    /**
     * Show a file in its parent directory, if possible selecting the file (not possible on all platforms).
     * @param file file to show in the system's default file navigator
     * @throws NoSuchMethodException if showing a file on the current platform is not supported
     */
	public static final void show(File file) throws NoSuchMethodException, IOException {
        if (Platform.isWindows()) {
			exec("explorer /e,/select,\"" + file.getCanonicalPath() + "\"");
        } else {
            open(file.getAbsoluteFile().getParentFile());
    }
    }

    static final void execArgs(String... cmd) throws NoSuchMethodException {
		try {
			Runtime.getRuntime().exec(cmd);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new NoSuchMethodException(ex.toString());
		}
	}

	static final void exec(String cmd) throws NoSuchMethodException {
		try {
			Runtime.getRuntime().exec(cmd).waitFor();
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new NoSuchMethodException(ex.toString());
		}
	}

    static final boolean hasUnixCommand(String name) {
		try {
            Process p = Runtime.getRuntime().exec(new String[]{"which", name});
			return p.waitFor() == 0;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}
}
