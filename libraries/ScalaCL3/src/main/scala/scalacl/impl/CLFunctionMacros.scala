package scalacl
package impl

import scala.reflect.makro.Context
import language.experimental.macros

private[impl] object CLFunctionMacros 
{
  private val random = new java.util.Random(System.currentTimeMillis)
  
  /// These ids are not necessarily unique, but their values should be dispersed well
  private def nextKernelId = random.nextLong
  
  private[impl] def convertFunction[T, U](c: Context)(f: c.Expr[T => U]): c.Expr[CLFunction[T, U]] = {
    import c.universe._
    
    val Function(List(param), body) = f.tree
    
    val outName = newTermName(c.fresh("out"))
    
    val code = CodeConversion.convertCode(c)(
	    Assign(Ident(outName), body),
	    dimensionallyIteratedInput = Some(param.symbol),
	    dimensionallyIteratedOutput = Some(outName),
	    dimensionRangeIndexes = None,
	    dimensionRangeOffsets = None,
	    dimensionRangeSteps = None,
	    capturedInputs = Seq(),
	    capturedOutputs = Seq(),
	    capturedConstants = Seq()
    )
    val src = c.Expr(Literal(Constant(code)))
    val id = c.Expr[Long](Literal(Constant(nextKernelId)))
    c.reify {
      new CLFunction[T, U](f.splice, new Kernel(id.splice, src.splice))
    }
  }
}