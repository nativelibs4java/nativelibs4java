package com.nativelibs4java.opencl.demos.interactiveimage;

import java.awt.Point;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.nativelibs4java.opencl.*;
import javax.swing.*;
import java.awt.event.*;
import javax.imageio.*;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.image.*;
import java.io.*;
import java.awt.FileDialog;
import java.util.*;

import com.ochafik.swing.UndoRedoUtils;
import com.ochafik.swing.syntaxcoloring.TokenMarker;
import com.ochafik.swing.syntaxcoloring.CCTokenMarker;
import com.ochafik.swing.syntaxcoloring.JEditTextArea;
import com.ochafik.util.SystemUtils;

import static com.nativelibs4java.opencl.demos.interactiveimage.Utils.*;

public class InteractiveImageDemo extends JPanel {
	JSplitPane imgSrcSplitPane, imgsSplitPane;
	JLabel origImgLab, resultImgLab, instructionsLabel, timeLabel, progressLabel;
	JScrollPane origImgScroll, resultImgScroll;
    
	JEditTextArea sourceTextArea;
	JComboBox devicesCombo, examplesCombo;
	//JTextArea sourceTextArea;
	
	JButton runButton;
	BufferedImage image, result;
	JProgressBar progressBar;
	
	JComponent[] toDisable;
    File lastOpenedFile;
	
	static final String RUN_ACTION = "run";
	class RunAction extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			try {
				final BufferedImage bufferedImage = getImage();
					if (bufferedImage == null)
						return;
					
                // Could just be this : final CLContext context = JavaCL.createBestContext();
				final CLContext context = getContext();
				if (context == null)
					return;
				
                final Point initialViewPosition = origImgScroll.getViewport().getViewPosition();
				
				for (JComponent c : toDisable)
					c.setEnabled(false);
				resultImgLab.setText(null);
				resultIcon(null);
				resultImgLab.setToolTipText(null);
				result = null;
				timeLabel.setVisible(false);
				progressBar.setIndeterminate(true);
				progressBar.setVisible(true);
				setProgress("Initializing...");
                
