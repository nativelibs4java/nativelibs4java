/*
 * scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package scalacl


trait Functions {

  def die1 = (_: Double) => throw new RuntimeException("Not implemented")
  def die2 = (_: Double, _: Double) => throw new RuntimeException("Not implemented")
  def die3 = (_: Double, _: Double, _: Double) => throw new RuntimeException("Not implemented")

  private def f1d(name: String, f: Double => Double) =
    (x: Expr) => Fun1(name, DoubleType, x, f)
  private def f2d(name: String, f: (Double, Double) => Double) =
    (x: Expr, y: Expr) => Fun2(name, DoubleType, x, y, f)
  private def f3d(name: String, f: (Double, Double, Double) => Double) =
    (x: Expr, y: Expr, z: Expr) => Fun3(name, DoubleType, x, y, z, f)

  var cos = f1d("cos", Math.cos)
  var sin = f1d("sin", Math.sin)
  var tan = f1d("tan", Math.tan)
  var atan = f1d("atan", Math.atan)
  var acos = f1d("acos", Math.acos)
  var asin = f1d("asin", Math.asin)
  var atanh = f1d("atanh", die1)
  var acosh = f1d("acosh", die1)
  var asinh = f1d("asinh", die1)
  var atanpi = f1d("atanpi", Math.atan(_) / Math.Pi)
  var acospi = f1d("acospi", Math.acos(_) / Math.Pi)
  var asinpi = f1d("asinpi", Math.asin(_) / Math.Pi)
  var cosh = f1d("cosh", die1)
  var sinh = f1d("sinh", die1)
  var tanh = f1d("tanh", die1)
  var atan2 = f2d("atan2", Math.atan2)
  var atan2pi = f2d("atan2pi", Math.atan2(_, _) / Math.Pi)

  /// Cube root
  var cbrt = f1d("cbrt", x => Math.exp(x * Math.log(1/3.0)))

  /// Square root
  var sqrt = f1d("sqrt", Math.sqrt)


  /// |x|
  var abs = f1d("abs", Math.abs)

  /// | x - y | without modulo overflow
  var abs_diff = f2d("abs_diff", (x, y) => Math.abs(x - y))

  /// x + y and saturates the result
  var add_sat = f2d("add_sat", die2)

  /// (x + y) >> 1 without modulo overflow
  var hadd = f2d("hadd", die2)

  /// (x + y + 1) >> 1
  var rhadd = f2d("rhadd", die2)

  /// Number of leading 0-bits in x
  var clz = f1d("clz", die1)

  ///mul_hi(a, b) + c
  var mad_hi = f3d("mad_hi", die3)

  /// (Optional) Multiply 24-bit integer values a and b and add the 32-bit integer result to 32-bit integer c
  var mad24 = f3d("mad24", die3)

  /// a * b + c and saturates the result
  var mad_sat = f3d("mad_sat", die3)

  /// y if x < y, otherwise it returns x
  var max = f2d("max", Math.max)

  /// y if y < x, otherwise it returns x
  var min = f2d("min", Math.min)

  /// high half of the product of x and y
  var mul_hi = f2d("mul_hi", die2)

  /// (Optional) Multiply 24-bit integer values a and b
  var mul24 = f2d("mul24", die2)

  /// result[indx] = v[indx] << i[indx]
  var rotate = f2d("rotate", die2)

  /// x - y and saturates the result
  var sub_sat = f2d("sub_sat", die2)

  /**
  	ulongnn upsample (uintn hi, uintn lo)
	result[i]= ((short)hi[i]<< 8)|lo[i] result[i]=((ushort)hi[i]<< 8)|lo[i] result[i]=((int)hi[i]<< 16)|lo[i] result[i]=((uint)hi[i]<< 16)|lo[i] result[i]=((long)hi[i]<< 32)|lo[i] result[i]=((ulong)hi[i]<< 32)|lo[i]
  */
  var upsample = f2d("upsample", die2)

  // Clamp x to range given by min, max
  var clamp = f2d("clamp", die2)

  /// radians to degrees
  var degrees = f1d("degrees", die1)

  /// degrees to radians
  var radians = f1d("radians", die1)

  /// Linear blend ofxandy
  var mix = f2d("mix", die2)

  /// 0.0 if x < edge, else 1.0
  var step = f2d("step", (x, y) => if (x < y) 0.0 else 1.0)
  /// Step and interpolate

  /// Step and interpolate
  var smoothstep = f2d("smoothstep", die2)

  /// Sign of x
  var sign = f1d("sign", Math.signum)

}
