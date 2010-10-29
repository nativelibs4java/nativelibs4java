/*
 * ScalaCL - putting Scala on the GPU with JavaCL / OpenCL
 * http://scalacl.googlecode.com/
 *
 * Copyright (c) 2009-2010, Olivier Chafik (http://ochafik.free.fr/)
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
package scalacl

import scala.tools.nsc.Global

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}
import scala.tools.nsc.typechecker.Analyzer
import scala.tools.nsc.typechecker.Implicits

object LoopsTransformComponent {
  val runsAfter = List[String](
    "namer"
    //, OpsFuserTransformComponent.phaseName, Seq2ArrayTransformComponent.phaseName
  )
  val runsBefore = List[String]("refchecks")
  val phaseName = "scalacl-loopstransform"
}


/**
 * Transforms the following constructs into their equivalent while loops :
 * - Array[T].foreach(x => body)
 * - Array[T].map(x => body)
 * - Array[T].reduceLeft((x, y) => body) / reduceRight
 * - Array[T].foldLeft((x, y) => body) / foldRight
 * - Array[T].scanLeft((x, y) => body) / scanRight
 */
class LoopsTransformComponent(val global: Global, val fileAndLineOptimizationFilter: ScalaCLPlugin.FileAndLineOptimizationFilter)
extends PluginComponent
   with Transform
   with TypingTransformers
   with MiscMatchers
   with TreeBuilders
   with RewritingPluginComponent
   with WithOptimizationFilter
{
  import global._
  import global.definitions._
  import scala.tools.nsc.symtab.Flags._
  import typer.{typed}    // methods to type trees

  override val runsAfter = LoopsTransformComponent.runsAfter
  override val runsBefore = LoopsTransformComponent.runsBefore
  override val phaseName = LoopsTransformComponent.phaseName

  def newTransformer(compilationUnit: CompilationUnit) = new TypingTransformer(compilationUnit) with CollectionRewriters {

    override val unit = compilationUnit
    
    def getMappedArrayType(lengths: Int, returnType: Type): Type = lengths match {
        case 1 => 
            appliedType(ArrayClass.tpe, List(returnType))
        case _ =>
            assert(lengths > 1)
            appliedType(ArrayClass.tpe, List(getMappedArrayType(lengths - 1, returnType)))
    }
    
    override def transform(tree: Tree): Tree = {
      if (!shouldOptimize(tree))
        super.transform(tree)
      else
        try {
          tree match {
            case ArrayTabulate(componentType, lengths @ (firstLength :: otherLengths), f @ Func(params, body), manifest) =>
              val tpe = body.tpe
              val returnType = if (tpe.isInstanceOf[ConstantType]) 
                tpe.widen
              else
                tpe
              
              val lengthDefs = lengths.map(length => newVariable(unit, "n$", currentOwner, tree.pos, false, length.setType(IntClass.tpe)))
                  
              msg(unit, tree.pos, "transformed Array.tabulate[" + returnType + "] into equivalent while loop") {
                  
                def replaceTabulates(lengthDefs: List[(TreeGen, Symbol, ValDef)], parentArrayIdentGen: TreeGen, params: List[ValDef], mappings: Map[Symbol, TreeGen], symbolReplacements: Map[Symbol, Symbol]): (Tree, Type) = {
              
                  val param = params.head
                  val pos = tree.pos
                  val (nIdentGen, _, nDef) = lengthDefs.head
                  val (iIdentGen, iSym, iDef) = newVariable(unit, "i$", currentOwner, pos, true, newInt(0))
                  
                  val newMappings = mappings + (param.symbol -> iIdentGen)
                  val newReplacements = symbolReplacements ++ Map(param.symbol -> iSym, f.symbol -> currentOwner)
                  
                  val mappedArrayTpe = getMappedArrayType(lengthDefs.size, returnType)
                  
                  val (arrayIdentGen: TreeGen, arraySym, arrayDef) = if (parentArrayIdentGen == null)
                    newVariable(unit, "m$", currentOwner, tree.pos, false, newArrayMulti(mappedArrayTpe, returnType, lengthDefs.map(_._1()), manifest))
                  else
                    (parentArrayIdentGen, null, null)
                  
                  val (subArrayIdentGen, _, subArrayDef) =  if (lengthDefs.tail == Nil)
                    (null, null, null)
                  else
                    newVariable(unit, "subArray$", currentOwner, tree.pos, false, newApply(tree.pos, arrayIdentGen(), iIdentGen()))
                                    
                  val (newBody, bodyType) = if (lengthDefs.tail == Nil)
                      (
                          replaceOccurrences(
                            body,
                            newMappings,
                            newReplacements,
                            unit
                          ),
                          returnType
                      )
                  else
                      replaceTabulates(
                        lengthDefs.tail,
                        subArrayIdentGen,
                        params.tail,
                        newMappings,
                        newReplacements
                      )
                  
                  newBody.setType(bodyType)
                  
                  (
                    super.transform {
                      typed {
                        treeCopy.Block(
                          tree,
                          (
                            if (parentArrayIdentGen == null) 
                                lengthDefs.map(_._3) ++ List(arrayDef) 
                            else 
                                Nil
                          ) ++
                          List(
                            iDef,
                            whileLoop(
                              currentOwner,
                              unit,
                              tree,
                              binOp(
                                iIdentGen(),
                                IntClass.tpe.member(nme.LT),
                                nIdentGen()
                              ),
                              Block(
                                (
                                  if (lengthDefs.tail == Nil)
                                    List(
                                      newUpdate(
                                        tree.pos,
                                        arrayIdentGen(),
                                        iIdentGen(),
                                        newBody
                                      )
                                    )
                                  else {
                                    List(
                                      subArrayDef,
                                      newBody
                                    )
                                  }
                                ),
                                incrementIntVar(iIdentGen, newInt(1))
                              )
                            )
                          ),
                          if (parentArrayIdentGen == null)
                            arrayIdentGen()
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
            case Foreach(collection, f @ Func(List(param), body)) =>
              collection match {
                case CollectionRewriter(colType, tpe, array, componentType) =>
                  msg(unit, tree.pos, "transformed " + tpe + ".foreach into equivalent while loop.") {
                    if (array != null)
                      array.tpe = tpe

                    super.transform(
                      colType.foreach[Unit](
                        tree,
                        array,
                        componentType,
                        false,
                        false,
                        env => new LoopOuters(Nil, null, payload = ()), // no extra outer statement
                        env => {
                          val content = replaceOccurrences(
                            body,
                            Map(param.symbol -> env.itemIdentGen),
                            Map(f.symbol -> currentOwner),
                            unit
                          )
                          LoopInners(
                            List(
                              colType.filters match {
                                case Nil =>
                                  content
                                case filterFunctions: List[Tree] =>
                                  If(
                                    (filterFunctions.map {
                                      case Func(List(filterParam), filterBody) =>
                                        replaceOccurrences(
                                          filterBody,
                                          Map(filterParam.symbol -> env.itemIdentGen),
                                          Map(
                                            filterParam.symbol -> env.itemSym,
                                            f.symbol -> currentOwner
                                          ),
                                          unit
                                        )
                                    }).reduceLeft(newLogicAnd),
                                    content,
                                    newUnit
                                  )
                              }
                            )
                          )
                        }
                      )
                    )
                  }
                case _ =>
                  super.transform(tree)
              }
              
            case TraversalOp(op, collection, resultType, mappedCollectionType, f, isLeft, initialValue) =>
              var leftParam: ValDef = null
              var rightParam: ValDef = null
              var body: Tree = null
              if (f != null) {
                  f match { 
                      case Func(List(leftParamExtr, rightParamExtr), bodyExtr) =>
                        leftParam = leftParamExtr
                        rightParam = rightParamExtr
                        body = bodyExtr
                      case Func(List(uniqueParam), bodyExtr) =>
                        leftParam = uniqueParam
                        body = bodyExtr
                      case _ =>
                        return super.transform(tree)
                  }
              }
               
              val accParam = if (isLeft) leftParam else rightParam
              val newParam = if (isLeft) rightParam else leftParam
              
              collection match {
                case CollectionRewriter(colType, tpe, array, componentType) =>
                  if (isLeft || colType.supportsRightVariants)
                    msg(unit, tree.pos, "transformed " + tpe + "." + op.methodName(isLeft) + " into equivalent while loop.") {
                      //if (array != null)
                      //  array.tpe = tpe
                      super.transform(
                        op match {
                          case TraversalOp.Reduce | TraversalOp.Fold | TraversalOp.Sum | TraversalOp.Min | TraversalOp.Max =>
                            val skipFirst = op.loopSkipsFirst
                            colType.foreach[IdentGen](
                              tree,
                              array,
                              componentType,
                              !isLeft,
                              skipFirst,
                              env => {
                                assert((initialValue == null) == !op.needsInitialValue) // no initial value for reduce-like ops only
                                val (totIdentGen, _, totDef) = newVariable(unit, op + "$", currentOwner, tree.pos, true,
                                  if (initialValue == null) {
                                    op match {
                                      case TraversalOp.Sum =>
                                        Literal(Constant(0: Byte)).setType(componentType.tpe)
                                      case _ =>
                                        // Take first or last value for Reduce, Min, Max
                                        assert(skipFirst)
                                        newApply(
                                          tree.pos,
                                          env.aIdentGen(),
                                          if (isLeft)
                                            newInt(0)
                                          else
                                            intAdd(env.nIdentGen(), newInt(-1))
                                        )
                                    }
                                  } else
                                    initialValue
                                )
                                new LoopOuters(List(totDef), totIdentGen(), payload = totIdentGen)
                              },
                              env => {
                                val totIdentGen = env.payload
                                LoopInners(
                                  List(
                                    if (body == null) {
                                      assert(!op.needsFunction)
                                      val totIdent = totIdentGen()
                                      op match {
                                        case TraversalOp.Sum =>
                                          Assign(totIdentGen(), binOp(totIdent, totIdent.tpe.member(nme.PLUS), env.itemIdentGen())).setType(UnitClass.tpe)
                                        case TraversalOp.Min =>
                                          If(
                                            binOp(env.itemIdentGen(), totIdent.tpe.member(nme.LT), totIdent),
                                            Assign(totIdentGen(), env.itemIdentGen()).setType(UnitClass.tpe),
                                            newUnit
                                          )
                                        case TraversalOp.Max =>
                                          If(
                                            binOp(env.itemIdentGen(), totIdent.tpe.member(nme.GT), totIdent),
                                            Assign(totIdentGen(), env.itemIdentGen()).setType(UnitClass.tpe),
                                            newUnit
                                          )
                                        case _ =>
                                          throw new RuntimeException("unexpected op : " + op)
                                      }
                                    } else {
                                      Assign(
                                        totIdentGen(),
                                        replaceOccurrences(
                                          body,
                                          Map(
                                            accParam.symbol -> totIdentGen,
                                            newParam.symbol -> env.itemIdentGen
                                          ),
                                          Map(f.symbol -> currentOwner),
                                          unit
                                        )
                                      )
                                    }
                                  )
                                )
                              }
                            )
                          case TraversalOp.Scan =>
                            //val mappedArrayTpe = appliedType(ArrayClass.tpe, List(resultType.tpe))
                            colType.foreach[(IdentGen, IdentGen, Symbol)](
                              tree,
                              array,
                              componentType,
                              !isLeft,
                              false,
                              env => {
                                //val mappedArrayType = appliedType(ArrayClass.tpe, List(componentType))
                                val (mIdentGen, _, mDef) = newVariable(unit, "m$", currentOwner, tree.pos, false, newArray(componentType.tpe, intAdd(env.nIdentGen(), newInt(1))))
                                val (totIdentGen, totSym, totDef) = newVariable(unit, "tot$", currentOwner, tree.pos, true, initialValue)//.setType(IntClass.tpe))
                                new LoopOuters(
                                  List(
                                    totDef,
                                    mDef,
                                    newUpdate(tree.pos, mIdentGen(), newInt(0), totIdentGen())
                                  ),
                                  mIdentGen(),
                                  payload = (mIdentGen, totIdentGen, totSym)
                                )
                              },
                              env => {
                                val (mIdentGen, totIdentGen, totSym) = env.payload
                                LoopInners(
                                  List(
                                    Assign(
                                      totIdentGen(),
                                      replaceOccurrences(
                                        body,
                                        Map(
                                          accParam.symbol -> totIdentGen,
                                          newParam.symbol -> env.itemIdentGen
                                        ),
                                        Map(f.symbol -> currentOwner),
                                        unit
                                      )
                                    ).setType(UnitClass.tpe),
                                    newUpdate(
                                      tree.pos,
                                      mIdentGen(),
                                      if (isLeft)
                                        intAdd(env.iIdentGen(), newInt(1))
                                      else
                                        intSub(env.nIdentGen(), env.iIdentGen()),
                                      totIdentGen())
                                  )
                                )
                              }
                            )
                          case TraversalOp.AllOrSome(all) =>
                            colType.foreach[IdentGen](
                              tree,
                              array,
                              componentType,
                              false,
                              false,
                              env => {
                                val (hasTrueIdentGen, hasTrueSym, hasTrueDef) = newVariable(
                                  unit,
                                  "hasTrue$",
                                  currentOwner,
                                  tree.pos,
                                  true,
                                  newBool(all)
                                )
                                new LoopOuters(
                                  List(
                                    hasTrueDef
                                  ),
                                  hasTrueIdentGen(),
                                  payload = hasTrueIdentGen
                                )
                              },
                              env => {
                                val hasTrueIdentGen = env.payload
                                LoopInners(
                                  List(
                                    Assign(
                                        hasTrueIdentGen(),
                                        replaceOccurrences(
                                          super.transform(body),
                                          Map(
                                            leftParam.symbol -> env.itemIdentGen
                                          ),
                                          Map(f.symbol -> currentOwner),
                                          unit
                                        )
                                    )
                                  ),
                                  if (all)
                                    hasTrueIdentGen()
                                  else
                                    boolNot(hasTrueIdentGen())
                                )
                              }
                            )
                          case TraversalOp.Filter(not) =>
                            colType.foreach[(CollectionBuilder, IdentGen, Symbol)](
                              tree,
                              array,
                              componentType,
                              false,
                              false,
                              env => {
                                val cb @ CollectionBuilder(builderCreation, _, _, builderResult) = colType.newBuilder(collection.pos, componentType)
                                val (builderIdentGen, builderSym, builderDef) = newVariable(
                                  unit,
                                  "builder$",
                                  currentOwner,
                                  tree.pos,
                                  true,
                                  builderCreation
                                )
                                new LoopOuters(
                                  List(
                                    builderDef
                                  ),
                                  builderResult(builderIdentGen),
                                  payload = (cb, builderIdentGen, builderSym)
                                )
                              },
                              env => {
                                val (cb, builderIdentGen, builderSym) = env.payload
                                //val addAssignMethod = builderSym.tpe member addAssignName
                                val cond = replaceOccurrences(
                                  super.transform(body),
                                  Map(
                                    leftParam.symbol -> env.itemIdentGen
                                  ),
                                  Map(f.symbol -> currentOwner),
                                  unit
                                )
                                LoopInners(
                                  List(
                                    If(
                                      if (not)
                                        boolNot(cond)
                                      else
                                        cond,
                                      cb.add(builderIdentGen, env.itemIdentGen),
                                      newUnit
                                    )
                                  )
                                )
                              }
                            )
                          case TraversalOp.Map =>
                            //array.tpe = appliedType(ArrayClass.tpe, List(componentType.tpe))
                            colType.foreach[(CollectionBuilder, IdentGen, Symbol)](
                              tree,
                              array,
                              componentType,
                              false,
                              false,
                              env => {
                                val cb @ CollectionBuilder(builderCreation, _, _, builderResult) = colType.newBuilder(collection.pos, resultType, mappedCollectionType, env.nIdentGen)
                                val (builderIdentGen, builderSym, builderDef) = newVariable(
                                  unit,
                                  "builder$",
                                  currentOwner,
                                  tree.pos,
                                  true,
                                  builderCreation
                                )
                                new LoopOuters(
                                  List(
                                    builderDef
                                  ),
                                  builderResult(builderIdentGen),
                                  payload = (cb, builderIdentGen, builderSym)
                                )
                              },
                              env => {
                                val (cb, builderIdentGen, builderSym) = env.payload
                                val content = replaceOccurrences(
                                  super.transform(body),
                                  Map(
                                    leftParam.symbol -> env.itemIdentGen
                                  ),
                                  Map(f.symbol -> currentOwner),
                                  unit
                                )
                                LoopInners(
                                  List(
                                    if (cb.set != null)
                                      cb.set(builderIdentGen, env.iIdentGen, () => content)
                                    else
                                      cb.add(builderIdentGen, () => content)
                                  )
                                )
                              }
                            )
                          case _ =>
                            msg(unit, tree.pos, "INFO: will soon optimize this " + tpe + "." + op.methodName(isLeft) + " call") {
                              super.transform(tree)
                            }
                        }
                      )
                    }
                  else
                    super.transform(tree)
                case _ =>
                  super.transform(tree)
              }
            case _ =>
              super.transform(tree)
          }
        } catch {
          case ex =>
            //if (ScalaCLPlugin.trace)
            //  ex.printStackTrace
            super.transform(tree)
        }
    }
  }
}
