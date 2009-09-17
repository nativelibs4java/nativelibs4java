/*
 * ScalaCL2.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package scalacl
import com.nativelibs4java.opencl.OpenCL4Java._

//case class Dim(size: Int) extends Int1(size)


object ScalaCL {
//  implicit def Int2Dim(v: Int1) = Dim(v)
  implicit def Int2Int1(v: Int) = Int1(v)
  implicit def Int12Int(v: Int1) = v.value
  implicit def Double2Double1(v: Double) = Double1(v)
  implicit def Double12Double(v: Double1) = v.value

  def cos(x: Expr) = Fun("cos", DoubleType, List(x), "math.h")
  def sin(x: Expr) = Fun("sin", DoubleType, List(x), "math.h")
  def tan(x: Expr) = Fun("tan", DoubleType, List(x), "math.h")
  def atan(x: Expr) = Fun("atan", DoubleType, List(x), "math.h")
  def acos(x: Expr) = Fun("acos", DoubleType, List(x), "math.h")
  def asin(x: Expr) = Fun("asin", DoubleType, List(x), "math.h")
  def cosh(x: Expr) = Fun("cosh", DoubleType, List(x), "math.h")
  def sinh(x: Expr) = Fun("sinh", DoubleType, List(x), "math.h")
  def tanh(x: Expr) = Fun("tanh", DoubleType, List(x), "math.h")
  def atan2(x: Expr, y: Expr) = Fun("atan2", DoubleType, List(x, y), "math.h");

}