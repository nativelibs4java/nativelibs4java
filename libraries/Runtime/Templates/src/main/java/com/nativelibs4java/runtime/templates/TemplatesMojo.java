package com.nativelibs4java.runtime.templates;

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

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

/**
 * Generates source code with velocity templates
 * @goal compile
 * @execute phase=generate-sources
 * @description Generates source code with velocity templates
 */
public class TemplatesMojo
    extends AbstractMojo
{
	/**
     * @parameter
     * @optional
     */
    private Map<Object, Object> parameters;
	
	/**
     * @parameter
     * @optional
     */
    private String[] templates;

    /**
     * @parameter
     * @optional
     */
    private String[] testTemplates;

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

    public File getOutputDirectory() {
		return outputDirectory;
	}

    public void execute()
        throws MojoExecutionException
    {
        VelocityEngine ve;
        try {
            ve = new VelocityEngine();
            ve.setProperty("resource.loader", "class");
            ve.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            ve.init();
            //Velocity.init();
            //System.out.println("VELOCITY PARAMETERS = " + parameters);
		} catch (Exception ex) {
            throw new MojoExecutionException("Failed to initialize Velocity", ex);
        }
        if (templates != null && templates.length != 0) {
            getLog().info("Found " + templates.length + " templates");
            for (String resource : templates)
                execute(ve, resource, outputDirectory);
            
			//File jf = new File(outputDirectory, "java");
			//if (jf.exists())
			//	outputDirectory = jf;
			project.addCompileSourceRoot(outputDirectory.toString());
			
        } else {
            getLog().info("No templates configuration");

        }

        if (testTemplates != null && testTemplates.length != 0) {
            getLog().info("Found " + testTemplates.length + " test templates");
            for (String resource : testTemplates)
                execute(ve, resource, testOutputDirectory);

            //File jf = new File(testOutputDirectory, "java");
			//if (jf.exists())
			//	testOutputDirectory = jf;
			project.addTestCompileSourceRoot(testOutputDirectory.toString());
        } else {
            getLog().info("No testTemplates configuration");

        }
    }

    @SuppressWarnings("unchecked")
	private void execute(VelocityEngine ve, String resource, File outDir) throws MojoExecutionException {
        try {
            org.apache.velocity.Template template = ve.getTemplate(resource);

            VelocityContext context = new VelocityContext(new HashMap(parameters));
            context.put("primitives", Primitive.getPrimitives());
            context.put("primitivesNoBool", Primitive.getPrimitivesNoBool());

            StringWriter out = new StringWriter();
            template.merge(context, out);
            out.close();

            File outFile = null;
            Object s = context.get("outputFile");
            if (s != null)
                outFile = new File(s.toString());
            else {
                s = context.get("relativeOutputFile");
                if (s != null)
                    outFile = new File(outDir, s.toString());
                else {
                    s = context.get("package");
                    if (s != null)
                        outFile = new File(new File(outDir, s.toString().replace('.', File.separatorChar)), new File(resource).getName());
                    else {
                        getLog().info("No 'outputFile' nor 'relativeOutputFile' variable defined. Using template resource name.");
                        outFile = new File(outDir, resource);
                    }
                }
            }
            outFile.getParentFile().mkdirs();

            getLog().info("Writing template '" + resource + "' to '" + outFile + "'");


            FileWriter f = new FileWriter(outFile);
            f.write(out.toString());
            f.close();

        } catch (Exception ex) {
            throw new MojoExecutionException("Failed to execute template '" + resource + "'", ex);
        }
    }

}
