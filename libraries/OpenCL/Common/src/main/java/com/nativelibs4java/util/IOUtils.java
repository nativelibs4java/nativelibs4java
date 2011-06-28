/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.util;

import java.io.*;
import java.net.*;

/**
 *
 * @author Olivier
 */
public class IOUtils {
    public static String readText(File f) throws IOException {
        Reader in = new FileReader(f);
        try {
            return readText(in);
        } finally {
            in.close();
        }
    }
    public static String readText(InputStream in) throws IOException {
        return readText(new InputStreamReader(in));
    }
    public static String readText(URL url) throws IOException {
        return readTextClose(url.openStream());
    }
    public static String readTextClose(InputStream in) throws IOException {
        return readTextClose(new InputStreamReader(in));
    }
    public static String readTextClose(Reader in) throws IOException {
        try {
            return readText(in);
        } finally {
            in.close();
        }
    }
    public static String readText(Reader in) throws IOException {
        StringBuffer b = new StringBuffer();
        BufferedReader bin = new BufferedReader(in);
        String line;
        while ((line = bin.readLine()) != null) {
            b.append(line);
            b.append('\n');
        }
        return b.toString();
    }
    public static void readWrite(InputStream in, OutputStream out) throws IOException {
    		byte[] buf = new byte[1024];
    		int len;
    		while ((len = in.read(buf)) > 0)
    			out.write(buf, 0, len);
    }
}
