/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.generator;

import com.ochafik.io.IOUtils;
import com.ochafik.lang.jnaerator.JNAerator.Feedback;
import com.ochafik.lang.jnaerator.JNAeratorConfig;
import com.ochafik.lang.jnaerator.SourceFiles;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.ArrayList;

/**
 * Launch JNAerator to wrap native libraries in Java for use with JNA.
 * @goal compile
 * @execute phase=generate-sources
 * @description Launches JNAerator with the command-line arguments contained in src/main/jnaerator/config.jnaerator. To launch from command line, use "mvn com.jnaerator:maven-jnaerator:jnaerate"
 */
public class JavaCLGeneratorMojo
    extends AbstractMojo
{
    /**
     * Configuration file for JNAerator.
     * @parameter expression="${project.build.directory}/../src/main/jnaerator/config.jnaerator"
     * @required
     */
    //private File config;

    /**
     * OpenCL sources tree.
     * @parameter expression="${project.build.directory}/../src/main/opencl"
     * @required
     */
    private File sourcesDirectory;

    /**
     * OpenCL test sources tree.
     * @parameter expression="${project.build.directory}/../src/test/opencl"
     * @required
     */
    private File testSourcesDirectory;

    /**
     * Output directory for JNAerated Java sources.
     * @parameter expression="${project.build.directory}/generated-sources/main/java"
     * @optional
     */
    private File javaOutputDirectory;

    /**
     * Output directory for JNAerated Java test sources.
     * @parameter expression="${project.build.directory}/generated-sources/test/java"
     * @optional
     */
    private File testJavaOutputDirectory;

    /**
     * Output directory for OpenCL sources.
     * @parameter expression="${project.build.directory}/generated-sources/main/resources"
     * @optional
     */
    private File openCLOutputDirectory;

    /**
     * Output directory for OpenCL test sources.
     * @parameter expression="${project.build.directory}/generated-sources/test/resources"
     * @optional
     */
    private File testOpenCLOutputDirectory;

    static File canonizeDir(File f) throws IOException {
        if (!f.exists())
            f.mkdirs();
        return f.getCanonicalFile();
    }

    static void listOpenCLFiles(File f, Collection<File> out) {
        if (f.isHidden())
            return;

        String n = f.getName().toLowerCase();
        if (f.isDirectory()) {
            if (n.equals(".svn"))
                return;

            for (File ff : f.listFiles())
                listOpenCLFiles(ff, out);
        } else {
            if (n.endsWith(".c") || n.endsWith(".cl"))
                out.add(f);
        }
    }

    public void generateAll(File root, File javaOutDir, File openCLOutDir) throws IOException, MojoExecutionException {
        List<File> sources = new ArrayList<File>();
        listOpenCLFiles(root.getCanonicalFile(), sources);

        System.out.println("Found " + sources.size() + " files in " + root);
        String rootPath = root.getCanonicalPath();
        String openCLOutPath = openCLOutDir.getCanonicalPath();
        String javaOutPath = javaOutDir.getCanonicalPath();
        for (File file : sources) {
            final JNAeratorConfig config = new JNAeratorConfig();
            config.autoConf = true;
            config.compile = false;
            config.outputJar = null;
            if (!javaOutDir.exists())
                javaOutDir.mkdirs();
            config.outputDir = javaOutDir;
            config.addSourceFile(file, null, false);

            final String fileName = file.getName();
            String filePath = file.getCanonicalPath();
            File openCLOutFile = new File(openCLOutPath + filePath.substring(rootPath.length()));

            int i = filePath.lastIndexOf(".");
            File javaOutFile = new File(javaOutPath + filePath.substring(rootPath.length(), i) + ".java");
            if (javaOutFile.exists() && javaOutFile.lastModified() > file.lastModified()) {
                System.out.println("File " + fileName + " is up-to-date. Skipping generation.");
                continue;
            }

            File openCLOutParent = openCLOutFile.getParentFile();
            if (!openCLOutParent.exists())
                openCLOutParent.mkdirs();

            System.out.println("Copying " + file + " to " + openCLOutFile);
            FileWriter out = new FileWriter(openCLOutFile);
            FileReader in = new FileReader(file);
            IOUtils.readWrite(in, out);
            out.close();
            in.close();
            
            JavaCLGenerator generator = new JavaCLGenerator(config);
            final Throwable[] ex = new Throwable[1];
            generator.jnaerate(new Feedback() {

                @Override
                public void setStatus(String string) {
                    if (config.verbose)
                        System.out.println(string);
                }

                @Override
                public void setFinished(Throwable e) {
                    System.out.println("JNAeration failed !");
                    e.printStackTrace();
                    ex[0] = e;
                }

                @Override
                public void setFinished(File toOpen) {
                    System.out.println("JNAeration of " + fileName + " completed in " + toOpen.getAbsolutePath());
                }

                @Override
                public void sourcesParsed(SourceFiles sourceFiles) {

                }

                @Override
                public void wrappersGenerated(
                        com.ochafik.lang.jnaerator.Result result) {
                }
            });
            if (ex[0] != null)
                throw new MojoExecutionException( "Error JNAerating " + fileName, ex[0]);
        }
    }
    public void execute()
        throws MojoExecutionException
    {
        try
        {
            generateAll(sourcesDirectory, javaOutputDirectory, openCLOutputDirectory);
            generateAll(testSourcesDirectory, testJavaOutputDirectory, testOpenCLOutputDirectory);
        }
        catch (MojoExecutionException e )
        {
            throw e;
        }
        catch (Exception e )
        {
            throw new MojoExecutionException( "Error running JNAerator", e );
        }
    }
}