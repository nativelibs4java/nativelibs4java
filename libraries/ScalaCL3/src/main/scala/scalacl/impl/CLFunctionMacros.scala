package scalacl.impl
import scalacl.CLArray
import scalacl.CLFilteredArray

import language.experimental.macros
import scala.reflect.macros.Context

private[impl] object CLFunctionMacros 
{
  def cast[A, B](a: A): B = a.asInstanceOf[B]
  
  private val random = new java.util.Random(System.currentTimeMillis)
  
  /// These ids are not necessarily unique, but their values should be dispersed well
  private def nextKernelId = random.nextLong
  
  private[impl] def convertFunction[T: c.WeakTypeTag, U: c.WeakTypeTag](c: Context)(f: c.Expr[T => U]): c.Expr[CLFunction[T, U]] = {
    import c.universe._
    import definitions._
    
    val Function(List(param), body) = c.typeCheck(f.tree)
    
    val outSymbol = c.enclosingMethod.symbol.newTermSymbol(newTermName(c.fresh("out")))
    
    val inputTpe = implicitly[c.WeakTypeTag[T]].tpe
    val outputTpe = implicitly[c.WeakTypeTag[U]].tpe
    
    val conversion = new CodeConversion {
	    override val u = c.universe
	    val (code, capturedSymbols) = convertCode(
	      Assign(Ident(outSymbol).setType(outputTpe), body).asInstanceOf[u.Tree],
        Seq(
          ParamDesc(
            param.symbol.asInstanceOf[u.Symbol],
            inputTpe.asInstanceOf[u.Type],
            ParamMode.ReadArray, 
            Some(0)),
          ParamDesc(
            outSymbol.asInstanceOf[u.Symbol], 
            outputTpe.asInstanceOf[u.Type],
            ParamMode.WriteArray, 
            Some(0))
        ),
        s => c.fresh(s)
      )
    }
    val code = conversion.code
	  
    val src = c.Expr[String](Literal(Constant(code)))
    val id = c.Expr[Long](Literal(Constant(nextKernelId)))
    
    //val captures = 
    //  Apply(
    //    Select(Ident(newTermName("scalacl")), newTypeName("Captures"))
    val constants = c.Expr[Array[AnyRef]](
      Apply(
        TypeApply(
          Select(Ident(ArrayModule), newTermName("apply")),
          List(TypeTree(typeOf[AnyRef]))
        ),
        conversion.capturedSymbols.toList.map(s => {
          
          val x = c.Expr[Array[AnyRef]](Ident(s.asInstanceOf[Symbol]))
          (c.universe.reify {
            x.splice.asInstanceOf[AnyRef]
          }).tree
          //Ident(s.asInstanceOf[Symbol]))
        })
      )
    )
    val res = c.universe.reify {
      // TODO: add captured vars here.
      new CLFunction[T, U](
        f.splice, 
        new Kernel(id.splice, src.splice),
        Captures(constants = constants.splice)
      )
    }
    println(res)
    res
  }
}