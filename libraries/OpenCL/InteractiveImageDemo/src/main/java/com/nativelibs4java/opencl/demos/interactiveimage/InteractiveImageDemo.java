package com.nativelibs4java.opencl.demos.interactiveimage;

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

public class InteractiveImageDemo extends JPanel {
	JSplitPane imgSrcSplitPane, imgsSplitPane;
	JLabel origImgLab, resultImgLab, contextLabel, instructionsLabel, timeLabel, progressLabel;
	JTextArea sourceTextArea;
	JButton runButton, changeContextButton;
	BufferedImage image, result;
	JProgressBar progressBar;
	
	CLContext context;
	JComponent[] toDisable;
	
	static final String RUN_ACTION = "run";
	class RunAction extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			try {
				final BufferedImage bufferedImage = getImage();
					if (bufferedImage == null)
						return;
					
				final CLContext context = getContext();
				if (context == null)
					return;
				
				for (JComponent c : toDisable)
					c.setEnabled(false);
				resultImgLab.setText(null);
				resultImgLab.setIcon(null);
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
						
						setProgress(null);
						
						imageIn.release();
						imageOut.release();
						kernel.release();
						program.release();
						queue.release();
						
						SwingUtilities.invokeLater(new Runnable() { public void run() {
							resultImgLab.setIcon(result == null ? null : new ImageIcon(result));
							resultImgLab.setToolTipText(result == null ? null : "Click to save this image");
						}});
					} catch (Exception ex) {
						resultError(ex);
					} finally {
						SwingUtilities.invokeLater(new Runnable() { public void run() {
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
				resultError(ex);
			}
		}
	}
	
	<C extends JComponent> C withTitle(String title, C c) {
		c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(title), BorderFactory.createLoweredBevelBorder()));
		return c;
	}
	JLabel createLinkLabel(String caption, final Runnable action) {
		JLabel lab = new JLabel("<html><body><a href='#'>" + caption + "</a></body></html>");
		lab.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lab.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) {
			action.run();
		}});
		return lab;
	}
	
	String runKeyStroke = "F5";
	
	public InteractiveImageDemo() {
		super(new BorderLayout());
		JPanel srcPanel = new JPanel(new BorderLayout());
		srcPanel.add("Center", withTitle("Image transformation kernel source code", new JScrollPane(sourceTextArea = new JTextArea())));
		{
			InputMap im = sourceTextArea.getInputMap();
			ActionMap am = sourceTextArea.getActionMap();
			im.put(KeyStroke.getKeyStroke(runKeyStroke), RUN_ACTION);
			am.put(RUN_ACTION, new RunAction());
		}

		
		runButton = new JButton("Run (" + runKeyStroke + ")");
		changeContextButton = new JButton("Define OpenCL context");
		{
			Box instructions = Box.createHorizontalBox();
			final String signature = "void transform(int imageType, int width, int height, __global read_only image2d imgIn, __global write_only image2d imgOut)";
			
			instructionsLabel = new JLabel("Load example :");
			instructionsLabel.setToolTipText("Expecting a kernel '" + signature + "'"); 
			instructions.add(instructionsLabel);
			for (String example : new String[] { "DummySample" }) {
				instructions.add(Box.createHorizontalStrut(5));
				final String x = example;
				instructions.add(createLinkLabel(example, new Runnable() { public void run() {
					loadExample(x);
				}}));
			}
			instructionsLabel.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) {
				String t = sourceTextArea.getText();
				if (t.trim().length() > 0)
					t = t + "\n";
				sourceTextArea.setText(t + "__kernel " + signature + " {\n\tint x = get_global_id(0), y = get_global_id(1);\n\t// write here\n}");	
			}});
			srcPanel.add("North", instructions);
		}
		{
			Box toolbar = Box.createHorizontalBox();
			toolbar.add(runButton);
			//toolbar.add(changeContextButton);
			changeContextButton.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) {
				defineContext();
			}});
			toolbar.add(contextLabel = new JLabel("<no OpenCL context defined>"));
			toolbar.add(Box.createHorizontalStrut(5));
			toolbar.add(Box.createHorizontalGlue());
			toolbar.add(progressLabel = new JLabel());
			toolbar.add(Box.createHorizontalStrut(5));
			toolbar.add(progressBar = new JProgressBar());
			toolbar.add(timeLabel = new JLabel());
			progressLabel.setVisible(false);
			progressBar.setVisible(false);
			timeLabel.setVisible(false);
			srcPanel.add("South", toolbar);
		}
		add("Center", imgSrcSplitPane = new JSplitPane(
			JSplitPane.VERTICAL_SPLIT, 
			imgsSplitPane = new JSplitPane(
				JSplitPane.HORIZONTAL_SPLIT,
				withTitle("Input", new JScrollPane(origImgLab = new JLabel())),
				withTitle("Output", new JScrollPane(resultImgLab = new JLabel()))
			),
			srcPanel
		));
		imgSrcSplitPane.setResizeWeight(0.3);
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
			runButton,
			changeContextButton,
			sourceTextArea,
			origImgLab,
			resultImgLab
		};
		
		runButton.requestFocus();
	}
	
	static File chooseFile(boolean load) {
		if (System.getProperty("os.name").startsWith("Mac")) {
			FileDialog d = new FileDialog((java.awt.Frame)null);
			d.setMode(load ? FileDialog.LOAD : FileDialog.SAVE);
			d.show();
			String f = d.getFile();
			if (f != null)
				return new File(new File(d.getDirectory()), d.getFile());
		} else {
	        JFileChooser chooser = new JFileChooser();
	        if ((load ? chooser.showOpenDialog(null) : chooser.showSaveDialog(null)) == JFileChooser.APPROVE_OPTION)
	        		return chooser.getSelectedFile();
		}
        return null;
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
			
			image = ImageIO.read(in);
			origImgLab.setIcon(image == null ? null : new ImageIcon(image));
		} catch (Exception ex) {
			origImgLab.setText(toHTML(ex));
		}
	}
	void chooseImage() {
		try {
			File f = chooseFile(true);
			if (f == null)
				return;
			
			origImgLab.setText(null);
			origImgLab.setIcon(null);
			
			image = ImageIO.read(f);
			origImgLab.setIcon(image == null ? null : new ImageIcon(image));
		} catch (Exception ex) {
			origImgLab.setText(toHTML(ex));
		}
	}
	void saveResult() {
		if (result == null)
			return;
		
		try {
			File f = chooseFile(false);
			if (f == null)
				return;
			
			ImageIO.write(result, "png", f);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, toHTML(ex), "Failed to write image", JOptionPane.ERROR_MESSAGE);
		}
	}
	CLContext getContext() {
		if (context == null)
			defineContext();
		
		return context;
	}
	void defineContext() {
		context = JavaCL.createBestContext();
		contextLabel.setText(Arrays.asList(context.getDevices()).toString());
	}
	void setProgress(final String caption) {
		SwingUtilities.invokeLater(new Runnable() { public void run() {
			progressLabel.setVisible(caption != null);
			progressLabel.setText(caption);
		}});
	}
	void resultError(Exception ex) {
		String html = toHTML(ex);
		resultImgLab.setText(html);
		resultImgLab.setToolTipText(html);
	}
	String toHTML(Exception ex) {
		return "<html><body><pre><code>" + toString(ex).replaceAll("\n", "<br>") + "</code></pre></body></html>";
	}
		
	String toString(Exception ex) {
		StringWriter sout = new StringWriter();
		PrintWriter pout = new PrintWriter(sout);
		ex.printStackTrace(pout);
		pout.close();
		return sout.toString();
	}
	void loadExample(String name) {
		try {
			String s = readTextResource("examples/" + name + ".cl");
			sourceTextArea.setText(s);
			sourceTextArea.setCaretPosition(0);
		} catch (Exception ex) {
			sourceTextArea.setText("Failed to load example '" + name + "' :\n" + toString(ex));
		}
	}
	String readTextResource(String path) throws IOException {
		InputStream in = getClass().getClassLoader().getResourceAsStream(path);
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
	public static void main(String[] args) {
		JFrame f = new JFrame("JavaCL's Interactive Image Transform Demo");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		InteractiveImageDemo demo = new InteractiveImageDemo();
		f.getContentPane().add("Center", demo);
		f.setSize(1000, 800);
		f.setVisible(true);
		
		demo.getContext();
		demo.readImageResource("lena.jpg");
		demo.loadExample("DummySample");
	}
}