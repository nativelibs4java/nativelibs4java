/*
 * JavaCL - Java API and utilities for OpenCL
 * http://javacl.googlecode.com/
 *
 * Copyright (c) 2009-2010, Olivier Chafik (http://ochafik.free.fr/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Olivier Chafik nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY OLIVIER CHAFIK AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.nativelibs4java.opencl.demos.hardware;
import org.bridj.BridJ;
import com.nativelibs4java.opencl.*;
import com.nativelibs4java.opencl.demos.SetupUtils;
import org.bridj.Platform;

import java.awt.BorderLayout;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;

/**
 * Small program that outputs html reports about the information returned by OpenCL for each device.
 * @author ochafik
 */
public class HardwareReport {

    public static Map<String, Method> infoMethods(Class<?> c) {
        Map<String, Method> mets = new TreeMap<String, Method>();
        for (Method met : c.getMethods()) {
            InfoName name = met.getAnnotation(InfoName.class);
            if (name == null) {
                continue;
            }
            mets.put(name.value(), met);
        }
        return mets;
    }

    public static List<Map<String, Object>> listInfos(CLPlatform platform) {
        try {
            List<Map<String, Object>> ret = new ArrayList<Map<String, Object>>();
            Map<String, Method> platMets = infoMethods(CLPlatform.class);
            Map<String, Method> devMets = infoMethods(CLDevice.class);
            //for (CLPlatform platform : JavaCL.listPlatforms()) {
                Map<String, Object> platInfos = new TreeMap<String, Object>();
                for (Map.Entry<String, Method> platMet : platMets.entrySet()) {
                	try {
                		platInfos.put(platMet.getKey(), platMet.getValue().invoke(platform));
                	} catch (InvocationTargetException ex) {
                		if (ex.getCause() instanceof UnsupportedOperationException)
                			platInfos.put(platMet.getKey(), "n/a");
                		else
                			throw ex;
                	}
                }
                for (CLDevice device : platform.listAllDevices(false)) {
                    Map<String, Object> devInfos = new TreeMap<String, Object>(platInfos);
                    for (Map.Entry<String, Method> devMet : devMets.entrySet()) {
                    	try {
                    		devInfos.put(devMet.getKey(), devMet.getValue().invoke(device));
                    	} catch (InvocationTargetException ex) {
                    		if (ex.getCause() instanceof UnsupportedOperationException)
                    			devInfos.put(devMet.getKey(), "n/a");
                    		else
                    			throw ex;
                    	}
                    }
                    ret.add(devInfos);
                }
            //}
            return ret;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String toString(Object value) {
        if (value == null) {
            return "null";
        }
        Class<?> c = value.getClass();
        try {
            if (c.isArray()) {
                if (!c.getComponentType().isPrimitive()) {
                    c = Object[].class;
                }
                value = Arrays.class.getMethod("toString", c).invoke(null, value);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        String s = value.toString();
        if (s.startsWith("[") && s.endsWith("]")) {
			s = s.substring(1, s.length() - 1);
            s = s.replaceAll(", ", "<br>");
        }
        return s;
    }

    public static String toTable(List<Map<String, Object>> list) {
        StringBuilder b = new StringBuilder();

        b.append("<table border='1'>\n");
        if (!list.isEmpty()) {
            Set<String> keys = list.get(0).keySet();

            b.append("\t<tr valign=\"top\">\n");
            b.append("\t\t<td></td>");
            for (Map<String, Object> device : list) {
                Object value = device.get("CL_DEVICE_NAME");
                b.append("<td><b>[" + toString(value) + "]</b></td>\n");
            }
            b.append("\n");
            b.append("\t</tr>\n");

            for (String key : keys) {
                b.append("\t<tr valign=\"top\">\n");
                b.append("\t\t<td>" + key + "</td>");
                for (Map<String, Object> device : list) {
                    Object value = device.get(key);
                    b.append("<td>" + toString(value) + "</td>");
                }
                b.append("\n");
                b.append("\t</tr>\n");
            }
        }

        b.append("</table>\n");
        return b.toString();
    }

    public static String toHTML(List<Map<String, Object>> list) {
        return //"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n"
                + "<body>\n"
                + toTable(list)
                + "</body></html>";
    }

    public static JComponent getHardwareReportComponent(CLPlatform platform) {
        List<Map<String, Object>> list = listInfos(platform);
        final String html = toHTML(list);

        JEditorPane ed = new JEditorPane();
        ed.setContentType("text/html");
        ed.setText(html);
        ed.setEditable(false);

        JPanel ret = new JPanel(new BorderLayout());
        ret.add("Center", new JScrollPane(ed));

        final String fileName = "HardwareReport.html";
        JButton bWrite = new JButton("Save " + fileName + "...");
        bWrite.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FileDialog fd = new FileDialog((Frame)null, "Save " + fileName, FileDialog.SAVE);
                fd.setFile(fileName);
                fd.setVisible(true);
                if (fd.getFile() == null)
                    return;

                try {
                    File file = new File(new File(fd.getDirectory()), fd.getFile());
                    file.getParentFile().mkdirs();
                    Writer w = new OutputStreamWriter(new FileOutputStream(file), "utf-8");
                    w.write(html);
                    w.close();

                    Platform.show(file);
                } catch (Throwable ex) {
                    SetupUtils.exception(ex);
                }
            }

        });

        ret.add("South", bWrite);
        return ret;
    }
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch(Exception ex) {}
        SetupUtils.failWithDownloadProposalsIfOpenCLNotAvailable();

        try {
            JTabbedPane pane = new JTabbedPane();
            for (CLPlatform platform : JavaCL.listPlatforms())
                pane.addTab(platform.toString(), getHardwareReportComponent(platform));
            
            JFrame f = new JFrame("OpenCL4Java: Hardware Characteristics");
            f.getContentPane().add("Center", pane);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.pack();
            f.setSize(f.getWidth() + 15, 700);
            f.setVisible(true);
            
        } catch (Throwable ex) {
			StringWriter sout = new StringWriter();
			ex.printStackTrace(new PrintWriter(sout));
			JOptionPane.showMessageDialog(null, sout.toString(), "[Error] OpenCL4Java HardwareReport", JOptionPane.ERROR_MESSAGE);
        }
    }
}
