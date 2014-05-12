package com.nativelibs4java.velocity;

import com.google.common.base.Function;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    

    protected static String processComments(String source, Function<String, String> processor) {
        Pattern p = Pattern.compile("(?m)(?s)/\\*\\*?.*?\\*/");
        Matcher m = p.matcher(source);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String comment = m.group();
            String replacement = processor.apply(comment);
            m.appendReplacement(sb, "");
            sb.append(replacement);
        }
        m.appendTail(sb);
        return sb.toString();
    }
    
    protected static String quoteSharpsInComments(String source) {
        return processComments(source, new Function<String, String>() {
            public String apply(String f) {
                return f.replaceAll("(?<=\\w)#", "\\\\#");
            }
        });
    }
    
    protected static String unquoteSharpsInComments(String source) {
        return processComments(source, new Function<String, String>() {
            public String apply(String f) {
                return f.replaceAll("\\\\#", "#");
            }
        });
    }
    
    static String readTextFile(File file) throws IOException {
        BufferedReader sourceIn = new BufferedReader(new FileReader(file));
        StringWriter sourceOut = new StringWriter();
        PrintWriter sourcePOut = new PrintWriter(sourceOut);
        String line;
        while ((line = sourceIn.readLine()) != null) {
            sourcePOut.println(line);
        }
        return sourceOut.toString();
    }
    
    static void writeTextFile(File file, String text) throws IOException {
        FileWriter f = new FileWriter(file);
        f.write(unquoteSharpsInComments(text));
        f.close();
    }
}
