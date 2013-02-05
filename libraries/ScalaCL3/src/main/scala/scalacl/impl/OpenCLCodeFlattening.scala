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
package scalacl.impl
import scalaxy.common._
import scalaxy.common.FlatCodes._

import scala.collection.immutable.Stack
import scala.collection.mutable.ArrayBuffer

import scala.reflect.NameTransformer
import scala.reflect.api.Universe

trait OpenCLCodeFlattening
extends MiscMatchers
   with TreeBuilders
   with TupleAnalysis
{
  import global._
  import global.definitions._
  import Flag._
  
  /**
   * Phases :
   * - unique renaming
   * - tuple cartography (map symbols and treeId to TupleSlices : x._2 will be checked against x ; if is x's symbol is mapped, the resulting slice will be composed and flattened
   * - tuple + block flattening (gives (Seq[Tree], Seq[Tree]) result)
   */
   // separate pass should return symbolsDefined, symbolsUsed
   // DefTree vs. RefTree
   
  def getDefAndRefTrees(tree: Tree) = {
    val defTrees = new ArrayBuffer[DefTree]()
    val refTrees = new ArrayBuffer[RefTree]()
    new Traverser {
      override def traverse(tree: Tree): Unit = {
        if (tree.symbol != null && tree.symbol != NoSymbol)
          tree match {
            case dt: DefTree => defTrees += dt
            case rt: RefTree => refTrees += rt
            case _ =>
          }
        super.traverse(tree)
      }
    }.traverse(tree)
    (defTrees.toArray, refTrees.toArray)
  }
  def renameDefinedSymbolsUniquely(tree: Tree) = {
    val (defTrees, refTrees) = getDefAndRefTrees(tree)
    val definedSymbols   = defTrees.collect { case d if d.name != null => d.symbol -> d.name } toMap
    val usedIdentSymbols = refTrees.collect { case ident @ Ident(name) => ident.symbol -> name} toMap
    
    val outerSymbols = usedIdentSymbols.keys.toSet.diff(definedSymbols.keys.toSet)
    val nameCollisions = (definedSymbols ++ usedIdentSymbols).groupBy(_._2).filter(_._2.size > 1)
    val renamings = nameCollisions.flatMap(_._2) map { case (sym, name) =>
      val newName: Name = N(fresh(name.toString))
      (sym, newName)
    } toMap
    
    if (renamings.isEmpty)
      tree
    else
      new Transformer {
        // TODO rename the symbols themselves ??
        override def transform(tree: Tree): Tree = {
          def setAttrs(newTree: Tree) =
            //newTree
            //withSymbol(tree.symbol, tree.tpe) {
              typeCheck(newTree, tree.tpe)
            //}
            
          renamings.get(tree.symbol).map(newName => {
            tree match {
              case ValDef(mods, name, tpt, rhs) =>
                setAttrs(treeCopy.ValDef(tree, mods, newName, super.transform(tpt), super.transform(rhs)))
              case DefDef(mods, name, tparams, vparams, tpt, rhs) =>
                setAttrs(treeCopy.DefDef(tree, mods, newName, tparams, vparams, super.transform(tpt), super.transform(rhs)))
              case Ident(name) =>
                setAttrs(treeCopy.Ident(tree, newName)) 
              case _ =>
                super.transform(tree)
            }
          }).getOrElse(super.transform(tree))
        }
      }.transform(tree)
  }

  def flatten(
      tree: Tree, 
      inputSymbols: Seq[(Symbol, Type)] = Seq(), 
      owner: Symbol = NoSymbol,
      renameSymbols: Boolean = true): FlatCode[Tree] = {
    val actualTree =
      if (renameSymbols)
        renameDefinedSymbolsUniquely(tree)
      else
        tree
    val tupleAnalyzer = new TupleAnalyzer(actualTree)
    val flattener = new TuplesAndBlockFlattener(tupleAnalyzer)
    flattener.flattenTuplesAndBlocksWithInputSymbols(actualTree, inputSymbols, owner)
  }
  
  class TuplesAndBlockFlattener(val tupleAnalyzer: TupleAnalyzer) 
  {
    import tupleAnalyzer._
    
    def isOpenCLIntrinsicTuple(components: List[Type]) = {
      components.size match {
        case 2 | 4 | 8 | 16 =>
          components.distinct.size == 1
        case _ =>
          false
      }
    }

    var sliceReplacements = new scala.collection.mutable.HashMap[TupleSlice, TreeGen]()

    object NumberConversion {
      def unapply(tree: Tree): Option[(Tree, String)] = tree match {
        case Select(expr, n) =>
          Option(n) collect {
            case toSizeTName() => (expr, "size_t")
            case toLongName() => (expr, "long")
            case toIntName() => (expr, "int")
            case toShortName() => (expr, "short")
            case toByteName() => (expr, "char")
            case toCharName() => (expr, "short")
            case toDoubleName() => (expr, "double")
            case toFloatName() => (expr, "float")
          }
        case _ =>
          None
      }
    }
    def isUnitOrNoType(tpe: Type) = tpe == NoType || tpe == UnitTpe
    
    def makeValuesSideEffectFree(code: FlatCode[Tree], symbolOwner: Symbol) = 
    {
      //code /*
      var hasNewStatements = false
      val vals = for (value <- code.values) yield {
        value match {
          case Select(ScalaMathFunction(_, _, _), toFloatName()) =>
            // special case for non-double math :
            // exp(20: Float).toFloat
            (Seq(), value)
          case ScalaMathFunction(_, _, _) =>//Apply(f @ Select(left, name), args) if left.toString == "scala.math.package" =>
            // TODO this is not fair : ScalaMathFunction should have worked here !!!
            (Seq(), value)
          /*case Apply(s @ Select(left, name), args) if NameTransformer.decode(name.toString) match {
              case op @ ("+" | "-" | "*" | "/" | "%" | "^" | "^^" | "&" | "&&" | "|" | "||" | "<<" | ">>" | "==" | "<" | ">" | "<=" | ">=" | "!=") =>
                true
              case n if left.toString == "scala.math.package" =>
                true
              case _ =>
                false
          } =>
            (Seq(), value)*/
          case Ident(_) | Select(_, _) | ValDef(_, _, _, _) | Literal(_) | NumberConversion(_, _) | Typed(_, _) | Apply(_, List(_)) =>
            // already side-effect-free (?)
            (Seq(), value)
          case _ if isUnitOrNoType(getType(value)) =>
            (Seq(), value)
          case _ =>
            assert(getType(value) != NoType, value + ": " + value.getClass.getName)// + " = " + nodeToString(value) + ")")
            val tempVar = newVariable("tmp", symbolOwner, value.pos, false, value)
            //println("Creating temp variable " + tempVar.symbol + " for " + value)
            hasNewStatements = true
            for (slice <- getTreeSlice(value))
              setSlice(tempVar.definition, slice)
              
            (Seq(tempVar.definition), tempVar())
        }
      }
      if (!hasNewStatements)
        code
      else
        FlatCode[Tree](
          code.outerDefinitions,
          code.statements ++ vals.flatMap(_._1),
          vals.map(_._2)
        )
    }
    def flattenTuplesAndBlocksWithInputSymbols(tree: Tree, inputSymbols: Seq[(Symbol, Type)], symbolOwner: Symbol): FlatCode[Tree] = {
      //setSlice(inputSymbol, TupleSlice(inputSymbol, 0, flattenedTypes.size))
        
      val fiberVars = inputSymbols.flatMap { case (inputSymbol, inputType) =>
        val flattenedTypes = flattenTypes(inputType)
        //println(s"flattenTypes($inputSymbol, $inputType) = $flattenedTypes")
        if (flattenedTypes.size == 1) {
          sliceReplacements ++= Seq(TupleSlice(inputSymbol, 0, 1) -> (() => ident(inputSymbol, inputType, inputSymbol.name)))
          Seq()
        } else {
          val fiberPaths = flattenFiberPaths(inputType)
          //println("flattenedTypes = " + flattenedTypes)
          //println(s"fiberPaths = $fiberPaths")
          fiberPaths.zip(flattenedTypes).zipWithIndex map { case ((path, tpe), i) =>
            val fiberExpr = applyFiberPath(() => {
              val res = ident(inputSymbol, inputType, inputSymbol.name)
              setSlice(res, TupleSlice(inputSymbol, i, 1))
              res
            }, path)
            //setType(fiberExpr, tpe)
            val fiberVar = newVariable(inputSymbol.name + "$" + path.map(_ + 1).mkString("$"), symbolOwner, tree.pos, false, fiberExpr)
            sliceReplacements ++= Seq(TupleSlice(inputSymbol, i, 1) -> fiberVar.identGen)
            //println(s"fiberExpr = $fiberExpr, fiberVar = $fiberVar")
            fiberVar
          }
        }
      }
      //println(s"fiberVars = $fiberVars, sliceReplacements = $sliceReplacements, inputSymbols = $inputSymbols")
      val top @ FlatCode(outerDefinitions, statements, values) = flattenTuplesAndBlocks(tree)(symbolOwner)
      //top.printDebug("top")
      //println(s"top = $top")
      //top
      //*
      def lastMinuteReplace = {
        val trans = new Transformer {
          override def transform(tree: Tree) =
            replace(super.transform(tree))
        }
        trans transform _
      }
      val replaced = FlatCode[Tree](
        outerDefinitions map lastMinuteReplace,
        (fiberVars.map(_.definition) ++ statements) map lastMinuteReplace,
        values map lastMinuteReplace
      )
      //replaced.printDebug("replaced")
      replaced
      //*/
      /*FlatCode[Tree](
        outerDefinitions,
        fiberVars.map(_.definition) ++ statements,
        values
      )*/
    }
    def replace(tree: Tree): Tree =
      replaceValues(tree) match {
        case Seq(unique) => unique
        case _ => tree
      }

    def replaceValues(tree: Tree): Seq[Tree] = tree match {
      case ValDef(_, _, _, _) =>
        Seq(tree)
      case _ =>
        try {
          getTreeSlice(tree, recursive = true) match {
            case Some(slice) if slice.baseSymbol != tree.symbol =>
              sliceReplacements.get(slice) match {
                case Some(identGen) =>
                  val ident = identGen()
                  //setSlice(ident, slice)
                  //println("Replacing " + tree + " by " + ident + " (slice = " + slice + ")")
                  Seq(ident)
                case None =>
                  for (i <- 0 until slice.sliceLength) yield {
                    val subSlice = slice.subSlice(i, 1)
                    val ident = subSlice.toTreeGen(tupleAnalyzer)()
                    //println("Replacing " + tree + " by slice ident " + ident + " (slice = " + slice + ")")
                    //println("Tree " + tree + " has associated slice " + slice + ", extracting subSlice " + subSlice + " = " + ident)
                    ident
                  }
              }
            case _ =>
              //println("Tree " + tree + " has no associated slice")
              Seq(tree)
          }
        } catch { case ex: Throwable =>
          ex.printStackTrace
          Seq(tree)
        }
    }
    
    def flattenTuplesAndBlocks(initialTree: Tree, sideEffectFree: Boolean = true)(implicit symbolOwner: Symbol): FlatCode[Tree] = {
      val tree = initialTree//replace(initialTree)
      // If the tree is mapped to a slice and that slice is mapped to a replacement, then replace the tree by an ident to the corresponding name+symbol
      val res: FlatCode[Tree] = tree match {
        case Block(statements, value) => 
          // Flatten blocks up
          statements.map(flattenTuplesAndBlocks(_).noValues).reduceLeft(_ ++ _) >> flattenTuplesAndBlocks(value)
        case TupleCreation(components) =>
          val sub = components.map(flattenTuplesAndBlocks(_))
          FlatCode[Tree](
            sub.flatMap(_.outerDefinitions),
            sub.flatMap(_.statements),
            sub.flatMap(_.values)
          )
        case TupleComponent(target, i) =>//if getTreeSlice(target).collect(sliceReplacements) != None =>
          getTreeSlice(target, recursive = true) match {
            case Some(slice) =>
              //println("Found slice " + slice + " for tuple component " + target + " i = " + i)
              sliceReplacements.get(slice) match {
                case Some(rep) =>
                  FlatCode[Tree](Seq(), Seq(), Seq(rep()))
                case None =>
                  FlatCode[Tree](Seq(), Seq(), Seq(tree))
              }
            case _ =>
              FlatCode[Tree](Seq(), Seq(), Seq(tree))
          }
        case Ident(_) | Literal(_) =>
          // this ident has no known replacement !
          FlatCode[Tree](Seq(), Seq(), Seq(tree))
        case s @ Select(This(targetClass), name) =>
          FlatCode[Tree](
            Seq(),
            Seq(),
            Seq(withSymbol(s.symbol) { Ident(name) })
          )
        case Select(target, name) =>
          //println("CONVERTING select " + tree)
          val FlatCode(defs, stats, vals) = flattenTuplesAndBlocks(target, sideEffectFree = getType(target) != NoType)
          //println(s"NAME = '$name', encoded = '${NameTransformer.encode(name.toString)}, decoded = '${NameTransformer.decode(name.toString)}") 
          val res = FlatCode[Tree](
            defs,
            stats,
            vals.map(v => Select(v, NameTransformer.decode(name.toString)))
          )
          //println("\tres = " + res)
          res
        case Apply(ident @ Ident(functionName), args) =>
          val f = args.map(flattenTuplesAndBlocks(_))
          // TODO assign vals to new vars before the calls, to ensure a correct evaluation order !
          FlatCode[Tree](
            f.flatMap(_.outerDefinitions),
            f.flatMap(_.statements),
            Seq(
              typeCheck(//withSymbol(tree.symbol, tree.tpe) {
                Apply(
                  Ident(ident.symbol),//withSymbol(ident.symbol, ident.tpe) { Ident(functionName) }, 
                  f.flatMap(_.values)
                ),
                tree.tpe
              )
            )
          )
        case Assign(lhs, rhs) =>
          merge(Seq(lhs, rhs).map(flattenTuplesAndBlocks(_)):_*) { 
            case Seq(l, r) => 
              Seq(Assign(l, r)) 
          }
        case Apply(Select(target, updateName()), List(index, value)) if isTupleType(getType(value)) =>
          //println(s"CONVERTING update $tree (isTupleType(${getType(value)}) = ${isTupleType(getType(value))})")
          val indexVal = newVariable("index", symbolOwner, tree.pos, false, index)
          
          val mf = Seq(target, value).map(flattenTuplesAndBlocks(_))
          val m = merge(mf/*Seq(target, value).map(flattenTuplesAndBlocks(_))*/:_*) { 
            case Seq(t, v) => 
              Seq(Apply(Select(t, updateName()), List(indexVal(), v)))
            //case s =>
            //  sys.error(s"CONVERTED Seq(target = $target, value = $value, mf = $mf) to $s")
          }
          m.copy(statements = Seq(indexVal.definition) ++ m.statements)
        case Apply(target, args) =>
          //println("CONVERTING apply " + tree)
          /*
          val convArgs = args.map(flattenTuplesAndBlocks(_, symbolOwner))
          target match { 
            case Select(leftOperand, name) if args.length == 1 =>
              NameTransformer.decode(name.toString) match {
                case op @ ("+" | "-" | "*" | "/" | "%" | "^" | "&" | "|" | "==" ) =>
                  Some(out("(", left, " ", op, " ", args(0), ")")
          */
          val FlatCode(defs1, stats1, vals1) = flattenTuplesAndBlocks(target, sideEffectFree = getType(target) != NoType)
          val argsConv = args.map(flattenTuplesAndBlocks(_))
          val tpes = flattenTypes(getType(tree))
          // TODO assign vals to new vars before the calls, to ensure a correct evaluation order !
          val res = FlatCode[Tree](
            defs1 ++ argsConv.flatMap(_.outerDefinitions),
            stats1 ++ argsConv.flatMap(_.statements),
            vals1.zip(argsConv.flatMap(_.values)).zip(tpes).map { 
              case ((v1, v2), tpe) => 
                setType(Apply(v1, List(v2)), tpe) 
            }
          )
          //println(s"CONVERTED apply $tree\n\tres = $res, \n\ttpes = $tpes, \n\targsConv = $argsConv, \n\tvals1 = $vals1")
          res
        case f @ DefDef(_, _, _, _, _, _) =>
          FlatCode[Tree](Seq(f), Seq(), Seq())
        case WhileLoop(condition, content) =>
          // TODO clean this up !!!
          val flatCondition = flattenTuplesAndBlocks(condition)
          val flatContent = content.map(flattenTuplesAndBlocks(_)).reduceLeft(_ >> _)
          val Seq(flatConditionValue) = flatCondition.values
          FlatCode[Tree](
            flatCondition.outerDefinitions ++ flatContent.outerDefinitions, 
            flatCondition.statements ++
            Seq(
              whileLoop(
                symbolOwner, 
                tree, 
                flatConditionValue,
                Block((flatContent.statements ++ flatContent.values :+ newUnit): _*)
              )
            ),
            Seq()
          )
        case If(condition, thenDo, otherwise) =>
          // val (a, b) = if ({ val d = 0 ; d != 0 }) (1, d) else (2, 0)
          // ->
          // val d = 0
          // val condition = d != 0
          // val a = if (condition) 1 else 2
          // val b = if (condition) d else 0
          val FlatCode(dc, sc, Seq(vc)) = flattenTuplesAndBlocks(condition)
          assert(getType(vc) != NoType, vc)
          val conditionVar = newVariable("condition", symbolOwner, tree.pos, false, vc)

          val fct @ FlatCode(Seq(), st, vt) = flattenTuplesAndBlocks(thenDo)
          val fco @ FlatCode(Seq(), so, vo) = flattenTuplesAndBlocks(otherwise)

          FlatCode[Tree](
            dc,
            sc :+ conditionVar.definition,
            (st, so) match {
              case (Seq(), Seq()) =>
                vt.zip(vo).map { case (t, o) => If(conditionVar(), t, o) } // pure (cond ? then : otherwise) form, possibly with tuple values
              case _ =>
                Seq(
                  If(
                    conditionVar(), 
                    Block((st.toList ++ vt.toList :+ newUnit): _*), 
                    Block((so.toList ++ vo.toList :+ newUnit): _*)
                  )
                )
            }
          )
        case Typed(expr, tpt) =>
          flattenTuplesAndBlocks(expr).mapValues(_.map(Typed(_, tpt)))
        case ValDef(_, _, _, _) if isTupleType(getType(tree)) =>
          // Skipping tuple definitions
          FlatCode[Tree](Seq(), Seq(), Seq())
        case ValDef(paramMods, paramName, tpt, rhs) =>
          val isVal = !paramMods.hasFlag(MUTABLE)
          // val p = {
          //   val x = 10
          //   (x, x + 2)
          // }
          // ->
          // val x = 10
          // val p_1 = x
          // val p_2 = x + 2
          val FlatCode(defs, stats, values) = flattenTuplesAndBlocks(replace(rhs))(tree.symbol)
          //val flatValues = values.flatMap(replaceValues)
          val tpe = getType(tree)
          val flattenedTypes = if (tpe != NoType) {
            val types = flattenTypes(tpe)
            assert(values.size == types.size, "values = " + values + ", flattenedTypes = " + types + ", tree = " + tree + ", tree.symbol = " + tree.symbol)
            types
          } else {
            values.map(getType _)
          }
          //for ((value, i) <- values.zipWithIndex; slice <- getTreeSlice(value)) {
          //  setSlice(tree, slice.subSlice(i, 1))
          //}
          //println(s"""tree = { $tree }: ${tpe}, flattenedTypes = $flattenedTypes""")
          if (flattenedTypes.size == 1) {
            //assert(values.size == 1, "values = " + values + ", flattenedTypes = " + flattenedTypes + ", tree = " + tree)
            val List(value) = values
            val vd = withSymbol(tree.symbol, tpe) {
              ValDef(paramMods, paramName, tpt, value)
            }
            //println("single vd = " + vd)
            //replace(value) // TODO REMOVE THIS DEBUG LINE
            FlatCode[Tree](
              defs,
              stats :+ vd,
              Seq()
            )
          } else {
            val splitSyms = //: Map[TupleSlice, (String, Symbol)] =
              flattenedTypes.zipWithIndex.map({ case (tpe, i) =>
                val name = newTermName(fresh(paramName + "_" + (i + 1)))
                val sym = setInfo(symbolOwner.newTermSymbol(name, tree.pos, LOCAL), tpe)
                (TupleSlice(tree.symbol, i, 1), (name, sym))//() => ident(sym, name))
              })

            //println("splitSyms = " + splitSyms)
            if (splitSyms.size != 1 && isVal)
              sliceReplacements ++= splitSyms.map { case (slice, (name, sym)) =>
                val rep = ident(sym, sym.typeSignature, name)//slice.toTreeGen(replace(_, false), tupleAnalyzer)()
                //println("New replacement for " + slice + " = " + rep + " (but name = " + name + ", sym = " + sym + ")")
                (slice, () => rep)
              }

            val FlatCode(defs, stats, flattenedValues) = flattenTuplesAndBlocks(rhs)(tree.symbol)
            
            if (isVal)
              for (((splitSlice, (splitName, splitSym)), value) <- splitSyms.zip(flattenedValues)) {
                for (slice <- getTreeSlice(value)) {
                  //println("Forwarded split slice " + slice + " from value " + value + " to split symbol " + splitSym)
                  setSlice(splitSym, slice)
                }
              }
              
            //println(s"""splitSyms = $splitSyms, flattenedValues = $flattenedValues, flattenedTypes = $flattenedTypes""")
          
            FlatCode[Tree](
              defs,
              stats ++ splitSyms.zip(flattenedValues).zip(flattenedTypes).map({ case (((slice, (name, sym)), value), tpe) =>
                val vv = value
                val vd = withSymbol(sym, tpe) {
                  ValDef(Modifiers(NoFlags), name, TypeTree(tpe), replace(value))
                }
                //println("vd = " + vd)
                vd
              }),
              Seq()//splitSyms.map({ case (slice, (name, sym)) => ident(sym, name) })
            )
          }
        case Match(selector, List(CaseDef(pat, guard, body))) =>
          def extract(tree: Tree): Tree = tree match {
            case Typed(expr, tpt) => extract(expr)
            case Annotated(annot, arg) => extract(arg)
            case _ => tree
          }
          getTreeSlice(selector, recursive = true).orElse(getTreeSlice(extract(selector), recursive = true)) match {
            case Some(slice) =>
              val subMatcher = new BoundTuple(slice)
              val subMatcher(m) = pat
              
              //val info = getTupleInfo(slice.baseSymbol.tpe)
              //if (false)
              for ((sym, subSlice) <- m) {
                val info = getTupleInfo(sym.typeSignature)
                val boundSlice = TupleSlice(sym, 0, info.flattenTypes.size)
                val rep = subSlice.toTreeGen(tupleAnalyzer)
                //setSlice(rep, subSlice)
                //println("Not Adding replacement for slice " + boundSlice + " to " + rep())
                //println("Adding replacement for slice " + boundSlice + " to " + subSlice)
                setSlice(sym, subSlice)
                                                //sliceReplacements ++= Seq(boundSlice -> rep)
                if (subSlice.sliceLength == 1)
                  sliceReplacements ++= Seq(boundSlice -> subSlice.toTreeGen(tupleAnalyzer))
                  //(() => applyFiberPath(subSlice.toTreeGen(replace), path)))
              }
              /*for ((path, i) <- info.flattenPaths.zipWithIndex) {
                val subSlice = TupleSlice(slice.baseSymbol, i, 1)
                sliceReplacements ++=
                  Seq(subSlice -> subSlice.toTreeGen(replace(_, false), tupleAnalyzer))
                  //(() => applyFiberPath(subSlice.toTreeGen(replace), path)))
              }*/
              
              flattenTuplesAndBlocks(body)
            case _ =>
              //println("selector: " + selector.getClass.getName + " = " + selector)// + " = " + nodeToString(selector))
              //println("selector.symbol = " + selector.symbol)
              //println("extract(selector): " + extract(selector).getClass.getName + " = " + extract(selector))// + " = " + nodeToString(extract(selector)))
              //println("extract(selector).symbol = " + extract(selector).symbol)
              //println("sliceReplacements = \n\t" + sliceReplacements.mkString("\n\t"))
              throw new RuntimeException("Unable to connect the matched pattern with its corresponding single case")
              //FlatCode[Tree](Seq(), Seq(), Seq())
          }
        case EmptyTree => {
          println("CodeFlattening  -  WARNING EmptyTree! Should this ever happen?")
          FlatCode[Tree](Seq(), Seq(), Seq())
        }
        case _ =>
          assert(false, "Case not handled in tuples and blocks flattening : " + tree + ": " + tree.getClass.getName)// + ") :\n\t" + nodeToString(tree))
          FlatCode[Tree](Seq(), Seq(), Seq())
      }
      //res.printDebug("res")
      val ret = if (sideEffectFree)
        makeValuesSideEffectFree(
          res,
          symbolOwner
        )
      else
        res

      //ret.printDebug("ret")
      //println("tree = \n\t" + tree.toString.replaceAll("\n", "\n\t"))
      //println("res = \n\t" + res.toString.replaceAll("\n", "\n\t"))
      //println("ret = \n\t" + ret.toString.replaceAll("\n", "\n\t"))
      
      val out = ret.mapValues(s => s.flatMap(replaceValues))
      //out.printDebug("out")
      out
    }
  }
}
