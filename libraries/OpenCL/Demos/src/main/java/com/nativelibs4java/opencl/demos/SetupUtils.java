/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.demos;

import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bridj.BridJ;
import org.bridj.JNI;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import com.nativelibs4java.opencl.JavaCL;
import org.bridj.Platform;

/**
 *
 * @author ochafik
 */
public class SetupUtils {

    public enum DownloadURL {
        ATI("http://developer.amd.com/tools-and-sdks/opencl-zone/amd-accelerated-parallel-processing-app-sdk"),
        NVidia("http://www.nvidia.com/Download/Find.aspx");
        
        public final URL url;
        DownloadURL(String s) {
            URL url;
            try {
                url = new URL(s);
            } catch (MalformedURLException ex) {
                Logger.getLogger(SetupUtils.class.getName()).log(Level.SEVERE, null, ex);
                url = null;
            }
            this.url = url;
        }
    }
    public static void failWithDownloadProposalsIfOpenCLNotAvailable() {
        ///*
        try {
           JavaCL.listPlatforms();
           return;
        } catch (Throwable ex) {
            ex.printStackTrace();
        } //*/
        String title = "JavaCL Error: OpenCL library not found";
        if (Platform.isMacOSX()) {
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
            DownloadURL url;
            if (option == 0) {
                /*String nvidiaVersion = "260.99";
                boolean appendPlatform = true;
                String sys;

                if (JNI.isWindows()) {
                    if (System.getProperty("os.name").toLowerCase().contains("xp")) {
                        sys = "winxp";
                        appendPlatform = false;
                    } else {
                        sys = "win7_vista";
                    }
                    urlString = "http://www.nvidia.fr/object/" + sys + "_" + nvidiaVersion + (appendPlatform ? "_" + (JNI.is64Bits() ? "64" : "32") + "bit" : "") + "_whql.html";
                } else
                    urlString = "http://developer.nvidia.com/object/opencl-download.html";
                */
                url = DownloadURL.NVidia;
            } else
                url = DownloadURL.ATI;

            try {
                Platform.open(url.url);
            } catch (Exception ex1) {
                exception(ex1);
            }
        }
        System.exit(1);
    }

    public static void exception(Throwable ex) {
        StringWriter sout = new StringWriter();
        ex.printStackTrace(new PrintWriter(sout));
        JOptionPane.showMessageDialog(null, sout.toString(), "Error in JavaCL Demo", JOptionPane.ERROR_MESSAGE);
    }

    static Border etchedBorder;
    static synchronized Border getEtchedBorder() {
        if (etchedBorder == null) {
            etchedBorder = UIManager.getBorder( "TitledBorder.aquaVariant" );
            if (etchedBorder == null)
                etchedBorder = BorderFactory.createCompoundBorder(new EtchedBorder(), BorderFactory.createEmptyBorder(5, 5, 5, 5));
        }
        return etchedBorder;
    }
    public static void setEtchedTitledBorder(JComponent comp, String title) {
        comp.setBorder(new TitledBorder(getEtchedBorder(), title));
    }
}
