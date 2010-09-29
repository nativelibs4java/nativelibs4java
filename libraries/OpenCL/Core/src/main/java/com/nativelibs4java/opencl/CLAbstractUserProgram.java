/*
 * JavaCL - Java API and utilities for OpenCL
 * http://javacl.googlecode.com/
 *
 * Copyright (c) 2009-2010, Olivier Chafik (http://ochafik.free.fr/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Olivier Chafik nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY OLIVIER CHAFIK AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.nativelibs4java.opencl;

import com.nativelibs4java.util.IOUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.lang.reflect.Array;

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

    protected void checkArrayLength(Object arr, int length, String argName) {
        if (arr == null)
            throw new IllegalArgumentException("Argument " + argName + " cannot be null. Expected array of size " + length);

        int len = Array.getLength(arr);
        if (len != length)
            throw new IllegalArgumentException("Argument " + argName + " must be an array of size " + length + ", but given array of size " + len);
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
