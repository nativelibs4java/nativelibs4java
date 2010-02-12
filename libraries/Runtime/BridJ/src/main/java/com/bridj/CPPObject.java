/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj;


/**
 *
 * @author Olivier
 */
public class CPPObject {
	protected Pointer<?> $this;
	public CPPObject(Pointer<?> $this) {
		this.$this = $this;
		BridJ.register(getClass());
	}
	//protected CPPObjectIO $io;
	//public CPPObject(CPPObjectIO $io, long $this) {
	//	this.$io = $io;
	//	this.$this = $this;
	//	$io.use();
	//}
}
