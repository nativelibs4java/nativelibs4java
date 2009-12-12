/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.demos;

import com.nativelibs4java.opencl.JavaCL;
import com.ochafik.util.SystemUtils;
import com.sun.jna.Platform;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import javax.swing.JOptionPane;

/**
 *
 * @author ochafik
 */
public class SetupUtils {

    public static void failWithDownloadProposalsIfOpenCLNotAvailable() {
        ///*
        try {
           JavaCL.listPlatforms();
           return;
        } catch (UnsatisfiedLinkError ex) {
            ex.printStackTrace();
        } //*/
        String title = "JavaCL Error: OpenCL library not found";
        if (Platform.isMac()) {
            JOptionPane.showMessageDialog(null, "Please upgrade Mac OS X to Snow Leopard (10.6) to be able to use OpenCL.", title, JOptionPane.ERROR_MESSAGE);
            return;
        }
        Object[] options = new Object[] {
            "NVIDIA graphic card",
            "ATI graphic card",
            "CPU only",
            "Cancel"
        };
        //for (;;) {
        int option = JOptionPane.showOptionDialog(null,
                "You don't appear to have an OpenCL implementation properly configured.\n" +
                "Please choose one of the following options to proceed to the download of an appropriate OpenCL implementation :", title, JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[2]);
        if (option >= 0 && option != 3) {
            String urlString;
            if (option == 0) {
                String nvidiaVersion = "195.62";
                boolean appendPlatform = true;
                String sys;

                if (Platform.isWindows()) {
                    if (System.getProperty("os.name").toLowerCase().contains("xp")) {
                        sys = "winxp";
                        appendPlatform = false;
                    } else {
                        sys = "win7_vista";
                    }
                    urlString = "http://www.nvidia.fr/object/" + sys + "_" + nvidiaVersion + (appendPlatform ? "_" + (Platform.is64Bit() ? "64" : "32") + "bit" : "") + "_whql.html";
                } else
                    urlString = "http://developer.nvidia.com/object/opencl-download.html";
            } else
                urlString = "http://developer.amd.com/GPU/ATISTREAMSDKBETAPROGRAM/Pages/default.aspx";

            try {
                SystemUtils.runSystemOpenURL(new URL(urlString));
            } catch (Exception ex1) {
                exception(ex1);
            }
        }
        System.exit(1);
    }

    public static void exception(Throwable ex) {
        StringWriter sout = new StringWriter();
        ex.printStackTrace(new PrintWriter(sout));
        JOptionPane.showMessageDialog(null, sout.toString(), "[Error] " + ParticlesDemo.class.getSimpleName() + " JavaCL Demo", JOptionPane.ERROR_MESSAGE);
    }
    
}
