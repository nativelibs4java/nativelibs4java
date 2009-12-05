/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.blas;

import java.nio.Buffer;


public interface AsynchronousData<B extends Buffer> extends Data<B> {
	void waitForRead();
	void waitForWrite();
}

