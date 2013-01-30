package scalacl.impl
import scalacl.CLArray
import scalacl.CLFilteredArray

//import language.experimental.macros
//import scala.reflect.macros.Context

import scala.reflect.api.Universe

trait CodeConversion {
  val u: Universe
  
  case class ParamDesc(
    symbol: u.Symbol,
    tpe: u.Type,
    mode: ParamKind,
    usage: UsageKind,
    implicitIndexDimension: Option[Int] = None,
    rangeOffset: Option[u.Symbol] = None,
    rangeStep: Option[u.Symbol] = None
  ) {
    assert((mode == ParamKind.ImplicitArrayElement || mode == ParamKind.RangeIndex) == (implicitIndexDimension != None))
    def isArray = 
      mode == ParamKind.ImplicitArrayElement || mode == ParamKind.Normal && tpe <:< u.typeOf[CLArray[_]]
  }
  
  /*
  ParamDesc(i, ParamKindRangeIndex, Some(0))
    -> get_global_id(0)
  ParamDesc(i, ParamKindRangeIndex, Some(0), Some(from), Some(by))
    -> (from + get_global_id(0) * by)
  ParamDesc(x, ParamKindRead)
    -> x
  ParamDesc(x, ParamKindRead, Some(0))
    -> x[get_global_id(0)]
  */
  def convertCode(code: u.Tree, explicitParamDescs: Seq[ParamDesc], fresh: String => String): (String, Seq[ParamDesc]) = {
    val converter = new OpenCLConverter {
	    override val global = u
	  }
	  
	  val tree = code.asInstanceOf[converter.global.Tree]
	  
	  val externalSymbols =
	    converter.getExternalSymbols(
	      tree, 
	      knownSymbols = explicitParamDescs.map(_.symbol.asInstanceOf[converter.global.Symbol]).toSet
	    )

	  val capturedParamDescs: Seq[ParamDesc] = {
	    for (sym <- externalSymbols.capturedSymbols) yield {
	      val tpe = externalSymbols.symbolTypes(sym)
	      val kind = externalSymbols.getKind(sym, tpe)
	      val usage = externalSymbols.symbolUsages(sym)
	      /*println(s"""sym = $sym, tpe = $tpe, kind = $kind, usage = $usage""")
	      val mode = (kind, usage) match {
	        case (SymbolKind.ArrayLike, UsageKind.Input) => ParamKind.ReadArray
	        case (SymbolKind.ArrayLike, UsageKind.Output) => ParamKind.WriteArray
	        case (SymbolKind.ArrayLike, UsageKind.InputOutput) => ParamKind.ReadWriteArray
	        case (SymbolKind.Scalar, UsageKind.Input) => ParamKind.Scalar
	      }*/
        ParamDesc(
          sym.asInstanceOf[u.Symbol],
          tpe.asInstanceOf[u.Type],
          mode = ParamKind.Normal,
          usage = usage)
	    }
	  }
	  val paramDescs = explicitParamDescs ++ capturedParamDescs
	  val flat = converter.convert(tree)
	  
	  val globalIDIndexes =
	    paramDescs.flatMap(_.implicitIndexDimension).toSet
	  
	  val globalIDValNames: Map[Int, String] =
	    globalIDIndexes.map(i => i -> fresh("_global_id_" + i + "_")).toMap
	  
	  val replacements: Seq[String => String] = paramDescs.map(paramDesc => {
      val r = ("\\b(" + paramDesc.symbol.name + ")\\b").r
      // TODO handle composite types, with replacements of all possible fibers (x._1, x._2._1, x._2._2)
	    paramDesc match {
        case ParamDesc(_, _, ParamKind.ImplicitArrayElement, _, Some(i), None, None) =>
          (s: String) => r.replaceAllIn(s, "$1[" + globalIDValNames(i) + "]")
        case ParamDesc(_, _, ParamKind.RangeIndex, _, Some(i), Some(from), Some(by)) =>
          (s: String) => r.replaceAllIn(s, "(" + from + " + " + globalIDValNames(i) + " * " + by + ")")
        case _ =>
          (s: String) => s
      }
    })
    
    val globalIDStatements = globalIDValNames.toSeq.map { case (i, n) =>
      s"size_t $n = get_global_id($i);"
    }
    
	  val result = 
      (FlatCode[String](statements = globalIDStatements) ++ flat).mapEachValue(s => Seq(
        replacements.foldLeft(s)((v, f) => f(v))))
	  
    val params: Seq[String] = paramDescs.map(paramDesc => {
      // TODO handle composite types, with fresh names for each fiber (x_1, x_2_1, x_2_2)
	    val t = converter.convertTpe(paramDesc.tpe.asInstanceOf[converter.global.Type])

	    (if (paramDesc.isArray) "global " else "") +
	    (if (paramDesc.usage == UsageKind.Input) "const " else "") +
	    t +
	    (if (paramDesc.mode == ParamKind.ImplicitArrayElement) " *" else " ") +
	    paramDesc.symbol.name
    })
    
    val convertedCode =
      s"""
        /*
        code: $code
        externalSymbols: $externalSymbols
        capturedSymbols: ${externalSymbols.capturedSymbols}
        paramDescs: $paramDescs
        globalIDIndexes: $globalIDIndexes
        result: $result
        params: $params
        */
      """ +
      result.outerDefinitions.mkString("\n") +
      "kernel void f(" + params.mkString(", ") + ") {\n\t" +
        (result.statements ++ result.values).mkString("\n\t") + "\n" +
      "}"
    (convertedCode, capturedParamDescs)
    //externalSymbols.capturedSymbols.map(_.asInstanceOf[u.Symbol]))
	}
	
}
