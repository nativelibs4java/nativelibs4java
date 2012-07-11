#parse("main/Header.vm")
package com.nativelibs4java.opencl;

import com.ochafik.util.string.StringUtils;
import java.util.Collection;

/**
 * OpenCL program build exception
 * @author ochafik
 */
@SuppressWarnings("serial")
public class CLBuildException extends CLException {
	final CLProgram program;
	CLBuildException(CLProgram program, String string, Collection<String> errors) {
		super(string + "\n" + StringUtils.implode(errors, "\n"), -1);
		this.program = program;
	}
	public CLProgram getProgram() {
		return program;
	}
}
