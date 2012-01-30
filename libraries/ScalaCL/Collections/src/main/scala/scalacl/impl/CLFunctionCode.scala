package scalacl

package impl

import scala.collection._
//import com.nativelibs4java.opencl._


//import CLFunction._
case class CapturedIOs(
  inputBuffers: Array[CLDataIO[Any]] = Array(),
  outputBuffers: Array[CLDataIO[Any]] = Array(),
  scalars: Array[CLDataIO[Any]] = Array()
) {
  lazy val isEmpty = 
    inputBuffers.isEmpty && outputBuffers.isEmpty && scalars.isEmpty 
}

case class SourceData(
  functionName: String,
  functionSource: String,
  kernelsSource: String,
  includedSources: Array[String],
  outerDeclarations: Seq[String]
)

object CLFunctionCode {
  private var nextuid = 1
  protected def newuid = this.synchronized {
    val uid = nextuid
    nextuid += 1
    uid
  }
  def clType[T](implicit dataIO: CLDataIO[T]) = dataIO.clType

  val compositions = new mutable.HashMap[(Long, Long), CLFunctionCode[_, _]]
  val ands = new mutable.HashMap[(Long, Long), CLFunctionCode[_, _]]
  
  protected val indexVar: String = "$i"
  protected val sizeVar: String = "$size"
  protected val clVarPrefix = "__cl_"
  protected val indexVarName = clVarPrefix + "i"
  protected val sizeVarName = clVarPrefix + "size"
    
  
  sealed trait ArgKind
  object ParallelInputValueArg extends ArgKind
  object ParallelOutputValueArg extends ArgKind
  object InputBufferArg extends ArgKind
  object OutputBufferArg extends ArgKind
  object InputScalarArg extends ArgKind
  
  case class ArgInfo(
    io: CLDataIO[_],
    kind: ArgKind
  )
  trait FiberReplacementContent {
    def apply(parallelIndexesExprs: Seq[String], isParallelInputARange: Boolean): String
  }
  trait ReplacementContent {
    def apply(parallelIndexesExprs: Seq[String]): Seq[(String, List[Int])]
  }
  case class FiberInfo(
    pattern: String,
    tupleIndexes: List[Int],
    replacementContent: FiberReplacementContent
  )
  case class ReplacementInfo(
    nameBasis: String,
    //pattern: Option[String],
    argInfo: ArgInfo,
    //offset: Int,
    kernelParamsDeclarations: Seq[String],
    functionParamsDeclarations: Seq[String],
    fiberInfos: Option[Seq[FiberInfo]],
    replacementContent: ReplacementContent
  )
  
