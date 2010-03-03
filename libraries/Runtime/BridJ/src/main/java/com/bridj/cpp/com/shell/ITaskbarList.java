/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj.cpp.com.shell;

import com.bridj.Pointer;
import com.bridj.ann.Virtual;
import com.bridj.cpp.com.IUnknown;

/**
 *
 * @author Olivier
 */
public class ITaskbarList extends IUnknown {

	@Virtual(0) public native void HrInit();
	@Virtual(1) public native void AddTab(Pointer<Integer> hWnd);
	@Virtual(2) public native void DeleteTab(Pointer<Integer> hWnd);
	@Virtual(3) public native void ActivateTab(Pointer<Integer> hWnd);
	@Virtual(4) public native void SetActiveAlt(Pointer<Integer> hWnd);
}