/*
 * ScalaCL - putting Scala on the GPU with JavaCL / OpenCL
 * http://scalacl.googlecode.com/
 *
 * Copyright (c) 2009-2013, Olivier Chafik (http://ochafik.com/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Olivier Chafik nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY OLIVIER CHAFIK AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package scalaxy.common

import scala.reflect.api.Universe

trait StreamTransformers
extends MiscMatchers
   with TreeBuilders
   with TraversalOps
   with Streams 
   with StreamSources 
   with StreamOps
   with StreamSinks
{
  val global: Universe
  import global._
  import definitions._
  import Flag._
  
  def stream = true
  
  case class OpsStream(
    source: StreamSource, 
    colTree: Tree, 
    ops: List[StreamTransformer]
  )
  
  def newTransformer = new Transformer /* TODO: TypingTransformer */ {
    object OpsStream {
      def unapply(tree: Tree) = {
        var ops = List[StreamTransformer]()
        var colTree = tree
        var source: StreamSource = null
        var finished = false
        while (!finished) {
          //println("Trying to match " + colTree)
          colTree match {
            case TraversalOp(traversalOp) if traversalOp.op.isInstanceOf[StreamTransformer] =>
              //println("found op " + traversalOp + "\n\twith collection = " + traversalOp.collection)
              val trans = traversalOp.op.asInstanceOf[StreamTransformer]
              if (trans.resultKind != StreamResult)
                ops = List()
              ops = trans :: ops
              colTree = traversalOp.collection
            case StreamSource(cr) =>
              //println("found streamSource " + cr.getClass + " (ops = " + ops + ")")
              source = cr
              if (colTree != cr.unwrappedTree) {
                //println("Unwrapping " + colTree.tpe + " into " + cr.unwrappedTree.tpe)
                colTree = cr.unwrappedTree
              } else
                finished = true
            case _ =>
              //if (!ops.isEmpty) println("Finished with " + ops.size + " ops upon "+ tree + " ; source = " + source + " ; colTree = " + colTree)
              finished = true
          }
        }
        if (ops.isEmpty || source == null)
          None
        else
          Some(new OpsStream(source, colTree, ops))
      }
    }

    var matchedColTreeIds = Set[Tree]()

    override def transform(tree: Tree): Tree = {
      //val retryWithSmallerChain = false
      //def internalTransform(tree: Tree, retryWithSmallerChain: Boolean) = transform(tree)
      
      internalTransform(tree)
    }
      
    protected def internalTransform(
        tree: Tree, 
        retryWithSmallerChain: Boolean = true): Tree = 
    {
      //if (!shouldOptimize(tree))
      //  super.transform(tree)
      //else
        try {
          tree match {
            case ArrayTabulate(componentType, lengths @ (firstLength :: otherLengths), f @ Func(params, body), manifest) =>
              val tpe = body.tpe
              val returnType = //if (tpe.isInstanceOf[ConstantType]) 
                tpe.normalize.widen
              //else
              //  tpe
              
              val lengthDefs = lengths.map { case length =>
                newVariable("n$", currentOwner, tree.pos, false, typeCheck(length, IntTpe))
              }
                  
              //msg(tree.pos, "transformed Array.tabulate[" + returnType + "] into equivalent while loop") 
              {
                  
                def replaceTabulates(lengthDefs: List[VarDef], parentArrayIdentGen: IdentGen, params: List[ValDef], mappings: Map[Symbol, TreeGen], symbolReplacements: Map[Symbol, Symbol]): (Tree, Type) = {
              
                  val param = params.head
                  val pos = tree.pos
                  val nVar = lengthDefs.head
                  val iVar = newVariable("i$", currentOwner, pos, true, newInt(0))
                  val iVal = newVariable("i$val$", currentOwner, pos, false, iVar())
                  
                  val newMappings: Map[Symbol, TreeGen] = mappings + (param.symbol -> iVal)
                  val newReplacements = symbolReplacements ++ Map(param.symbol -> iVal.symbol, f.symbol -> currentOwner)
                  
                  val mappedArrayTpe = getArrayType(lengthDefs.size, returnType)
                  
                  val arrayVar = if (parentArrayIdentGen == null)
                    newVariable("m$", currentOwner, tree.pos, false, newArrayMulti(mappedArrayTpe, returnType, lengthDefs.map(_.identGen()), manifest))
                  else
                    VarDef(parentArrayIdentGen, null, null)
                  
                  val subArrayVar =  if (lengthDefs.tail == Nil)
                    null
                  else
                    newVariable("subArray$", currentOwner, tree.pos, false, newApply(tree.pos, arrayVar(), iVal()))
                                    
                  val (newBody, bodyType) = if (lengthDefs.tail == Nil)
                      (
                          replaceOccurrences(
                            body,
                            newMappings,
                            newReplacements,
                            Map()
                          ),
                          returnType
                      )
                  else
                      replaceTabulates(
                        lengthDefs.tail,
                        subArrayVar,
                        params.tail,
                        newMappings,
                        newReplacements
                      )
                  
                  val checkedBody = typeCheck(
                    newBody,
                    bodyType
                  )
                  
                  (
                    super.transform {
                      typed {
                        treeCopy.Block(
                          tree,
                          (
                            if (parentArrayIdentGen == null) 
                              lengthDefs.map(_.definition) :+ arrayVar.definition
                            else 
                              Nil
                          ) ++
                          List(
                            iVar.definition,
                            whileLoop(
                              currentOwner,
                              tree,
                              binOp(
                                iVar(),
                                IntTpe.member(LT),
                                nVar()
                              ),
                              Block(
                                (
                                  if (lengthDefs.tail == Nil)
                                    List(
                                      iVal.definition,
                                      newUpdate(
                                        tree.pos,
                                        arrayVar(),
                                        iVar(),
                                        checkedBody
                                      )
                                    )
                                  else {
                                    List(
                                      iVal.definition,
                                      subArrayVar.definition,
                                      checkedBody
                                    )
                                  }
                                ),
                                incrementIntVar(iVar, newInt(1))
                              )
                            )
                          ),
                          if (parentArrayIdentGen == null)
                            arrayVar()
                          else
                            newUnit
                        )
                      }
                    },
                    mappedArrayTpe
                  )
                }
                replaceTabulates(lengthDefs, null, params, Map(), Map())._1
              }
            case OpsStream(opsStream) 
            if 
              stream &&
              //(opsStream.source ne null) && 
              //!opsStream.ops.isEmpty && 
              //(opsStream ne null) && 
              (opsStream.colTree ne null) && 
              !matchedColTreeIds.contains(opsStream.colTree) 
              =>
              import opsStream._
              
              val txt = "Streamed ops on " + (if (source == null) "UNKNOWN COL" else source.tree.tpe) + " : " + ops/*.map(_.getClass.getName)*/.mkString(", ")
              matchedColTreeIds += colTree
              //msg(tree.pos, "# " + txt) 
              
              {
                try {
                  val stream = Stream(source, ops)
                  checkStreamWillBenefitFromOptimization(stream)
                  val asm = assembleStream(stream, tree, this.transform _, tree.pos, currentOwner)
                  //println(txt + "\n\t" + asm.toString.replaceAll("\n", "\n\t"))
                  //println("### TRANSFORMED : ###\n" + nodeToString(asm))
                  asm
                } catch {
                  case BrokenOperationsStreamException(msg, sourceAndOps, componentsWithSideEffects) =>
                    warning(sourceAndOps.head.tree.pos, "Cannot optimize this operations stream due to side effects")
                    for (SideEffectFullComponent(comp, sideEffects, preventedOptimizations) <- componentsWithSideEffects) {
                      for (sideEffect <- sideEffects) {
                        if (preventedOptimizations)
                          warning(sideEffect.pos, 
                                  "This side-effect prevents optimization of the enclosing " + comp + " operation ; node = " + sideEffect //+
                            //(if (verbose) " ; node = " + nodeToString(sideEffect) else "")
                          )
                        else if (verbose)
                          warnSideEffect(sideEffect)
                      }
                      //println("Side effects of " + comp + " :\n\t" + sideEffects.mkString(",\n\t"))
                    }
                    
                    val sub = super.transform(tree)
                    if (retryWithSmallerChain)
                      internalTransform(sub, retryWithSmallerChain = false)
                    else
                      sub
                }
              }
            case _ =>
              super.transform(tree)//toMatch)
          }
        } catch {
          case ex: CodeWontBenefitFromOptimization =>
            if (verbose)
              warning(tree.pos, ex.toString)
            super.transform(tree)
          case ex: Throwable =>
            if (verbose)
              ex.printStackTrace
            super.transform(tree)
        }
    }
  }
  def checkStreamWillBenefitFromOptimization(stream: Stream): Unit = {
    val Stream(source, transformers) = stream
    
    val sourceAndOps = source +: transformers
    
    import TraversalOps._
    
    val closuresCount = sourceAndOps.map(_.closuresCount).sum
    (transformers, closuresCount, source) match {
      case (Seq(), _, _) =>
        throw CodeWontBenefitFromOptimization("No operations chain : " + sourceAndOps)
      case (_, _, _: AbstractArrayStreamSource) if !transformers.isEmpty =>
        // ok to transform any stream that starts with an array
      case (Seq(_), 0, _) =>
        throw CodeWontBenefitFromOptimization("Only one operations without closure is not enough to optimize : " + sourceAndOps)
      case (Seq(_), 1, _: ListStreamSource) =>
        throw CodeWontBenefitFromOptimization("List operations chains need at least 2 closures to make the optimization beneficial : " + sourceAndOps)
      case 
        (
          Seq(
            _: FilterWhileOp |
            _: MaxOp |
            _: MinOp |
            _: SumOp |
            _: ProductOp |
            _: ToCollectionOp
          ), 
          1, 
          _: RangeStreamSource
        ) 
        =>
        throw CodeWontBenefitFromOptimization("This operations stream would not benefit from a while-loop-rewrite optimization : " + sourceAndOps)
      case _ =>
    }
  }
}