  def getReplacements(argInfos: Seq[ArgInfo]): Seq[ReplacementInfo] = {
    case class ArgOffsets(fiberOffset: Int, extraArgOffset: Int)
    val argsWithOffsets = argInfos.zip(argInfos.scanLeft((0, 0)) { case ((offset, extraArgs), argInfo) =>
      (
        offset + argInfo.io.elementCount,
        extraArgs + (argInfo.kind match {
          case InputBufferArg | OutputBufferArg | InputScalarArg => 1
          case _ => 0
        })
      )
    }).map { case (argInfo, (offset, extraArgs)) => (argInfo, ArgOffsets(offset, extraArgs)) }
    
    argsWithOffsets map {
      case (argInfo, offsets) => 
        import CLDataIO.{ InputPointer, OutputPointer, Value }
        
        val (nameBasis, argTypeForKernel, argTypeForFunction, fiberInfos) = argInfo.kind match {
          case ParallelInputValueArg => 
            val nameBasis = clVarPrefix + "in"
            (nameBasis, InputPointer, Value, Some(argInfo.io.openCLIntermediateKernelTupleElementsExprs("_") map {
              case (expr, tupleIndexes) =>
                FiberInfo(expr, tupleIndexes, new FiberReplacementContent {
                  override def apply(parallelIndexesExprs: Seq[String], isParallelInputARange: Boolean) = {
                    val Seq(i) = parallelIndexesExprs
                    
                    def getRawIthArrayElement(i: String) =
                      argInfo.io.openCLIthTupleElementNthItemExpr(
                        nameBasis,
                        InputPointer, 
                        offsets.fiberOffset, 
                        tupleIndexes, 
                        i
                      )
                    
                    if (isParallelInputARange)
                      "(" + 
                        getRawIthArrayElement("0") + 
                        " + (" + i + ") * " + 
                        getRawIthArrayElement("2") + 
                      ")" // buffer contains (from, to, by, inclusive). Value is (from + i * by)  
                    else
                      getRawIthArrayElement(i)
                  }
                })
            }))
          case ParallelOutputValueArg => 
            (clVarPrefix + "out", OutputPointer, OutputPointer, None)
          case _ =>
            val (nameBasis, argTypeForKernel, argTypeForFunction) = argInfo.kind match {
              case InputBufferArg =>
                (clVarPrefix + "capturedIn", InputPointer, InputPointer)
              case OutputBufferArg =>
                (clVarPrefix + "capturedOut", OutputPointer, OutputPointer)
              case InputScalarArg =>
                (clVarPrefix + "capturedVal", Value, Value)
              case _ =>
                null
            }
            
            def fiberInfos(nameBasis: String) =
              argInfo.io.openCLIntermediateKernelTupleElementsExprs(
                "_" + (offsets.extraArgOffset + 1)
              ) map { case (expr, tupleIndexes) =>
                FiberInfo(expr, tupleIndexes, new FiberReplacementContent {
                  override def apply(parallelIndexesExprs: Seq[String], isParallelInputARange: Boolean) = {
                    //expr
                    argInfo.io.openCLIthTupleElementNthItemExpr(
                      nameBasis,
                      Value, 
                      offsets.fiberOffset, 
                      tupleIndexes, 
                      "0"
                    )
                  }
                })
              }
            
            (nameBasis, argTypeForKernel, argTypeForFunction, Some(fiberInfos(nameBasis)))
        } 
        
        ReplacementInfo(
          nameBasis, 
          argInfo,
          //offsets.fibersOffset,
          kernelParamsDeclarations = 
            argInfo.io.openCLKernelArgDeclarations(nameBasis, argTypeForKernel, offsets.fiberOffset),
          functionParamsDeclarations = 
            argInfo.io.openCLKernelArgDeclarations(nameBasis, argTypeForFunction, offsets.fiberOffset),
          fiberInfos = fiberInfos,
          new ReplacementContent {
            override def apply(parallelIndexesExprs: Seq[String]) = {
              val Seq(i) = parallelIndexesExprs
              argInfo.io.openCLKernelNthItemExprs(nameBasis, CLDataIO.OutputPointer, offsets.fiberOffset, i)
            }
          }
        )
    }
  }
  def toRxb(s: String) = {
    "(^|\\b)" +
    java.util.regex.Matcher.quoteReplacement(s) + 
    "($|\\b)"
  }
  
  def getFibersReplacementInfos(replacementInfos: Seq[ReplacementInfo]): Seq[(ReplacementInfo, FiberInfo)] = {
    replacementInfos.flatMap(ri => ri.fiberInfos.map(_.map(fi => (ri, fi)))).flatten
  }
  def replaceAll(s: String, isParallelInputARange: Boolean, fibersReplacementInfos: Seq[(ReplacementInfo, FiberInfo)], i: String = indexVarName): String = {
    var r = s.replaceAll(toRxb(indexVar), indexVarName)
    r = r.replaceAll(toRxb(sizeVar), sizeVarName)
  
    var sortedInfos: Seq[(ReplacementInfo, FiberInfo)] = 
      fibersReplacementInfos.filter(_._1.argInfo.kind != ParallelOutputValueArg). // don't replace the output
      sortBy({ case (ri, fi) => -fi.pattern.length }) // replace longest patterns first...
   
    for ((replacementInfo, fiberInfo) <- sortedInfos) {
      val x = fiberInfo.replacementContent(Seq(i), isParallelInputARange)
      val expr = fiberInfo.pattern
        
      //println("REPLACING '" + expr + "' by '" + x + "'")
      r = r.replaceAll(toRxb(expr), x)
      //println("\t-> '" + r.replaceAll("\n", "\n\t") + "'")
    }
    
    r
  }
  
