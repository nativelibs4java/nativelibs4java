/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.demos;

import com.nativelibs4java.opencl.JavaCL;
import com.ochafik.util.SystemUtils;
import com.sun.jna.Platform;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import sun.font.Font2D;

/**
 *
 * @author ochafik
 */
public class SetupUtils {

    public static void main(String[] args) {
        JFrame f = new JFrame();
        
        class CountItem {
            public int count;
            public String string;
            public CountItem(int count, String string) {
                this.count = count;
                this.string = string;
            }
            @Override
            public String toString() {
                return string;
            }

        }
        CountItem[] items = new CountItem[] {
            new CountItem(1024, "1K"),
            new CountItem(1024 * 10,"10K"),
            new CountItem(1024 * 100,"100K"),
            new CountItem(1024 * 1000,"1M"),
            new CountItem(1024 * 10000,"10M")
        };
        JComboBox cb = new JComboBox(items);
        cb.setSelectedIndex(2);
        JLabel lb = new JLabel("Number of particles");
        Box countPanel = Box.createHorizontalBox();
        setEtchedTitledBorder(countPanel, "Particles Demo Settings");
        countPanel.add(lb);
        countPanel.add(cb);

        final JavaCLSettingsPanel sett = new JavaCLSettingsPanel();
        //sett.removeOpenGLComponents();

        final JPanel opts = new JPanel(new BorderLayout());
        JLabel detailsLab = new JLabel("<html><body><a href='#'>Advanced OpenCL settings...</a></body>", JLabel.RIGHT);
        detailsLab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        opts.add("Center", detailsLab);
        detailsLab.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                opts.removeAll();
                opts.add("Center", sett);
                opts.invalidate();
                Component c = opts.getParent();
                while (c != null) {
                    if (c instanceof Frame) {
                        ((Frame)c).pack();
                        break;
                    }
                    if (c instanceof JDialog) {
                        ((JDialog)c).pack();
                        break;
                    }
                    c = c.getParent();
                }
            }

        });

        int opt = JOptionPane.showConfirmDialog(null, new Object[] { countPanel, opts }, "JavaCL Demo Settings", JOptionPane.OK_CANCEL_OPTION);
        if (opt != JOptionPane.OK_OPTION)
            System.exit(0);

        int count = ((CountItem)cb.getSelectedItem()).count;

        
        //f.getContentPane().add("Center", sett);
        //f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //f.pack();
        //f.setVisible(true);
    }
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
        JOptionPane.showMessageDialog(null, sout.toString(), "Error in JavaCL Demo", JOptionPane.ERROR_MESSAGE);
    }

    static Border etchedBorder;
    static synchronized Border getEtchedBorder() {
        if (etchedBorder == null) {
            etchedBorder = UIManager.getBorder( "TitledBorder.aquaVariant" );
            if (etchedBorder == null)
                etchedBorder = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), new EtchedBorder());
        }
        return etchedBorder;
    }
    public static void setEtchedTitledBorder(JComponent comp, String title) {
        comp.setBorder(new TitledBorder(getEtchedBorder(), title));
    }
}
