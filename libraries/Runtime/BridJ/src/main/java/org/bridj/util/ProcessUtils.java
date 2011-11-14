/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bridj.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.getProperty;

/**
 * Util methods to launch processes, including JVM processes
 * @author Olivier
 */
public class ProcessUtils {

    public static String[] computeJavaProcessArgs(Class<?> mainClass, List<?> mainArgs) {
        List<String> args = new ArrayList<String>();
        args.add(new File(new File(getProperty("java.home")), "bin" + File.separator + "java").toString());
        args.add("-cp");
        args.add(getProperty("java.class.path"));
        args.add(mainClass.getName());
        for (Object arg : mainArgs)
            args.add(arg.toString());
        
        return args.toArray(new String[args.size()]);
    }
    public static Process startJavaProcess(Class<?> mainClass, List<?> mainArgs) throws IOException {
        ProcessBuilder b = new ProcessBuilder();
        b.command(computeJavaProcessArgs(mainClass, mainArgs));
        b.redirectErrorStream(true);
        return b.start();
    }
    
}
