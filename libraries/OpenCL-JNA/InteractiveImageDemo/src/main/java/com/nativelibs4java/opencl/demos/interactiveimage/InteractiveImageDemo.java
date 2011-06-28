package com.nativelibs4java.opencl.demos.interactiveimage;

import com.nativelibs4java.opencl.demos.SetupUtils;
import java.awt.dnd.DropTarget;
import java.net.MalformedURLException;
import java.awt.datatransfer.DataFlavor;
import java.awt.Image;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DnDConstants;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.Dimension;
import java.awt.Point;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.nativelibs4java.opencl.*;
import com.ochafik.io.ReadText;
import com.ochafik.io.WriteText;
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

/**
mvn compile exec:java -Dexec.mainClass=com.nativelibs4java.opencl.demos.interactiveimage.InteractiveImageDemo
*/
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
	
	static final String RUN_ACTION = "run", SAVE_ACTION = "save";
	File persistentFile = new File(new File(new File(System.getProperty("user.home"), ".javacl"), getClass().getSimpleName()), "Test.cl");
	
    boolean load() {
        if (!persistentFile.exists())
            return false;
        
        sourceTextArea.setText(ReadText.readText(persistentFile));
        return true;
    }
    void save() {
        try {
            WriteText.writeText(sourceTextArea.getText(), persistentFile);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, traceToHTML(ex), "Failed to save file", JOptionPane.ERROR_MESSAGE);
        }
    }
    class SaveAction extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			save();
        }
    }
    void run() {
        save();
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
                    int width = bufferedImage.getWidth(), height = bufferedImage.getHeight(); 
                    CLImage2D imageIn = context.createImage2D(CLMem.Usage.InputOutput, bufferedImage, false);
                    CLImage2D imageOut = context.createImage2D(CLMem.Usage.InputOutput, imageIn.getFormat(), width, height);

                    long startTimeNanos = System.nanoTime();
                    CLEvent lastEvent = null;
                    CLImage2D finalImageOut = null;
                    for (CLKernel kernel : kernels) {
                        setProgress("Running kernel '" + kernel.getFunctionName() + "'...");
                        try {
							kernel.setArgs(imageIn, imageOut);
							finalImageOut = imageOut;
							imageOut = imageIn;
							imageIn = finalImageOut;
							lastEvent = kernel.enqueueNDRange(queue, new int[] { width, height }, lastEvent);
						} catch (CLException ex) {
							throw new RuntimeException("Error occurred while running kernel '" + kernel.getFunctionName() + "': " + ex, ex);
						}
                    }
                    lastEvent.waitFor();
                    elapsedTimeNanos[0] = System.nanoTime() - startTimeNanos;

                    setProgress("Reading the image output...");
                    result = finalImageOut.read(queue);

                    imageIn.release();
                    imageOut.release();
                    for (CLKernel kernel : kernels)
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
	class RunAction extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			run();
		}
	}
	
	String runKeyStroke = "F5";
    
    int spacing = 10;
	
    class Example {
        public Example(String caption, String fileName) {
            this.fileName = fileName;
            this.caption = caption;
        }
        public final String fileName, caption;
        @Override
        public String toString() {
            return caption;
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
			for (Example example : new Example[] { 
				//"Blur", 
				new Example("Convolution", "Convolution"), 
				new Example("Sobel Operator", "SobelFilter"), 
				new Example("Desaturate Colors", "DesaturateColors"), 
				new Example("Richardson-Lucy Deconvolution", "RichardsonLucyDeconvolution"),
				new Example("Luminance Threshold", "LuminanceThreshold"), 
				new Example("Naive Denoising", "NaiveDenoising"), 
				new Example("Identity", "Identity"), 
				new Example("Image Info", "QueryFormat") 
			}) {
                examplesCombo.addItem(example);
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
                    loadExample(((Example)selection).fileName);
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
			toolbar.add(Box.createHorizontalStrut(spacing));
			toolbar.add(createLinkLabel("JavaCL FAQ", "http://code.google.com/p/javacl/wiki/FAQ"));
			toolbar.add(Box.createHorizontalStrut(spacing));
			toolbar.add(Box.createHorizontalGlue());
			toolbar.add(progressLabel = new JLabel());
			toolbar.add(Box.createHorizontalStrut(spacing));
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
        for (JScrollPane sp : Arrays.asList(origImgScroll, resultImgScroll)) {
            sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        }
        resultVertScrollModel = resultImgScroll.getVerticalScrollBar().getModel();
        resultHorzScrollModel = resultImgScroll.getHorizontalScrollBar().getModel();
            
        origImgLab.setDropTarget(new DropTarget(origImgLab, DnDConstants.ACTION_COPY, imgDropTargetListener));
		
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
			im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, isMac() ? KeyEvent.META_MASK : KeyEvent.CTRL_MASK), SAVE_ACTION);
			am.put(RUN_ACTION, new RunAction());
            am.put(SAVE_ACTION, new SaveAction());
		}
	}
	
    protected DropTargetListener imgDropTargetListener = new DropTargetListener() {
        
		public void dragEnter(DropTargetDragEvent dtde) {
            try {
                if (
                    dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ||
                    dtde.isDataFlavorSupported(DataFlavor.stringFlavor) ||
                    dtde.isDataFlavorSupported(DataFlavor.imageFlavor))
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                else
                    dtde.rejectDrag();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            dtde.acceptDrag(DnDConstants.ACTION_COPY);
		}
		public void dragExit(DropTargetEvent dte) {}
		public void dragOver(DropTargetDragEvent dtde) {}
		public void dropActionChanged(DropTargetDragEvent dtde) {}
		public void drop(DropTargetDropEvent dtde) {
			try {
                if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    java.util.List<File> files = (java.util.List<File>)dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (files != null && !files.isEmpty()) {
                        readImage(files.get(0).toURI().toURL());
                    }
                    dtde.dropComplete(true);
                } else if (dtde.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    try {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY);
                        readImage(new URL((String)dtde.getTransferable().getTransferData(DataFlavor.stringFlavor)));
                        dtde.dropComplete(true);
                        return ;
                    } catch (MalformedURLException ex) {}
                } else if (dtde.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Image image = (Image)dtde.getTransferable().getTransferData(DataFlavor.imageFlavor);
                    if (image instanceof BufferedImage)
                        setImage((BufferedImage)image);
                    dtde.dropComplete(true);
                    return;
                }
                dtde.rejectDrop();
            } catch (Exception ex) {
                origImgLab.setToolTipText(traceToHTML(ex));
            }
		}
	};
	BufferedImage getImage() {
		if (image == null)
			chooseImage();
		return image;
	}
	void readImage(URL url) {
		try {
            setImage(null);
			
			InputStream in = url.openStream();
			if (in == null)
				return;
			
            lastOpenedFile = new File(url.getFile());
			setImage(ImageIO.read(in));
            
            in.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			origImgLab.setText(traceToHTML(ex));
		}
	}
    void setImage(BufferedImage image) {
        this.image = image;
		origImgLab.setText(null);
		origIcon(image == null ? null : new ImageIcon(image));
	}
	void readImageResource(String name) {
		readImage(getClass().getClassLoader().getResource("images/" + name));
	}
	
	void chooseImage() {
		try {
			File f = chooseFile(lastOpenedFile, true);
			if (f == null)
				return;
			
            readImage(f.toURI().toURL());
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
        System.out.println(caption);
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
    void origIcon(Icon icon) {
        origImgLab.setIcon(icon);
        SwingUtilities.invokeLater(new Runnable() { public void run() {
            JScrollBar bar;
            BoundedRangeModel model;
            model = (bar = origImgScroll.getVerticalScrollBar()).getModel();
            model.setValue((model.getMinimum() + model.getMaximum()) / 2);
            model = (bar = origImgScroll.getHorizontalScrollBar()).getModel();
            model.setValue((model.getMinimum() + model.getMaximum()) / 2);
            
        }});
    }
	void resultError(Exception ex) {
		String html = traceToHTML(ex);
		resultIcon(null);
		resultImgLab.setText(html);
        resultImgLab.setToolTipText(html);
	}
	void loadExample(String fileName) {
		try {
			String s = readTextResource("examples/" + fileName + ".cl");
			sourceTextArea.setText(s);
			sourceTextArea.setCaretPosition(0);
		} catch (Exception ex) {
			ex.printStackTrace();
			sourceTextArea.setText("Failed to load example '" + fileName + "' :\n" + traceToString(ex));
		}
	}
	public static void main(String[] args) {
		SetupUtils.failWithDownloadProposalsIfOpenCLNotAvailable();
		
		JFrame f = new JFrame("JavaCL's Interactive Image Transform Demo");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		InteractiveImageDemo demo = new InteractiveImageDemo();
		f.getContentPane().add("Center", demo);
		f.setSize(1200, 800);
		f.setVisible(true);
		
		demo.getContext();
		demo.readImageResource("lena.jpg");
        if (!demo.load())
            demo.loadExample("Convolution");
	}
}