/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.runtime;

import com.nativelibs4java.runtime.Addressable;

/**
 *
 * @author Olivier Chafik
 */
public abstract class Callback implements Addressable {
	long callback;
	@Override
	public long getAddress() {
		return callback;
	}
        @Override
	public void setAddress(long address) {
		callback = address;
	}
}