  def outputFunction(beforeParams: String, params: Seq[Seq[String]], body: Seq[Seq[String]], out: StringBuilder = new StringBuilder): StringBuilder = {
    //import out.{ append => << }
    out append beforeParams append '('
    var first = true
    for (sub <- params; param <- sub) {
      if (first)
        first = false
      else
        out append ", "
      out append param
    }
    out append ") {\n"
    for (sub <- body; stat <- sub)
      out append '\t' append stat append '\n'
      
    out append "}\n"
    
    out
  }
  
  def buildSourceData[A, B](
    outerDeclarations: Array[String],
    declarations: Array[String],
    expressions: Array[String],
    includedSources: Array[String],
    extraArgsIOs: CapturedIOs = CapturedIOs(),
    bodyPrefix: Array[String] = Array(),
    bodySuffix: Array[String] = Array()
  )(implicit aIO: CLDataIO[A], bIO: CLDataIO[B]): SourceData = 
  {
    assert(!expressions.isEmpty)
    
    val uid = newuid
    val functionName = "f" + uid
    
    val argInfos = 
      Seq(
        ArgInfo(aIO, ParallelInputValueArg), 
        ArgInfo(bIO, ParallelOutputValueArg)
      ) ++
      extraArgsIOs.inputBuffers.map(ArgInfo(_, InputBufferArg)) ++
      extraArgsIOs.outputBuffers.map(ArgInfo(_, OutputBufferArg)) ++
      extraArgsIOs.scalars.map(ArgInfo(_, InputScalarArg))
    
    //println("argInfos = " + argInfos)
    
    val replacementInfos = getReplacements(argInfos)
    val fibersReplacementInfos = getFibersReplacementInfos(replacementInfos)
    //println("replacementInfos = " + replacementInfos)
    
    
    val indexHeader = Seq("int " + indexVarName + " = get_global_id(0);")
    val sizeHeader = Seq("if (" + indexVarName + " >= " + sizeVarName + ") return;")
  
    val outputFibersInfos = replacementInfos.filter(_.argInfo.kind == ParallelOutputValueArg)
    
    def getAssignments(i: String, isRange: Boolean): Seq[String] =
      expressions.zip(outputFibersInfos.flatMap(_.replacementContent(Seq(i)))).map {
        case (expression, (xOut, indexes)) => xOut + " = " + replaceAll(expression, isRange, fibersReplacementInfos, i) + ";"
      }
  
    
    val funDecls = declarations.map(replaceAll(_, false, fibersReplacementInfos, "0"))
    //lazy val funDeclsRange = declarations.map(replaceAll(_, true, replacementInfos, "0"))
    var kernelParams = replacementInfos.flatMap(_.kernelParamsDeclarations)
    var functionParams = replacementInfos.flatMap(_.functionParamsDeclarations)
    
    val functionSource = ""/*outputFunction(
      "inline void " + functionName,
      Seq(functionParams),
      Seq(
        indexHeader,
        funDecls,
        getAssignments("0", false)
      )
    ).toString*/
  
    val presenceName = "__cl_presence"
    val presenceParam = Seq("__global const " + CLFilteredArray.presenceCLType + "* " + presenceName)
    val presenceHeader = Seq("if (!" + presenceName + "[" + indexVarName + "]) return;")
    
    val sizeParam = Seq("int " + sizeVarName)
    
    val kernDeclsArray = declarations.map(replaceAll(_, false, fibersReplacementInfos))
    lazy val kernDeclsRange = declarations.map(replaceAll(_, true, fibersReplacementInfos))
    
    val assigntArray = getAssignments(indexVarName, false)
    lazy val assigntRange = getAssignments(indexVarName, true)
    
    val kernelsSource = new StringBuilder
    for (outerDeclaration <- outerDeclarations)
      kernelsSource.append(outerDeclaration).append('\n')
    
    outputFunction(
      "__kernel void array_array",
      Seq(sizeParam, kernelParams),
      Seq(
        bodyPrefix,
        indexHeader, 
        sizeHeader,
        kernDeclsArray,
        assigntArray,
        bodySuffix
      ),
      kernelsSource
    )
    outputFunction(
      "__kernel void filteredArray_filteredArray",
      Seq(sizeParam, presenceParam, kernelParams), 
      Seq(
        bodyPrefix,
        indexHeader,
        sizeHeader,
        presenceHeader,
        kernDeclsArray,
        assigntArray,
        bodySuffix
      ),
      kernelsSource
    )
    
    if (aIO.t.erasure == classOf[Int]) {// || aIO.t.erasure == classOf[Integer])) {
      outputFunction(
        "__kernel void range_array",
        Seq(sizeParam, kernelParams),
        Seq(
          bodyPrefix,
          indexHeader,
          sizeHeader,
          kernDeclsRange,
          assigntRange,
          bodySuffix
        ),
        kernelsSource
      )
    }
    if (verbose)
      println("[ScalaCL] Creating kernel with source <<<\n\t" + kernelsSource.toString.replaceAll("\n", "\n\t") + "\n>>>")
      
    SourceData(
      functionName = functionName,
      functionSource = functionSource,
      kernelsSource = kernelsSource.toString,
      includedSources = includedSources,
      outerDeclarations = outerDeclarations
    )
  }
}

