/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;

import com.ochafik.util.string.StringUtils;
import java.util.Collection;

/**
 * OpenCL program build exception
 * @author ochafik
 */
@SuppressWarnings("serial")
public class CLBuildException extends Exception {
	final CLProgram program;
	public CLBuildException(CLProgram program, String string, Collection<String> errors) {
		super(string + "\n" + StringUtils.implode(errors, "\n"));
		this.program = program;
	}
	public CLProgram getProgram() {
		return program;
	}
}
