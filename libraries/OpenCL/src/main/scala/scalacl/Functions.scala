/*
 * scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package scalacl


trait Functions {
  
  case class Fun2WithPtrAsSecond(na: String, outTy: PrimType, ar1: Expr, ar2: Expr, ff: (Double, Double) => Double)
    extends Fun2(na, outTy, ar1, ar2, ff)
  {
    override def toString = name + "(" + arg1 + ", &" + arg2 + ")"
  }

  def die1 = (_: Double) => throw new RuntimeException("Not implemented")
  def die2 = (_: Double, _: Double) => throw new RuntimeException("Not implemented")
  def die3 = (_: Double, _: Double, _: Double) => throw new RuntimeException("Not implemented")

  private def f1d(name: String, f: Double => Double) =
    (x: Expr) => Fun1(name, DoubleType, x, f)
  private def f2d(name: String, f: (Double, Double) => Double) =
    (x: Expr, y: Expr) => Fun2(name, DoubleType, x, y, f)
  private def f2dWithPtrAsSecond(name: String) =
    (x: Expr, y: Expr) => Fun2WithPtrAsSecond(name, DoubleType, x, y, die2)
  private def f3d(name: String, f: (Double, Double, Double) => Double) =
    (x: Expr, y: Expr, z: Expr) => Fun3(name, DoubleType, x, y, z, f)

  var sin = f1d("sin", Math.sin)
  var cos = f1d("cos", Math.cos)
  var tan = f1d("tan", Math.tan)

  var asin = f1d("asin", Math.asin)
  var acos = f1d("acos", Math.acos)
  var atan = f1d("atan", Math.atan)
  var atan2 = f2d("atan2", Math.atan2)

  var sinpi = f1d("sinpi", x => Math.sin(x * Math.Pi))
  var cospi = f1d("cospi", x => Math.cos(x * Math.Pi))
  var tanpi = f1d("tanpi", x => Math.tan(x * Math.Pi))

  var asinpi = f1d("asinpi", Math.asin(_) / Math.Pi)
  var acospi = f1d("acospi", Math.acos(_) / Math.Pi)
  var atanpi = f1d("atanpi", Math.atan(_) / Math.Pi)
  var atan2pi = f2d("atan2pi", Math.atan2(_, _) / Math.Pi)

  var sinh = f1d("sinh", java.lang.Math.sinh)
  var cosh = f1d("cosh", java.lang.Math.cosh)
  var tanh = f1d("tanh", java.lang.Math.tanh)

  var asinh = f1d("asinh", die1)
  var acosh = f1d("acosh", die1)
  var atanh = f1d("atanh", die1)
  
  var log = f1d("log", Math.log)
  var logb = f1d("logb", die1)
  var log2 = f1d("log2", Math.log(_) / Math.log(2))
  var log10 = f1d("log10", java.lang.Math.log10)
  var log1p = f1d("log1p", java.lang.Math.log1p)
  var exp = f1d("exp", Math.exp)
  var pow = f2d("pow", Math.pow)
  var pown = f2d("pown", Math.pow)
  var powr = f2d("powr", Math.pow)
  var root = f2d("root", (x, y) => Math.exp(x * Math.log(1 / y)))

  /// Cube root
  var cbrt = f1d("cbrt", java.lang.Math.cbrt)
  /// Square root
  var sqrt = f1d("sqrt", Math.sqrt)
  /// Inverse square root
  var rsqrt = f1d("rsqrt", 1 / Math.sqrt(_))
  /// Gamma function
  var tgamma = f1d("tgamma", die1)

  var remainder = f2d("remainder", Math.IEEEremainder)

  
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

  /// mul_hi(a, b) + c
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

  /// Error function
  var erf = f1d("erf", die1)
  /// Complementary error function
  var erfc = f1d("erfc", die1)


  /// Round to integer towards +infinity
  var ceil = f1d("ceil", Math.ceil)

  /// x with sign changed to sign of y
  var copysign = f2d("copysign", (x, y) => Math.abs(x) * Math.signum(y))

  var floor = f1d("floor", Math.floor)
  var trunc = f1d("trunc", x => if (x >= 0) x.asInstanceOf[Int] else -((-x).asInstanceOf[Int]))

  var fma = f3d("fma", die3)
  var fabs = f1d("abs", die1)
  var ilogb = f1d("ilogb", die1)
  var ldexp = f2d("ldexp", (x, n) => x * Math.pow(2, n))
  var hypot = f2d("hypot", (x, y) => Math.sqrt(x * x + y * y))

  var mad = f3d("mad", (a, b, c) => a * b + c)
  var round = f1d("round", x => Math.round(x))
  var rint = f1d("rint", die1)

  var sincos = f2dWithPtrAsSecond("sincos")
  var fract = f2dWithPtrAsSecond("fract")
  var frexp = f2dWithPtrAsSecond("frexp")
  var modf = f2dWithPtrAsSecond("modf")
  var lgamma_r = f2dWithPtrAsSecond("lgamma_r")

  var normalize = f2d("normalize", die2)
  var cross = f2d("cross", die2)
  var dot = f2d("dot", die2)
  var distance = f2d("distance", die2)
  var length = f2d("length", die2)
  var fast_distance = f2d("fast_distance", die2)
  var fast_length = f2d("fast_length", die2)
  var fast_normalize = f2d("fast_normalize", die2)

}
