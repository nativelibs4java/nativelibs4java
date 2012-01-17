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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import org.apache.velocity.*;
import org.apache.velocity.app.Velocity;

/**
 * Generates source code with velocity templates
 * @goal generate
 * @phase generate-sources
 * @requiresDependencyResolution compile
 * @description Generates source code with velocity templates
 */
public class VelocityMojo
    extends AbstractMojo
{
    /**
     * Extra properties
     * @parameter
     * @optional
     */
    private Map<String, String> properties;
     
	/**
     * Source folder for velocity templates
     * @parameter expression="${basedir}/src/"
     * @required
     */
    private File sourcePathRoot;

    /**
     * Source folder for velocity templates
     * @parameter expression="${basedir}/src/main/velocity/"
     * @required
     */
    private File velocitySources;

	/**
     * Source folder for velocity test templates
     * @parameter expression="${basedir}/src/test/velocity/"
     * @required
     */
    private File velocityTestSources;

    /**
     * Output directory for generated sources.
     * @parameter expression="${project.build.directory}/generated-sources/main"
     * @optional
     */
    private File outputDirectory;

    /**
     * Output directory for generated test sources.
     * @parameter expression="${project.build.directory}/generated-sources/test"
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
        //String canoRoot = sourcePathRoot.getCanonicalPath();
        String canoRoot = velocitySources.getCanonicalPath();
        //String canoSrc = velocitySources.getCanonicalPath();
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
        //if (i >= 0) {
        //    String ext = rel.substring(i + 1);
        //    out = new File(out, ext);
        //}

        return new File(out.getCanonicalPath() + rel);
    }
    public void execute()
        throws MojoExecutionException
    {
        if (executeAll(velocitySources, outputDirectory)) {
			//File jf = new File(outputDirectory, "java");
			//if (jf.exists())
			//	outputDirectory = jf;
			project.addCompileSourceRoot(outputDirectory.toString());
		}
        
        if (executeAll(velocityTestSources, testOutputDirectory)) {
			//File jf = new File(testOutputDirectory, "java");
			//if (jf.exists())
			//	testOutputDirectory = jf;
			project.addTestCompileSourceRoot(testOutputDirectory.toString());
		}
        
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
		String canoPath;
		try {
			velocitySources = velocitySources.getCanonicalFile();
			listVeloFiles(velocitySources, files);

			canoPath = sourcePathRoot.getCanonicalPath();
            getLog().info("Velocity root path = " + canoPath);
            Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM, new MavenLogChute(getLog()));
			Velocity.setProperty("file.resource.loader.path", canoPath);//file.getParent());
			Velocity.init();
					
			
		} catch (Exception ex) {
			throw new MojoExecutionException("Failed to list files from '" + velocitySources + "'", ex);
		}

        getLog().info("Found " + files.size() + " files in '" + velocitySources + "'...");
        getLog().info("Got properties : " + properties);

        if (files.isEmpty())
            return false;

		for (File file : files) {
            try {
				file = file.getCanonicalFile();

				String name = file.getName();
				if (name.endsWith("~") || name.endsWith(".bak")) {
					getLog().info("Skipping: '" + name + "'");
                    continue;
				}
					
                File outFile = getOutputFile(file, velocitySources, outputDirectory);
                if (outFile.exists() && outFile.lastModified() > file.lastModified()) {
                    getLog().info("Up-to-date: '" + name + "'");
                    continue;
                }
                getLog().info("Executing template '" + name + "'...");

                //context = new VelocityContext();
				String cano = file.getCanonicalPath();
				cano = cano.substring(canoPath.length());
				if (cano.startsWith(File.separator))
					cano = cano.substring(File.separator.length());
				
                org.apache.velocity.Template template = Velocity.getTemplate(cano);//file.getName());

                VelocityContext context = new VelocityContext();//execution.getParameters());
                context.put("primitives", Primitive.getPrimitives());
                context.put("primitivesNoBool", Primitive.getPrimitivesNoBool());
                context.put("bridJPrimitives", Primitive.getBridJPrimitives());
                
                if (properties != null) {
                	for (Map.Entry<String, String> e : properties.entrySet()) {
                		String propName = e.getKey(), propValue = e.getValue();
                		getLog().debug("Got property : " + propName + " = " + propValue);
                
						context.put(propName, propValue);
					}
				}
                
                StringWriter out = new StringWriter();
                template.merge(context, out);
                out.close();

                outFile.getParentFile().mkdirs();


                FileWriter f = new FileWriter(outFile);
                f.write(out.toString());
                f.close();
                //getLog().info("\tGenerated '" + outFile.getName() + "'");

            } catch (Exception ex) {
                //throw 
				new MojoExecutionException("Failed to execute template '" + file + "'", ex).printStackTrace();
            }
        }
		
        return true;
    }
}
