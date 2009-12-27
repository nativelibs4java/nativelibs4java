package com.nativelibs4java.velocity;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.sun.tools.corba.se.idl.som.cff.FileLocator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.velocity.*;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

/**
 * Generates source code with velocity templates
 * @goal compile
 * @execute phase=generate-sources
 * @description Generates source code with velocity templates
 */
public class VelocityMojo
    extends AbstractMojo
{
	/**
     * Executions of the velocity engine.
	 * Each execution corresponds to one template and a set of parameters
     * @required
     */
    private List<Template> templates;

	/**
     * Source folder for velocity templates
     * @parameter expression="${project.build.directory}/../src/main/velocity/"
     * @required
     */
    private File velocitySources;

	/**
     * Source folder for velocity test templates
     * @parameter expression="${project.build.directory}/../src/test/velocity/"
     * @required
     */
    private File velocityTestSources;

    /**
     * Output directory for generated sources.
     * @parameter expression="${project.build.directory}/generated-sources/main/velocity"
     * @optional
     */
    private File outputDirectory;

    /**
     * Output directory for generated test sources.
     * @parameter expression="${project.build.directory}/generated-sources/test/velocity"
     * @optional
     */
    private File testOutputDirectory;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     * @since 1.0
     */
    private MavenProject project;

	public File getVelocitySources() {
		return velocitySources;
	}
    public File getOutputDirectory() {
		return outputDirectory;
	}

	static void listVeloFiles(File f, Collection<File> out) throws IOException {
        if (f.isHidden())
            return;

        String n = f.getName().toLowerCase();
        if (f.isDirectory()) {
            if (n.equals(".svn") || n.equals("CVS"))
                return;

            for (File ff : f.listFiles())
                listVeloFiles(ff.getAbsoluteFile(), out);
        } else if (f.isFile()) {
			//if (n.endsWith(".velo") || n.endsWith(".vm") || n.endsWith(".velocity"))
			if (!n.startsWith("."))//endsWith(".velo") || n.endsWith(".vm") || n.endsWith(".velocity"))
                out.add(f);
        }
    }

    public File getOutputFile(File vmFile, File velocitySources, File outputDirectory) throws IOException {
        String canoRoot = velocitySources.getCanonicalPath();
        String abs = vmFile.getCanonicalPath();
        String rel = abs.substring(canoRoot.length());
        String relLow = rel.toLowerCase();
        for (String suf : new String[] { ".vm", ".velo", ".velocity" }) {
            if (relLow.endsWith(suf)) {
                rel = rel.substring(0, rel.length() - suf.length());
                break;
            }
        }
        int i = rel.lastIndexOf('.');
        File out = outputDirectory;
        if (i >= 0) {
            String ext = rel.substring(i + 1);
            out = new File(out, ext);
        }

        return new File(out.getCanonicalPath() + rel);
    }
    public void execute()
        throws MojoExecutionException
    {
        if (executeAll(velocitySources, outputDirectory))
            project.addCompileSourceRoot(outputDirectory.toString());
        
        if (executeAll(velocityTestSources, testOutputDirectory))
            project.addTestCompileSourceRoot(testOutputDirectory.toString());
        
		/*if (templates == null)
			getLog().error("Did not find <templates> !");
		else {
			getLog().info("Found " + templates.size() + " templates");
			for (Template conf : templates)
				conf.execute(this);
		}*/
    }

    private boolean executeAll(File velocitySources, File outputDirectory) throws MojoExecutionException {
        List<File> files = new ArrayList<File>();
		try {
			velocitySources = velocitySources.getCanonicalFile();
			listVeloFiles(velocitySources, files);

		} catch (Exception ex) {
			throw new MojoExecutionException("Failed to list files from '" + velocitySources + "'", ex);
		}

        getLog().info("Found " + files.size() + " files in '" + velocitySources + "'...");

        if (files.isEmpty())
            return false;

        for (File file : files) {
            try {
				file = file.getCanonicalFile();

                File outFile = getOutputFile(file, velocitySources, outputDirectory);
                if (outFile.exists() && outFile.lastModified() > file.lastModified()) {
                    getLog().info("Up-to-date: '" + file + "'");
                    continue;
                }
                getLog().info("Executing template '" + file + "'...");

                Velocity.setProperty("file.resource.loader.path", file.getParent());
                Velocity.init();
                //context = new VelocityContext();
                org.apache.velocity.Template template = Velocity.getTemplate(file.getName());

                VelocityContext context = new VelocityContext();//execution.getParameters());
                StringWriter out = new StringWriter();
                template.merge(context, out);
                out.close();

                outFile.getParentFile().mkdirs();


                FileWriter f = new FileWriter(outFile);
                f.write(out.toString());
                f.close();
                getLog().info("\tGenerated '" + outFile + "'");

            } catch (Exception ex) {
                throw new MojoExecutionException("Failed to execute template '" + file + "'", ex);
            }
        }
        return true;
    }

}
