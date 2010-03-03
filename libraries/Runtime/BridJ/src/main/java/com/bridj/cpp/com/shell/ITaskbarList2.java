/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj.cpp.com.shell;

import com.bridj.Pointer;
import com.bridj.ann.Virtual;

/**
 *
 * @author Olivier
 */
public class ITaskbarList2  extends ITaskbarList {
	@Virtual(0) public native int MarkFullscreenWindow(Pointer<Integer> hWnd, boolean fFullscreen);
}