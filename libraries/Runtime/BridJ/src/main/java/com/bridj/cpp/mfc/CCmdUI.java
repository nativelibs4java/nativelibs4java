/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj.cpp.mfc;

import com.bridj.ann.Virtual;


public class CCmdUI extends MFCObject {

	@Virtual
	public native void Enable(boolean bOn);
}