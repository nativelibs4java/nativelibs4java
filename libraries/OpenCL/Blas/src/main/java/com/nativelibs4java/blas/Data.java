/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas;

import java.nio.Buffer;

/**
 *
 * @author Olivier
 */
public interface Data<B extends Buffer> {
	int size();
	void read(B out);
	void write(B in);
	B read();
}
