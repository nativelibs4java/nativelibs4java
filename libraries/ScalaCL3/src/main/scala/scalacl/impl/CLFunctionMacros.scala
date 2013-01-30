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
	    val (code, capturedParamDescs) = convertCode(
	      Assign(Ident(outSymbol).setType(outputTpe), body).asInstanceOf[u.Tree],
        Seq(
          ParamDesc(
            symbol = param.symbol.asInstanceOf[u.Symbol],
            tpe = inputTpe.asInstanceOf[u.Type],
            mode = ParamKind.ImplicitArrayElement,
            usage = UsageKind.Input,
            implicitIndexDimension = Some(0)),
          ParamDesc(
            symbol = outSymbol.asInstanceOf[u.Symbol], 
            tpe = outputTpe.asInstanceOf[u.Type],
            mode = ParamKind.ImplicitArrayElement,
            usage = UsageKind.Output,
            implicitIndexDimension = Some(0))
        ),
        s => c.fresh(s)
      )
    }
    val code = conversion.code
	  
    val src = c.Expr[String](Literal(Constant(code)))
    val id = c.Expr[Long](Literal(Constant(nextKernelId)))
    
    def arrayApply[A: TypeTag](values: List[Tree]): c.Expr[Array[A]] = {
      c.Expr[Array[A]](
        Apply(
          TypeApply(
            Select(Ident(ArrayModule), newTermName("apply")),
            List(TypeTree(typeOf[A]))
          ),
          values
        )
      )
    }
    val inputs = arrayApply[CLArray[_]](
      conversion.capturedParamDescs
        .filter(d => d.isArray && d.usage.isInput)
        .map(d => Ident(d.symbol.asInstanceOf[Symbol])).toList
    )
    val outputs = arrayApply[CLArray[_]](
      conversion.capturedParamDescs
        .filter(d => d.isArray && d.usage.isOutput)
        .map(d => Ident(d.symbol.asInstanceOf[Symbol])).toList
    )
    val constants = arrayApply[AnyRef](
      conversion.capturedParamDescs
        .filter(!_.isArray)
        .map(d => {
          val x = c.Expr[Array[AnyRef]](Ident(d.symbol.asInstanceOf[Symbol]))
          (c.universe.reify {
            x.splice.asInstanceOf[AnyRef]
          }).tree
        }).toList
    )
    c.universe.reify {
      new CLFunction[T, U](
        f.splice, 
        new Kernel(id.splice, src.splice),
        Captures(
          inputs = inputs.splice, 
          outputs = outputs.splice, 
          constants = constants.splice)
      )
    }
  }
}