import CLFunctionCode._

class CLFunctionCode[A, B](
  val sourceData: SourceData,
  val extraArgsIOs: CapturedIOs = CapturedIOs()
)(
  implicit 
  val aIO: CLDataIO[A], 
  val bIO: CLDataIO[B]
) 
extends CLCode 
{  
  import sourceData._
  
  val uid = newuid
  
  val sourcesToInclude = 
    //if (sourceData == null) null else 
    includedSources ++ Array(functionSource)
    
  override val sources = 
    //if (sourceData == null) null else 
    sourcesToInclude ++ Array(kernelsSource)
    
  override val macros = Map[String, String]()
  override val compilerArguments = Array[String]()

  //override def isOnlyInScalaSpace = sourceData == null
  
  protected def throwIfCapture(f: CLFunctionCode[_, _]) = {
    if (!extraArgsIOs.isEmpty)
      throw new UnsupportedOperationException("Cannot compose functions that capture external variables !")
  }
  
  def compose[C](f: CLFunctionCode[C, A])(implicit cIO: CLDataIO[C]) = {
    throwIfCapture(f)
    
    compositions.synchronized {
      compositions.getOrElseUpdate((uid, f.uid), {
        // TODO FIXME !
        new CLFunctionCode[C, B](
          sourceData = buildSourceData[C, B](
            outerDeclarations = Array(),// TODO ??? outerDeclarations ++ f.outerDeclarations, 
            declarations = Array(),
            expressions = Array(functionName + "(" + f.sourceData.functionName + "(_))"),
            includedSources = sourcesToInclude ++ f.sourcesToInclude
          )(f.aIO, bIO)
        ).asInstanceOf[CLFunctionCode[_, _]]
      }).asInstanceOf[CLFunctionCode[C, B]]
    }
  }
  
  def and(f: CLFunctionCode[A, B]) = {
    ands.synchronized {
      ands.getOrElseUpdate((uid, f.uid), {
        // TODO FIXME !
        new CLFunctionCode[A, B](
          sourceData = buildSourceData[A, B](
            outerDeclarations = Array(),  
            declarations = Array(), 
            expressions = Array("(" + functionName + "(_) && " + f.sourceData.functionName + "(_))"), 
            includedSources = sourcesToInclude ++ f.sourcesToInclude
          )
        ).asInstanceOf[CLFunctionCode[_, _]]
      }).asInstanceOf[CLFunctionCode[A, B]]
    }
  }
}
