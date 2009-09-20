/*
 * Functions.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package scalacl

import com.nativelibs4java.opencl.OpenCL4Java._

//case class Range(min: Int, max: Int)
trait Functions {
  //implicit def Dim2Int(v: Dim) = v.size

  private def f1d(name: String) = (x: Expr) => Fun(name, DoubleType, x)
  private def f2d(name: String) = (x: Expr, y: Expr) => Fun(name, DoubleType, x, y)
  private def f3d(name: String) = (x: Expr, y: Expr, z: Expr) => Fun(name, DoubleType, x, y, z)

  var cos = f1d("cos")
  var sin = f1d("sin")
  var tan = f1d("tan")
  var atan = f1d("atan")
  var acos = f1d("acos")
  var asin = f1d("asin")
  var atanh = f1d("atanh")
  var acosh = f1d("acosh")
  var asinh = f1d("asinh")
  var atanpi = f1d("atanpi")
  var acospi = f1d("acospi")
  var asinpi = f1d("asinpi")
  var cosh = f1d("cosh")
  var sinh = f1d("sinh")
  var tanh = f1d("tanh")
  var atan2 = f2d("atan2")
  var atan2pi = f1d("atan2pi")

  /// Cube root
  var cbrt = f1d("cbrt")

  /// Square root
  var sqrt = f1d("sqrt")


  /// |x|
  var abs = f1d("abs")

  /// | x - y | without modulo overflow
  var abs_diff = f2d("abs_diff")

  /// x + y and saturates the result
  var add_sat = f2d("add_sat")

  /// (x + y) >> 1 without modulo overflow
  var hadd = f2d("hadd")

  /// (x + y + 1) >> 1
  var rhadd = f2d("rhadd")

  /// Number of leading 0-bits in x
  var clz = f1d("clz")

  ///mul_hi(a, b) + c
  var mad_hi = f3d("mad_hi")

  /// (Optional) Multiply 24-bit integer values a and b and add the 32-bit integer result to 32-bit integer c
  var mad24 = f3d("mad24")

  /// a * b + c and saturates the result
  var mad_sat = f3d("mad_sat")

  /// y if x < y, otherwise it returns x
  var max = f2d("max")

  /// y if y < x, otherwise it returns x
  var min = f2d("min")

  /// high half of the product of x and y
  var mul_hi = f2d("mul_hi")

  /// (Optional) Multiply 24-bit integer values a and b
  var mul24 = f2d("mul24")

  /// result[indx] = v[indx] << i[indx]
  var rotate = f2d("rotate")

  /// x - y and saturates the result
  var sub_sat = f2d("sub_sat")

  /**
  	ulongnn upsample (uintn hi, uintn lo)
	result[i]= ((short)hi[i]<< 8)|lo[i] result[i]=((ushort)hi[i]<< 8)|lo[i] result[i]=((int)hi[i]<< 16)|lo[i] result[i]=((uint)hi[i]<< 16)|lo[i] result[i]=((long)hi[i]<< 32)|lo[i] result[i]=((ulong)hi[i]<< 32)|lo[i]
  */
  var upsample = f2d("upsample")

  // Clamp x to range given by min, max
  var clamp = f2d("clamp")

  /// radians to degrees
  var degrees = f1d("degrees")

  /// degrees to radians
  var radians = f1d("radians")

  /// Linear blend ofxandy
  var mix = f2d("mix")

  /// 0.0 if x < edge, else 1.0
  var step = f2d("step")
  /// Step and interpolate

  /// Step and interpolate
  var smoothstep = f2d("smoothstep")

  /// Sign of x
  var sign = f1d("sign")

}
