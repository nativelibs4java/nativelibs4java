package com.jnaerator.velocity;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import org.apache.velocity.*;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;


public class Template {

	/**
	 * Velocity template file
	 * @required
	 */
	private File path;
	/**
	 * Velocity variable that will contain the relative path of the output file after execution of the template
	 * @parameter expression="file"
	 * @optional
	 */
	private String outputFileVariable;
	/**
	 * Each execution of a template may have different parameters
	 * @required
	 */
	private Execution[] executions;

	void execute(VelocityMojo mojo) throws MojoExecutionException {
		try {
			File dir = mojo.getOutputDirectory();

			if (!this.path.isAbsolute())
				this.path = new File(mojo.getVelocitySources(), path.toString());

			mojo.getLog().info("Executing template '" + path + "'...");
			org.apache.velocity.Template template = Velocity.getTemplate(this.path.toString());
			for (Execution execution : executions) {
				VelocityContext context = new VelocityContext(execution.getParameters());
				StringWriter out = new StringWriter();
				template.merge(context, out);
				out.close();
				Object outputFileOb = context.get(outputFileVariable);
				if (outputFileOb == null)
					throw new MojoExecutionException("No variable '" + outputFileVariable + "' was defined in template '" + template + "'. \n" +
							"It is necessary to know where the template execution result should be output.");
				File file = new File(outputFileOb.toString());
				if (!file.isAbsolute()) {
					file = new File(dir, file.toString());
				}
				file.getAbsoluteFile().getParentFile().mkdirs();
				FileWriter f = new FileWriter(file);
				f.write(out.toString());
				f.close();
				mojo.getLog().info("\tGenerated '" + file + "'");
			}
		} catch (ResourceNotFoundException ex) {
			throw new MojoExecutionException("Failed to find template '" + path + "'", ex);
		} catch (ParseErrorException ex) {
			throw new MojoExecutionException("Failed to parse template '" + path + "'", ex);
		} catch (Exception ex) {
			throw new MojoExecutionException("Failed to execut template '" + path + "'", ex);
		}
	}
}