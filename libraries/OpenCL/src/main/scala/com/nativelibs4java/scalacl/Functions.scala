/*
 * Functions.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.nativelibs4java.scalacl


object Functions {
	def cos	(x: Expr) = Fun("cos", DoubleType, List(x), "math.h")
	def sin	(x: Expr) = Fun("sin", DoubleType, List(x), "math.h")
	def tan	(x: Expr) = Fun("tan", DoubleType, List(x), "math.h")
	def atan(x: Expr) = Fun("atan", DoubleType, List(x), "math.h")
	def acos(x: Expr) = Fun("acos", DoubleType, List(x), "math.h")
	def asin(x: Expr) = Fun("asin", DoubleType, List(x), "math.h")
	def cosh(x: Expr) = Fun("cosh", DoubleType, List(x), "math.h")
	def sinh(x: Expr) = Fun("sinh", DoubleType, List(x), "math.h")
	def tanh(x: Expr) = Fun("tanh", DoubleType, List(x), "math.h")
	def atan2(x: Expr, y: Expr) = Fun("atan2", DoubleType, List(x, y), "math.h");

	//def implode[T](elts: List[T], sep: String) = elts map (x => x.toString()) reduceLeft ((a, b) => a + sep + b)
}
