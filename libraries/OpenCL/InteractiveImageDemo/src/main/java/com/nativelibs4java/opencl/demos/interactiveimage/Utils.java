package com.nativelibs4java.opencl.demos.interactiveimage;

import com.nativelibs4java.opencl.*;
import javax.swing.*;
import java.awt.event.*;
import javax.imageio.*;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.image.*;
import java.io.*;
import java.awt.FileDialog;
import java.util.*;

import com.ochafik.swing.UndoRedoUtils;
import com.ochafik.swing.syntaxcoloring.TokenMarker;
import com.ochafik.swing.syntaxcoloring.CCTokenMarker;
import com.ochafik.swing.syntaxcoloring.JEditTextArea;
import com.ochafik.util.SystemUtils;
import java.net.URL;

class Utils {
	public static <C extends JComponent> C withTitle(String title, C c) {
		c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(title), BorderFactory.createLoweredBevelBorder()));
		return c;
	}
    public static JLabel createLinkLabel(String caption, final String urlString) {
        final JLabel[] lab = new JLabel[1];
        JLabel ret = lab[0] = createLinkLabel(caption, new Runnable() { public void run() {
            try {
                SystemUtils.runSystemOpenURL(new URL(urlString));
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(lab[0], traceToHTML(ex), "Failed to open URL", JOptionPane.ERROR_MESSAGE);
            }
        }});
        ret.setToolTipText(urlString);
        return ret;
    }
	public static JLabel createLinkLabel(String caption, final Runnable action) {
		JLabel lab = new JLabel("<html><body><a href='#'>" + caption + "</a></body></html>");
		lab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lab.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) {
			action.run();
		}});
        lab.setMaximumSize(lab.getPreferredSize());
		return lab;
	}
	
    public static boolean isMac() {
        return System.getProperty("os.name").startsWith("Mac");
    }
	public static File chooseFile(File initialFile, boolean load) {
		if (isMac()) {
			FileDialog d = new FileDialog((java.awt.Frame)null);
			d.setMode(load ? FileDialog.LOAD : FileDialog.SAVE);
            if (initialFile != null) {
                d.setDirectory(initialFile.getParent());
                d.setFile(initialFile.getName());
            }
			d.show();
			String f = d.getFile();
			if (f != null)
				return new File(new File(d.getDirectory()), d.getFile());
		} else {
	        JFileChooser chooser = new JFileChooser();
            if (initialFile != null)
                chooser.setSelectedFile(initialFile);
	        if ((load ? chooser.showOpenDialog(null) : chooser.showSaveDialog(null)) == JFileChooser.APPROVE_OPTION)
	        		return chooser.getSelectedFile();
		}
        return null;
	}
     
	public static JEditTextArea textArea(TokenMarker marker) {
		JEditTextArea ta = new JEditTextArea() {
			private static final long serialVersionUID = 1L;
//			int lastCode, lastLocation;
//			char lastChar = 0;
			
			@Override
			public void processKeyEvent(KeyEvent evt) {
				if (isMac()) {
					int m = evt.getModifiers();
					if ((m & InputEvent.META_MASK) != 0) {
						m = (m & ~InputEvent.META_MASK) | InputEvent.CTRL_MASK;
						evt = new KeyEvent(evt.getComponent(), evt.getID(), evt.getWhen(), m, evt.getKeyCode(), evt.getKeyChar(), evt.getKeyLocation());
						if (evt.getID() == KeyEvent.KEY_TYPED)
							return;
					}
				}
				
				super.processKeyEvent(evt);
			}
			@Override
			public Dimension getMinimumSize() {
				return new Dimension(100, 100);
			}
		};
		ta.setBorder(BorderFactory.createLoweredBevelBorder());
		ta.setFocusTraversalKeysEnabled(false);
		ta.addMouseWheelListener(mouseWheelListener);
		ta.setPreferredSize(new Dimension(200, 300));
		ta.setTokenMarker(marker);
		return ta;
	}
	static MouseWheelListener mouseWheelListener = new MouseWheelListener() {

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			if (!(e.getSource() instanceof JEditTextArea))
				return;
			
			JEditTextArea ta = (JEditTextArea)e.getSource();
			
			int line = ta.getFirstLine();
			if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
				int u = e.getUnitsToScroll();
				line += u > 0 ? 1 : -1;
				if (line < 0)
					line = 0;
				else if (line >= ta.getLineCount())
					line = ta.getLineCount() - 1;
				
				ta.setFirstLine(line);
			}
		}
		
	};
	public static String traceToHTML(Exception ex) {
		return "<html><body><pre><code>" + traceToString(ex).replaceAll("\n", "<br>") + "</code></pre></body></html>";
	}
		
	public static String traceToString(Exception ex) {
		StringWriter sout = new StringWriter();
		PrintWriter pout = new PrintWriter(sout);
		ex.printStackTrace(pout);
		pout.close();
		return sout.toString();
	}
	public static String readTextResource(String path) throws IOException {
		InputStream in = Utils.class.getClassLoader().getResourceAsStream(path);
		if (in == null)
			throw new FileNotFoundException(path);
		
		BufferedReader rin = new BufferedReader(new InputStreamReader(in));
		String line;
		StringBuilder b = new StringBuilder();
		while ((line = rin.readLine()) != null) {
			b.append(line);
			b.append("\n");
		}
		return b.toString();
	}
}
