package com.nativelibs4java.opencl;

import com.nativelibs4java.util.IOUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author ochafik
 */
public abstract class CLAbstractUserProgram {

    final CLProgram program;
    String rawSource;
    Map<String, String> velocityArgs;
    //Map<String, CLKernel> kernels;

    volatile boolean addedSources;
    
	protected Long getSourceChecksum() {
        return null;
    }

	public CLProgram getProgram() {
		return program;
	}
    
    protected static String readRawSourceForClass(Class<?> c) throws IOException {
        String simpleName = c.getSimpleName();

		InputStream srcIn = c.getResourceAsStream(simpleName + ".cl");
        try {
            if (srcIn == null)
                srcIn = c.getResourceAsStream(simpleName + ".c");

            if (srcIn == null)
                throw new FileNotFoundException("OpenCL source code not found : '" + simpleName + "'");

            return IOUtils.readText(srcIn);
        } finally {
            if (srcIn != null)
                srcIn.close();
        }
    }
    protected CLAbstractUserProgram(CLProgram program, String rawSource) {
        this.program = program;
        this.rawSource = rawSource;
    }

    protected CLAbstractUserProgram(CLContext context, String rawSource) {
        this(context.createProgram(), rawSource);
    }

    protected synchronized void addSources() {
        if (addedSources) {
            return;
        }

        String src;
        if (velocityArgs != null) {
            src = rawSource;//veloTransform(rawSource, velocityArgs);
        } else {
            src = rawSource;
        }

        program.addSource(src);
        addedSources = true;
    }

    protected void defineMacro(String name, String value) {
        if (value == null) {
            program.undefineMacro(name);
        } else {
            program.defineMacro(name, value);
        }
    }

    protected synchronized CLKernel createKernel(String name) throws CLBuildException {
        addSources();
        return program.createKernel(name);
    }
}
