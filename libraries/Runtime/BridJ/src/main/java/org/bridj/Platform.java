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
import java.util.ArrayList;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
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
    static final ClassLoader systemClassLoader; 
    public static ClassLoader getClassLoader() {
    		return getClassLoader(BridJ.class);
    }
    public static ClassLoader getClassLoader(Class<?> cl) {
    		ClassLoader loader = cl == null ? null : cl.getClassLoader();
    		return loader == null ? systemClassLoader : loader;
    }
    
    static {
    		{
			List<URL> urls = new ArrayList<URL>();
			for (String propName : new String[] { "java.class.path", "sun.boot.class.path" }) {
				String prop = System.getProperty(propName);
				if (prop == null)
					continue;
				
				for (String path : prop.split(File.pathSeparator)) {
					path = path.trim();
					if (path.length() == 0)
						continue;
					
					URL url;
					try {
						url = new URL(path);
					} catch (MalformedURLException ex) {
						try {
							url = new File(path).toURI().toURL();
						} catch (MalformedURLException ex2) {
							url = null;
						}
					}
					if (url != null)
						urls.add(url);
				}
			}
			//System.out.println("URLs for synthetic class loader :");
			//for (URL url : urls)
			//	System.out.println("\t" + url);
			systemClassLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]));
		}
		
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
    
    /**
     * Whether to use Unicode versions of Windows APIs rather than ANSI versions (for functions that haven't been bound yet : has no effect on functions that have already been bound).<br>
     * Some Windows APIs such as SendMessage have two versions : 
     * <ul>
     * <li>one that uses single-byte character strings (SendMessageA, with 'A' for ANSI strings)</li>
     * <li>one that uses unicode character strings (SendMessageW, with 'W' for Wide strings).</li>
     * </ul>
     * <br>
     * In a C/C++ program, this behaviour is controlled by the UNICODE macro definition.<br>
     * By default, BridJ will use the Unicode versions. Set this field to false, set the bridj.useUnicodeVersionOfWindowsAPIs property to "false" or the BRIDJ_USE_UNICODE_VERSION_OF_WINDOWS_APIS environment variable to "0" to use the ANSI string version instead.
     */
    public static boolean useUnicodeVersionOfWindowsAPIs = !(
    		"false".equals(System.getProperty("bridj.useUnicodeVersionOfWindowsAPIs")) ||
    		"0".equals(System.getenv("BRIDJ_USE_UNICODE_VERSION_OF_WINDOWS_APIS"))
	);
    
	private static volatile String arch;
	private static synchronized String getArch() {
		if (arch == null) {
			arch = System.getProperty("os.arch");
			if (arch == null)
				arch = System.getProperty("sun.arch.data.model");
		}
		return arch;
	}
	
	public static boolean isAndroid() {
		return "dalvik".equalsIgnoreCase(System.getProperty("java.vm.name")) && isLinux();
	}
	public static boolean isArm() {
    		String arch = getArch();
		return "arm".equals(arch);	
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

    static final String embeddedLibraryResourceRoot = "org/bridj/lib/";
    static Collection<String> getEmbeddedLibraryResource(String name) {
    	String root = embeddedLibraryResourceRoot;
    	if (isWindows())
    		return Collections.singletonList(root + (is64Bits() ? "win64/" : "win32/") + name + ".dll");
    	if (isMacOSX()) {
    		String suff = "/lib" + name + ".dylib";
    		if (isArm()) {
    			return Collections.singletonList(root + "iphoneos_arm32_arm" + suff);
    		} else {
    			String pref = root + "darwin_";
			String univ = pref + "universal" + suff;
			if (isAmd64Arch())
				return Arrays.asList(univ, pref + "x64" + suff);
			else
				return Collections.singletonList(univ);
		}
    }
    if (isAndroid()) {
    		String fileName = "lib" + name + ".so";
    		return Arrays.asList(
    			root + "android_arm32_arm/" + fileName, // BridJ-style .so embedding
    			"lib/armeabi/" + fileName // Android SDK + NDK-style .so embedding
		);
    }
    	if (isLinux())
    		return Collections.singletonList(root + (is64Bits() ? "linux_x64/" : "linux_x86/") + name + ".so");
    	if (isSolaris()) {
    		if (isSparc()) {	
    			return Collections.singletonList(root + (is64Bits() ? "sunos_sparc64/" : "sunos_sparc/") + name + ".so");
    		} else {
    			return Collections.singletonList(root + (is64Bits() ? "sunos_x64/" : "sunos_x86/") + name + ".so");
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
			InputStream in = getClassLoader().getResourceAsStream(libraryResource);
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
