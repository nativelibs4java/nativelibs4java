/*
 * Context.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.nativelibs4java.scalacl

import com.nativelibs4java.opencl.OpenCL4Java._

class Context(var clContext: CLContext) {

}

object Context {
	def GPU = new Context(CLContext.createContext(CLDevice.listGPUDevices()));
	def CPU = new Context(CLContext.createContext(CLDevice.listCPUDevices()));
	def BEST =
		try {
			GPU
		} catch {
			case _ => CPU
		}
}