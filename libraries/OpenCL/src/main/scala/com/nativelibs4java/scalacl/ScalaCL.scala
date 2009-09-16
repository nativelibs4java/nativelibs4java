/*
 * ScalaCL2.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.nativelibs4java.scalacl
import com.nativelibs4java.opencl.OpenCL4Java._

//case class Dim(size: Int) extends Int1(size)

object ScalaCL {
  implicit def Int2Int1(v: Int) = Int1(v)
  implicit def Int12Int(v: Int1) = v.value
  implicit def Double2Double1(v: Double) = Double1(v)
  implicit def Double12Double(v: Double1) = v.value
}