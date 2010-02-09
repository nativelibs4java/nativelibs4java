
package com.jdyncall;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jdyncall.ann.Library;
import com.jdyncall.ann.Mangling;
import com.jdyncall.ann.NoInheritance;

public class DynCall {
    static Map<String, Long> libHandles = new HashMap<String, Long>();
    public static synchronized long getSymbolAddress(AnnotatedElement member) throws FileNotFoundException {
        String lib = getLibrary(member);
        long libHandle = getLibHandle(lib);
        if (libHandle == 0)
            return 0;
        return getSymbolAddress(libHandle, member);
    }
    public static synchronized long getSymbolAddress(long libHandle, AnnotatedElement member) throws FileNotFoundException {
        //libHandle = libHandle & 0xffffffffL;
        Mangling mg = getAnnotation(Mangling.class, member);
        if (mg != null)
            for (String name : mg.value())
            {
                long handle = JNI.findSymbolInLibrary(libHandle, name);
                if (handle != 0)
                    return handle;
            }

        String name = null;
        if (member instanceof Member)
            name = ((Member)member).getName();
        else if (member instanceof Class<?>)
            name = ((Class<?>)member).getSimpleName();

        if (name != null) {
            long handle = JNI.findSymbolInLibrary(libHandle, name);
            if (handle == 0)
                handle = JNI.findSymbolInLibrary(libHandle, "_" + name);

            if (handle != 0)
                return handle;
        }
        return 0;
    }
    static List<String> paths;
    static synchronized List<String> getPaths() {
        if (paths == null) {
            paths = new ArrayList<String>();
            paths.add(".");
			String env;
			env = System.getenv("LD_LIBRARY_PATH");
            if (env != null)
                paths.addAll(Arrays.asList(env.split(File.pathSeparator)));
            env = System.getenv("DYLD_LIBRARY_PATH");
            if (env != null)
                paths.addAll(Arrays.asList(env.split(File.pathSeparator)));
            env = System.getenv("PATH");
            if (env != null)
                paths.addAll(Arrays.asList(env.split(File.pathSeparator)));
            env = System.getProperty("java.library.path");
            if (env != null)
                paths.addAll(Arrays.asList(env.split(File.pathSeparator)));
        }
        return paths;
    }

    public static synchronized File getLibFile(Class<?> member) throws FileNotFoundException {
        return getLibFile(getLibrary(member));
    }

    public static File getLibFile(String name) {
        if (name == null)
            return null;
        for (String path : getPaths()) {
            File pathFile;
            try {
                pathFile = new File(path).getCanonicalFile();
            } catch (IOException ex) {
                Logger.getLogger(DynCall.class.getName()).log(Level.SEVERE, null, ex);
                continue;
            }
            File f = new File(pathFile, name + ".dll").getAbsoluteFile();
            if (!f.exists())
                f = new File(pathFile, "lib" + name + ".so").getAbsoluteFile();
            if (!f.exists())
                f = new File(pathFile, "lib" + name + ".dylib").getAbsoluteFile();
            if (!f.exists())
                f = new File(pathFile, "lib" + name + ".jnilib").getAbsoluteFile();
            if (!f.exists())
                continue;

            try {
                return f.getCanonicalFile();
            } catch (IOException ex) {
                Logger.getLogger(DynCall.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
        	return JNI.extractEmbeddedLibraryResource(name);
        } catch (IOException ex) {
        	return null;
        }
    }
    public static synchronized long getLibHandle(String name) throws FileNotFoundException {
        if (name == null)
            return 0;
        
        Long l = libHandles.get(name);
        if (l != null)
            return l;

        File f = getLibFile(name);
        long ll = f == null ? 0 : JNI.loadLibrary(f.toString());
        if (ll == 0)
            throw new FileNotFoundException("Library '" + name + "' was not found in path '" + getPaths() + "'");
        libHandles.put(name, ll);
        return ll;
    }
    public static String getLibrary(AnnotatedElement m) {
        Library lib = getAnnotation(Library.class, m);
        return lib == null ? null : lib.value(); // TODO use package as last resort
    }
    protected static <A extends Annotation> A getAnnotation(Class<A> ac, AnnotatedElement m, Annotation... directAnnotations) {
        if (directAnnotations != null)
            for (Annotation ann : directAnnotations)
                if (ac.isInstance(ann))
                    return ac.cast(ann);
            
        if (m == null)
            return null;
        A a = m.getAnnotation(ac);
        if (a != null)
            return a;

        if (ac.getAnnotation(NoInheritance.class) != null)
            return null;

        if (m instanceof Member)
            return getAnnotation(ac, ((Member)m).getDeclaringClass());

        if (m instanceof Class<?>) {
            Class<?> c = (Class<?>)m, dc = c.getDeclaringClass();
            if (dc != null)
                return getAnnotation(ac, dc);
        }
        return null;
    }
}
