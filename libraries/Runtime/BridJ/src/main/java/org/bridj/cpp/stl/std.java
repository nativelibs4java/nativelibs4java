package org.bridj.cpp.stl;

import org.bridj.*;

///http://www.codesourcery.com/public/cxx-abi/cxx-vtable-ex.html

/**
 * Util methods for STL bindings in BridJ
 * @author ochafik
 */
public final class std implements StructIO.Customizer {
    /**
     * Perform platform-dependent structure bindings adjustments
     */
	public StructIO process(StructIO io) {
		if (Platform.isWindows()) {
			Class c = io.getStructClass();
			if (c == vector.class) {
				// On Windows, vector begins by 3 pointers, before the start+finish+end pointers :
				io.prependBytes(3 * Pointer.SIZE);
			}
		}
		return io;
	}
}

