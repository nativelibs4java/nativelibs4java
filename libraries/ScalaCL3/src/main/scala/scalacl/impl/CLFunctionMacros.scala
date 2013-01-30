package scalacl.impl
import scalacl.CLArray
import scalacl.CLFilteredArray

import language.experimental.macros
import scala.reflect.macros.Context

private[impl] object CLFunctionMacros 
{
  private val random = new java.util.Random(System.currentTimeMillis)
  
  /// These ids are not necessarily unique, but their values should be dispersed well
  private def nextKernelId = random.nextLong
  
  private[impl] def convertFunction[T: c.WeakTypeTag, U: c.WeakTypeTag](c: Context)(f: c.Expr[T => U]): c.Expr[CLFunction[T, U]] = {
    import c.universe._
    
    val Function(List(param), body) = c.typeCheck(f.tree)
    
    val outSymbol = c.enclosingMethod.symbol.newTermSymbol(newTermName(c.fresh("out")))
    
    val conversion = new CodeConversion {
	    override val u = c.universe
	    val code = convertCode(
	      Assign(Ident(outSymbol), body).asInstanceOf[u.Tree],
        Seq(
          ParamDesc(
            param.symbol.asInstanceOf[u.Symbol],
            implicitly[c.WeakTypeTag[T]].tpe.asInstanceOf[u.Type],
            ParamModeReadArray, 
            Some(0)),
          ParamDesc(
            outSymbol.asInstanceOf[u.Symbol], 
            implicitly[c.WeakTypeTag[U]].tpe.asInstanceOf[u.Type],
            ParamModeWriteArray, 
            Some(0))
        ),
        s => c.fresh(s)
      )
    }
    val code = conversion.code
	  
    val src = c.Expr[String](Literal(Constant(code)))
    val id = c.Expr[Long](Literal(Constant(nextKernelId)))
    c.universe.reify {
      // TODO: add captured vars here.
      new CLFunction[T, U](f.splice, new Kernel(id.splice, src.splice))
    }
  }
}