                final long[] elapsedTimeNanos = new long[] { -1L }; 
				new Thread() { public void run() {
					try {
						setProgress("Creating OpenCL queue...");
						CLQueue queue = context.createDefaultQueue();
						setProgress("Compiling program...");
						CLProgram program = context.createProgram(sourceTextArea.getText());
						CLKernel[] kernels = program.createKernels();
						if (kernels.length == 0)
							throw new RuntimeException("No kernels found in the source code ! (please mark a function with __kernel)");
						
						setProgress("Creating OpenCL images...");
						CLKernel kernel = kernels[0]; // taking first kernel...
						int width = bufferedImage.getWidth(), height = bufferedImage.getHeight(); 
						
						CLImage2D imageIn = context.createImage2D(CLMem.Usage.Input, bufferedImage, false);
						System.out.println("Image format = " + imageIn.getFormat());
						CLImage2D imageOut = context.createImage2D(CLMem.Usage.Output, imageIn.getFormat(), width, height);
						
						setProgress("Running the kernel...");
						kernel.setArgs(imageIn, imageOut);
						long startTimeNanos = System.nanoTime();
						kernel.enqueueNDRange(queue, new int[] { width, height }, null).waitFor();
						elapsedTimeNanos[0] = System.nanoTime() - startTimeNanos;
						
						setProgress("Reading the image output...");
						result = imageOut.read(queue);
						System.out.println("result = " + result);
						
						imageIn.release();
						imageOut.release();
						kernel.release();
						program.release();
						queue.release();
						
						SwingUtilities.invokeLater(new Runnable() { public void run() {
							resultIcon(result == null ? null : new ImageIcon(result));
							resultImgLab.setToolTipText(result == null ? null : "Click to save this image");
                        
                            SwingUtilities.invokeLater(new Runnable() { public void run() {
                                origImgScroll.getViewport().setViewPosition(initialViewPosition);
                            }});
						}});
					} catch (Exception ex) {
						ex.printStackTrace();
						resultError(ex);
					} finally {
						SwingUtilities.invokeLater(new Runnable() { public void run() {
								setProgress(null);
						
                                for (JComponent c : toDisable)
									c.setEnabled(true);
								progressBar.setIndeterminate(false);
								progressBar.setVisible(false);
								if (elapsedTimeNanos[0] >= 0) {
									timeLabel.setText("Completed in " + (elapsedTimeNanos[0] / 1000000.0) + " msecs");
									timeLabel.setVisible(true);
								}
						}});
					}
				}}.start();
			} catch (Exception ex) {
				ex.printStackTrace();
				resultError(ex);
			}
		}
	}
	
	String runKeyStroke = "F5";
	
    class Example {
        public Example(String name) {
            this.name = name;
        }
        public final String name;
        @Override
        public String toString() {
            return name;
        }
        
    }
	public InteractiveImageDemo() {
		super(new BorderLayout());
		
		devicesCombo = new JComboBox();
		List<CLDevice> devices = new ArrayList<CLDevice>();
		try {
			for (CLPlatform platform : JavaCL.listPlatforms()) {
				for (CLDevice device : platform.listAllDevices(true)) {
					devicesCombo.addItem(device);
					devices.add(device);
				}
			}
			if (!devices.isEmpty())
				devicesCombo.setSelectedItem(CLPlatform.getBestDevice(Arrays.asList(CLPlatform.DeviceFeature.MaxComputeUnits), devices));
		} catch (Exception ex) {
			ex.printStackTrace();
			devicesCombo.setToolTipText(traceToHTML(ex));
		}
		if (devices.isEmpty()) {
			devicesCombo.addItem("No OpenCL Device detected");
		}
        
        examplesCombo = new JComboBox();
        examplesCombo.addItem("Examples...");
        {
			final String signature = "__kernel void transform(__global read_only image2d inputImage, __global write_only image2d outputImage)";
			
			examplesCombo.setToolTipText("Kernel samples in the form of :\n'" + signature + "'"); 
			for (String example : new String[] { "Greyifier", "Blur", "SobelFilter", "Identity", "QueryFormat" }) {
                examplesCombo.addItem(new Example(example));
			}
			examplesCombo.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) {
				String t = sourceTextArea.getText();
				if (t.trim().length() > 0)
					t = t + "\n";
				sourceTextArea.setText(t + "__kernel " + signature + " {\n\tint x = get_global_id(0), y = get_global_id(1);\n\t// write here\n}");	
			}});
            examplesCombo.addItemListener(new ItemListener() { public void itemStateChanged(ItemEvent e) {
                Object selection = examplesCombo.getSelectedItem();
                if (selection instanceof Example) {
                    loadExample(((Example)selection).name);
                    examplesCombo.setSelectedIndex(0);
                }
            }});
		}
			
		JPanel srcPanel = new JPanel(new BorderLayout());
		sourceTextArea = textArea(new CCTokenMarker());
		srcPanel.add("Center", withTitle("Image transformation kernel source code", sourceTextArea));
		
		runButton = new JButton("Run (" + runKeyStroke + ")");
        {
			Box toolbar = Box.createHorizontalBox();
            for (JComponent c : new JComponent[] { examplesCombo, runButton, devicesCombo })
                c.setMaximumSize(c.getPreferredSize());
        
            runButton.putClientProperty("JButton.buttonType", "bevel");
            examplesCombo.putClientProperty("JComboBox.isPopDown", Boolean.TRUE);
            devicesCombo.putClientProperty("JComboBox.isPopDown", Boolean.TRUE);
                
            toolbar.add(examplesCombo);
			toolbar.add(runButton);
			toolbar.add(devicesCombo);
			toolbar.add(createLinkLabel("Khronos OpenCL Documentation", "http://www.khronos.org/registry/cl/sdk/1.0/docs/man/xhtml/"));
			toolbar.add(Box.createHorizontalStrut(5));
			toolbar.add(createLinkLabel("JavaCL FAQ", "http://code.google.com/p/javacl/wiki/FAQ"));
			toolbar.add(Box.createHorizontalStrut(5));
			toolbar.add(Box.createHorizontalGlue());
			toolbar.add(progressLabel = new JLabel());
			toolbar.add(Box.createHorizontalStrut(5));
			toolbar.add(progressBar = new JProgressBar());
            progressBar.putClientProperty("JProgressBar.style", "circular");
			toolbar.add(timeLabel = new JLabel());
            progressBar.setMaximumSize(progressBar.getPreferredSize());
            progressLabel.setVisible(false);
			progressBar.setVisible(false);
			timeLabel.setVisible(false);
			srcPanel.add("South", toolbar);
		}
        origImgScroll = new JScrollPane(origImgLab = new JLabel());
        resultImgScroll = new JScrollPane(resultImgLab = new JLabel());
        resultVertScrollModel = resultImgScroll.getVerticalScrollBar().getModel();
        resultHorzScrollModel = resultImgScroll.getHorizontalScrollBar().getModel();
            
        
        add("Center", imgSrcSplitPane = new JSplitPane(
			JSplitPane.VERTICAL_SPLIT, 
			imgsSplitPane = new JSplitPane(
				JSplitPane.HORIZONTAL_SPLIT,
				withTitle("Input", origImgScroll),
				withTitle("Output", resultImgScroll)
			),
			srcPanel
		));
		imgSrcSplitPane.setResizeWeight(0.5);
		imgsSplitPane.setResizeWeight(0.5);
		
		origImgLab.setToolTipText("Click to load a different image");
		origImgLab.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) {
			chooseImage();	
		}});
		resultImgLab.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) {
			saveResult();
		}});
			
		runButton.addActionListener(new RunAction());
		
		toDisable = new JComponent[] {
            examplesCombo,
			runButton,
			devicesCombo,
			sourceTextArea,
			//origImgLab,
			resultImgLab
		};
		
        UndoRedoUtils.registerNewUndoManager(sourceTextArea, sourceTextArea.getDocument());
		for (JComponent focusable : Arrays.asList(sourceTextArea, examplesCombo, devicesCombo, runButton))	{
			InputMap im = focusable.getInputMap();
			ActionMap am = focusable.getActionMap();
			im.put(KeyStroke.getKeyStroke(runKeyStroke), RUN_ACTION);
			am.put(RUN_ACTION, new RunAction());
		}
	}
	
	BufferedImage getImage() {
		if (image == null)
			chooseImage();
		return image;
	}
	void readImageResource(String name) {
		try {
			origImgLab.setText(null);
			origImgLab.setIcon(null);
			
			InputStream in = getClass().getClassLoader().getResourceAsStream("images/" + name);
			if (in == null)
				return;
			
            lastOpenedFile = new File(name);
			image = ImageIO.read(in);
			origImgLab.setIcon(image == null ? null : new ImageIcon(image));
		} catch (Exception ex) {
			ex.printStackTrace();
			origImgLab.setText(traceToHTML(ex));
		}
	}
	
	void chooseImage() {
		try {
			File f = chooseFile(lastOpenedFile, true);
			if (f == null)
				return;
			
            lastOpenedFile = f;
			origImgLab.setText(null);
			origImgLab.setIcon(null);
			
			image = ImageIO.read(f);
			origImgLab.setIcon(image == null ? null : new ImageIcon(image));
		} catch (Exception ex) {
			ex.printStackTrace();
			origImgLab.setText(traceToHTML(ex));
		}
	}
    String getOutputFormat(File file) {
        if (file != null) {
            String s = file.getName().toLowerCase();
            if (s.matches(".*?\\.jpe?g"))
                return "jpeg";
            for (String ex : new String[] { "png", "gif", "tiff", "pnm", "pbm" })
                if (s.matches(".*?\\." + ex))
                    return ex;
        }
        return "png";
    }
    static Pattern fileExtRx = Pattern.compile("(.*?)(\\.[^.]+)?");
	void saveResult() {
		if (result == null)
			return;
		
		try {
            File f = null;
            if (lastOpenedFile != null) {
                Matcher matcher = fileExtRx.matcher(lastOpenedFile.getName());
                if (matcher.matches()) {
                    String body = matcher.group(1);
                    String ext = matcher.group(2);
                    f = new File(lastOpenedFile.getParentFile(), body + ".transformed" + (ext == null ? ".png" : ext));
                }
            }
            
            f = chooseFile(f, false);
			if (f == null)
				return;
			
			ImageIO.write(result, getOutputFormat(f), f);
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, traceToHTML(ex), "Failed to write image", JOptionPane.ERROR_MESSAGE);
		}
	}
	CLContext getContext() {
		Object selection = devicesCombo.getSelectedItem();
		if (!(selection instanceof CLDevice))
			return null;
		
		CLDevice device = (CLDevice)selection;
		CLContext context = JavaCL.createContext(null, device);
		return context;
	}
	void setProgress(final String caption) {
		SwingUtilities.invokeLater(new Runnable() { public void run() {
			//progressLabel.setVisible(caption != null);
			//progressLabel.setText(caption);
            if (!isMac()) {
                progressBar.setStringPainted(caption != null);
                progressBar.setString(caption);
            }
            progressBar.setToolTipText(caption);
		}});
	}
    BoundedRangeModel resultVertScrollModel, resultHorzScrollModel;
    void resultIcon(Icon icon) {
        if (icon == null) {
            resultImgScroll.getVerticalScrollBar().setModel(resultVertScrollModel);
            resultImgScroll.getHorizontalScrollBar().setModel(resultHorzScrollModel);
        } else {
            resultImgScroll.getVerticalScrollBar().setModel(origImgScroll.getVerticalScrollBar().getModel());
            resultImgScroll.getHorizontalScrollBar().setModel(origImgScroll.getHorizontalScrollBar().getModel());
        }
        resultImgLab.setIcon(icon);
    }
	void resultError(Exception ex) {
		String html = traceToHTML(ex);
		resultIcon(null);
		resultImgLab.setText(html);
        resultImgLab.setToolTipText(html);
	}
	void loadExample(String name) {
		try {
			String s = readTextResource("examples/" + name + ".cl");
			sourceTextArea.setText(s);
			sourceTextArea.setCaretPosition(0);
		} catch (Exception ex) {
			ex.printStackTrace();
			sourceTextArea.setText("Failed to load example '" + name + "' :\n" + traceToString(ex));
		}
	}
	public static void main(String[] args) {
		JFrame f = new JFrame("JavaCL's Interactive Image Transform Demo");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		InteractiveImageDemo demo = new InteractiveImageDemo();
		f.getContentPane().add("Center", demo);
		f.setSize(1200, 800);
		f.setVisible(true);
		
		demo.getContext();
		demo.readImageResource("lena.jpg");
		demo.loadExample("Blur");
	}
}