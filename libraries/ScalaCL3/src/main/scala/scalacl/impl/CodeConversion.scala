package scalacl.impl
import scalacl.CLArray
import scalacl.CLFilteredArray

//import language.experimental.macros
//import scala.reflect.macros.Context

import scala.reflect.api.Universe

sealed trait ParamMode {
  def isArray: Boolean = false
}
object ParamMode {
  case object ReadArray extends ParamMode {
    override def isArray = true
  }
  case object ReadWriteArray extends ParamMode {
    override def isArray = true
  }
  case object WriteArray extends ParamMode {
    override def isArray = true
  }
  case object Scalar extends ParamMode
  case object RangeIndex extends ParamMode
}

trait CodeConversion {
  val u: Universe
  
  case class ParamDesc(
    symbol: u.Symbol,
    tpe: u.Type,
    mode: ParamMode,
    implicitIndexDimension: Option[Int] = None,
    rangeOffset: Option[u.Symbol] = None,
    rangeStep: Option[u.Symbol] = None
  )
  
  /*
  ParamDesc(i, ParamModeRangeIndex, Some(0))
    -> get_global_id(0)
  ParamDesc(i, ParamModeRangeIndex, Some(0), Some(from), Some(by))
    -> (from + get_global_id(0) * by)
  ParamDesc(x, ParamModeRead)
    -> x
  ParamDesc(x, ParamModeRead, Some(0))
    -> x[get_global_id(0)]
  */
  def convertCode(code: u.Tree, explicitParamDescs: Seq[ParamDesc], fresh: String => String): (String, Seq[u.Symbol]) = {
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
	      val mode = (kind, usage) match {
	        case (SymbolKind.ArrayLike, UsageKind.Input) => ParamMode.ReadArray
	        case (SymbolKind.ArrayLike, UsageKind.Output) => ParamMode.WriteArray
	        case (SymbolKind.ArrayLike, UsageKind.InputOutput) => ParamMode.ReadWriteArray
	        case (SymbolKind.Scalar, UsageKind.Input) => ParamMode.Scalar
	      }
        ParamDesc(
          sym.asInstanceOf[u.Symbol],
          tpe.asInstanceOf[u.Type],
          mode)
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
        case ParamDesc(_, _, ParamMode.ReadArray | ParamMode.WriteArray, Some(i), None, None) =>
          (s: String) => r.replaceAllIn(s, "$1[" + globalIDValNames(i) + "]")
        case ParamDesc(_, _, ParamMode.RangeIndex, Some(i), Some(from), Some(by)) =>
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

	    paramDesc.implicitIndexDimension.map(_ => "global ").getOrElse("") +
	    (if (paramDesc.mode == ParamMode.ReadArray) "const " else "") +
	    t +
	    (if (paramDesc.mode.isArray) " *" else " ") +
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
    (convertedCode, externalSymbols.capturedSymbols.map(_.asInstanceOf[u.Symbol]))
	}
	
}
