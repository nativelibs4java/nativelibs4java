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
   with WorkaroundsForOtherPhases
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
            //case TypeTree() =>
            //  println(nodeToString(tree.symbol.tpt))
            //  super.transform(tree)
            case ArrayTabulate(componentType, lengths @ (firstLength :: otherLengths), f @ Func(params, body), manifest) =>
              val tpe = body.tpe
              val returnType = if (tpe.isInstanceOf[ConstantType]) 
                tpe.widen
              else
                tpe
              
              val lengthDefs = lengths.map(length => newVariable(unit, "n$", currentOwner, tree.pos, false, length.setType(IntClass.tpe)))
                  
              msg(unit, tree.pos, "transformed Array.tabulate[" + returnType + "] into equivalent while loop") {
                  
                def replaceTabulates(lengthDefs: List[VarDef], parentArrayIdentGen: IdentGen, params: List[ValDef], mappings: Map[Symbol, TreeGen], symbolReplacements: Map[Symbol, Symbol]): (Tree, Type) = {
              
                  val param = params.head
                  val pos = tree.pos
                  val nVar = lengthDefs.head
                  val iVar = newVariable(unit, "i$", currentOwner, pos, true, newInt(0))
                  
                  val newMappings: Map[Symbol, TreeGen] = mappings + (param.symbol -> iVar)
                  val newReplacements = symbolReplacements ++ Map(param.symbol -> iVar.symbol, f.symbol -> currentOwner)
                  
                  val mappedArrayTpe = getMappedArrayType(lengthDefs.size, returnType)
                  
                  val arrayVar = if (parentArrayIdentGen == null)
                    newVariable(unit, "m$", currentOwner, tree.pos, false, newArrayMulti(mappedArrayTpe, returnType, lengthDefs.map(_.identGen()), manifest))
                  else
                    VarDef(parentArrayIdentGen, null, null)
                  
                  val subArrayVar =  if (lengthDefs.tail == Nil)
                    null
                  else
                    newVariable(unit, "subArray$", currentOwner, tree.pos, false, newApply(tree.pos, arrayVar(), iVar()))
                                    
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
                        subArrayVar,
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
                              lengthDefs.map(_.definition) ++ List(arrayVar.definition)
                            else 
                              Nil
                          ) ++
                          List(
                            iVar.definition,
                            whileLoop(
                              currentOwner,
                              unit,
                              tree,
                              binOp(
                                iVar(),
                                IntClass.tpe.member(nme.LT),
                                nVar()
                              ),
                              Block(
                                (
                                  if (lengthDefs.tail == Nil)
                                    List(
                                      newUpdate(
                                        tree.pos,
                                        arrayVar(),
                                        iVar(),
                                        newBody
                                      )
                                    )
                                  else {
                                    List(
                                      subArrayVar.definition,
                                      newBody
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
            case Foreach(collection, f @ Func(List(param), body)) =>
              collection match {
                case CollectionRewriter(colRewriter) =>
                  import colRewriter._
                  msg(unit, tree.pos, "transformed " + colType.colToString(tpe) + ".foreach into equivalent while loop.") {
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
                            Map(param.symbol -> env.itemVar),
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
                                          Map(filterParam.symbol -> env.itemVar),
                                          Map(
                                            filterParam.symbol -> env.itemVar.symbol,
                                            f.symbol -> currentOwner
                                          ),
                                          unit
                                        )
                                    }).reduceLeft(boolAnd),
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
              
            case TraversalOp(traversalOp) =>
              
              import traversalOp._
              
              var leftParam: ValDef = null
              var rightParam: ValDef = null
              var body: Tree = null
              if (op.f != null) {
                  op.f match { 
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
                case CollectionRewriter(colRewriter) =>
                  import colRewriter._
                  if ((isLeft || colType.supportsRightVariants) && (ScalaCLPlugin.experimental || colType.isSafeRewrite(op)))
                    msg(unit, tree.pos, "transformed " + colType.colToString(tpe) + "." + op + " into equivalent while loop.") {
                      super.transform(
                        op match {
                          case TraversalOp.Reduce(_, _) | TraversalOp.Fold(_, _) | TraversalOp.Sum | TraversalOp.Min | TraversalOp.Max =>
                            val skipFirst = op.loopSkipsFirst
                            colType.foreach[VarDef](
                              tree,
                              array,
                              componentType,
                              !isLeft,
                              skipFirst,
                              env => {
                                assert((initialValue == null) == !op.needsInitialValue) // no initial value for reduce-like ops only
                                val totVar = newVariable(unit, op + "$", currentOwner, tree.pos, true,
                                  if (initialValue == null) {
                                    op match {
                                      case TraversalOp.Sum =>
                                        Literal(Constant(0: Byte)).setType(componentType.tpe)
                                      case _ =>
                                        // Take first or last value for Reduce, Min, Max
                                        assert(skipFirst)
                                        newApply(
                                          tree.pos,
                                          env.aVar(),
                                          if (isLeft)
                                            newInt(0)
                                          else
                                            intAdd(env.nVar(), newInt(-1))
                                        )
                                    }
                                  } else
                                    initialValue
                                )
                                new LoopOuters(List(totVar.definition), totVar(), payload = totVar)
                              },
                              env => {
                                val totVar = env.payload
                                LoopInners(
                                  List(
                                    if (body == null) {
                                      assert(!op.needsFunction)
                                      val totIdent = totVar()
                                      op match {
                                        case TraversalOp.Sum =>
                                          Assign(totVar(), binOp(totIdent, totIdent.tpe.member(nme.PLUS), env.itemVar())).setType(UnitClass.tpe)
                                        case TraversalOp.Min =>
                                          If(
                                            binOp(env.itemVar(), totIdent.tpe.member(nme.LT), totIdent),
                                            Assign(totVar(), env.itemVar()).setType(UnitClass.tpe),
                                            newUnit
                                          )
                                        case TraversalOp.Max =>
                                          If(
                                            binOp(env.itemVar(), totIdent.tpe.member(nme.GT), totIdent),
                                            Assign(totVar(), env.itemVar()).setType(UnitClass.tpe),
                                            newUnit
                                          )
                                        case _ =>
                                          throw new RuntimeException("unexpected op : " + op)
                                      }
                                    } else {
                                      Assign(
                                        totVar(),
                                        replaceOccurrences(
                                          body,
                                          Map(
                                            accParam.symbol -> totVar,
                                            newParam.symbol -> env.itemVar
                                          ),
                                          Map(op.f.symbol -> currentOwner),
                                          unit
                                        )
                                      )
                                    }
                                  )
                                )
                              }
                            )
                          case TraversalOp.Scan(f, _) =>
                            colType.foreach[(CollectionBuilder, VarDef, VarDef)](
                              tree,
                              array,
                              componentType,
                              !isLeft,
                              false,
                              env => {
                                val cb @ CollectionBuilder(builderCreation, _, _, builderResult) = colType.newBuilder(
                                  collection.pos,
                                  componentType.tpe,
                                  mappedCollectionType,
                                  () => intAdd(env.nVar(), newInt(1)),
                                  localTyper
                                )
                                val builderVar = newVariable(
                                  unit,
                                  "builder$",
                                  currentOwner,
                                  tree.pos,
                                  true,
                                  builderCreation
                                )
                                val totVar = newVariable(unit, "tot$", currentOwner, tree.pos, true, initialValue)
                                new LoopOuters(
                                  List(
                                    totVar.definition,
                                    builderVar.definition,
                                    cb.setOrAdd(builderVar, () => newInt(0), totVar)
                                  ),
                                  builderResult(builderVar),
                                  payload = (cb, builderVar, totVar)
                                )
                              },
                              env => {
                                val (cb, builderVar, totVar) = env.payload
                                LoopInners(
                                  List(
                                    Assign(
                                      totVar(),
                                      replaceOccurrences(
                                        body,
                                        Map(
                                          accParam.symbol -> totVar,
                                          newParam.symbol -> env.itemVar
                                        ),
                                        Map(f.symbol -> currentOwner),
                                        unit
                                      )
                                    ).setType(UnitClass.tpe),
                                    cb.setOrAdd(
                                      builderVar,
                                      () => if (isLeft)
                                        intAdd(env.iVar(), newInt(1))
                                      else
                                        intSub(env.nVar(), env.iVar()),
                                      totVar
                                    )
                                  )
                                )
                              }
                            )
                          case TraversalOp.AllOrSome(f, all) =>
                            colType.foreach[VarDef](
                              tree,
                              array,
                              componentType,
                              false,
                              false,
                              env => {
                                val hasTrueVar = newVariable(
                                  unit,
                                  "hasTrue$",
                                  currentOwner,
                                  tree.pos,
                                  true,
                                  newBool(all)
                                )
                                new LoopOuters(
                                  List(
                                    hasTrueVar.definition
                                  ),
                                  hasTrueVar(),
                                  payload = hasTrueVar
                                )
                              },
                              env => {
                                val hasTrueVar = env.payload
                                LoopInners(
                                  List(
                                    Assign(
                                        hasTrueVar(),
                                        replaceOccurrences(
                                          super.transform(body),
                                          Map(
                                            leftParam.symbol -> env.itemVar
                                          ),
                                          Map(f.symbol -> currentOwner),
                                          unit
                                        )
                                    )
                                  ),
                                  if (all)
                                    hasTrueVar()
                                  else
                                    boolNot(hasTrueVar())
                                )
                              }
                            )
                          case TraversalOp.Filter(f, not) =>
                            colType.foreach[(CollectionBuilder, VarDef)](
                              tree,
                              array,
                              componentType,
                              false,
                              false,
                              env => {
                                val cb @ CollectionBuilder(builderCreation, _, _, builderResult) = colType.newBuilder(collection.pos, componentType.tpe, null, null, localTyper)
                                val builderVar = newVariable(
                                  unit,
                                  "builder$",
                                  currentOwner,
                                  tree.pos,
                                  true,
                                  builderCreation
                                )
                                new LoopOuters(
                                  List(
                                    builderVar.definition
                                  ),
                                  builderResult(builderVar),
                                  payload = (cb, builderVar)
                                )
                              },
                              env => {
                                val (cb, builderVar) = env.payload
                                val cond = replaceOccurrences(
                                  super.transform(body),
                                  Map(
                                    leftParam.symbol -> env.itemVar
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
                                      cb.add(builderVar, env.itemVar),
                                      newUnit
                                    )
                                  )
                                )
                              }
                            )
                          case TraversalOp.Count(f) =>
                            colType.foreach[(VarDef)](
                              tree,
                              array,
                              componentType,
                              false,
                              false,
                              env => {
                                val countVar = newVariable(
                                  unit,
                                  "count$",
                                  currentOwner,
                                  tree.pos,
                                  true,
                                  newInt(0)
                                )
                                new LoopOuters(
                                  List(
                                    countVar.definition
                                  ),
                                  countVar(),
                                  payload = countVar
                                )
                              },
                              env => {
                                val countVar = env.payload
                                val cond = replaceOccurrences(
                                  super.transform(body),
                                  Map(
                                    leftParam.symbol -> env.itemVar
                                  ),
                                  Map(f.symbol -> currentOwner),
                                  unit
                                )
                                LoopInners(
                                  List(
                                    If(
                                      cond,
                                      incrementIntVar(countVar, newInt(1)),
                                      newUnit
                                    )
                                  )
                                )
                              }
                            )
                          case TraversalOp.FilterWhile(f, take) =>
                            colType.foreach[(CollectionBuilder, VarDef, VarDef)](
                              tree,
                              array,
                              componentType,
                              false,
                              false,
                              env => {
                                val passedVar = newVariable(
                                  unit,
                                  "passed$",
                                  currentOwner,
                                  tree.pos,
                                  true,
                                  newBool(false)
                                )
                                val cb @ CollectionBuilder(builderCreation, _, _, builderResult) = colType.newBuilder(collection.pos, componentType.tpe, null, null, localTyper)
                                val builderVar = newVariable(
                                  unit,
                                  "builder$",
                                  currentOwner,
                                  tree.pos,
                                  true,
                                  builderCreation
                                )
                                new LoopOuters(
                                  List(
                                    builderVar.definition,
                                    passedVar.definition
                                  ),
                                  builderResult(builderVar),
                                  payload = (cb, passedVar, builderVar)
                                )
                              },
                              env => {
                                val (cb, passedVar, builderVar) = env.payload
                                val cond = boolNot(
                                  replaceOccurrences(
                                    super.transform(body),
                                    Map(
                                      leftParam.symbol -> env.itemVar
                                    ),
                                    Map(f.symbol -> currentOwner),
                                    unit
                                  )
                                )
                                LoopInners(
                                  if (take) {
                                    List(
                                      Assign(
                                        passedVar(),
                                        cond
                                      ).setType(UnitClass.tpe),
                                      If(
                                        boolNot(passedVar()),
                                        cb.add(builderVar, env.itemVar),
                                        newUnit
                                      )
                                    )
                                  } else {
                                    List(
                                      If(
                                        boolOr(
                                          passedVar(),
                                          Block(
                                            List(
                                              Assign(
                                                passedVar(),
                                                cond
                                              ).setType(UnitClass.tpe)
                                            ),
                                            passedVar()
                                          ).setType(BooleanClass.tpe)
                                        ),
                                        cb.add(builderVar, env.itemVar),
                                        newUnit
                                      )
                                    )
                                  },
                                  if (take)
                                    boolNot(passedVar())
                                  else
                                    null
                                )
                              }
                            )
                          case TraversalOp.Map(f) =>
                            colType.foreach[(CollectionBuilder, VarDef)](
                              tree,
                              array,
                              componentType,
                              false,
                              false,
                              env => {
                                val cb @ CollectionBuilder(builderCreation, _, _, builderResult) = colType.newBuilder(collection.pos, resultType, tree.tpe, env.outputSizeVar, localTyper)
                                val builderVar = newVariable(
                                  unit,
                                  "builder$",
                                  currentOwner,
                                  tree.pos,
                                  true,
                                  builderCreation
                                )
                                new LoopOuters(
                                  List(
                                    builderVar.definition
                                  ),
                                  builderResult(builderVar),
                                  payload = (cb, builderVar)
                                )
                              },
                              env => {
                                val (cb, builderVar) = env.payload
                                val content = replaceOccurrences(
                                  super.transform(body),
                                  Map(
                                    leftParam.symbol -> env.itemVar
                                  ),
                                  Map(f.symbol -> currentOwner),
                                  unit
                                )
                                LoopInners(
                                  // TODO !!! List(cb.setOrAdd(builderVar, env.outputIndexVar, () => content))
                                  List(cb.setOrAdd(builderVar, env.iVar, () => content))
                                )
                              }
                            )
                          case _ =>
                            msg(unit, tree.pos, "INFO: will soon optimize this " + tpe + "." + op + " call") {
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
