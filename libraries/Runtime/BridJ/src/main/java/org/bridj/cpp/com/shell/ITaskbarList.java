/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bridj.cpp.com.shell;

import org.bridj.Pointer;
import org.bridj.ann.Virtual;
import org.bridj.cpp.com.IUnknown;

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