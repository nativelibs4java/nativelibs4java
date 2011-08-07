/*
 * Created by IntelliJ IDEA.
 * User: ochafik
 * Date: 10/05/11
 * Time: 15:20
 */
package scalacl ; package plugin

import tools.nsc.plugins.PluginComponent
import tools.nsc.Global

trait StreamImpls extends Streams {
  this: PluginComponent with WithOptions with WorkaroundsForOtherPhases =>

  val global: Global
  import global._
  import definitions._
  import treeInfo.{ methPart }
  import typer.typed

  def itemIdentGen(value: StreamValue)(implicit loop: Loop): IdentGen =
      value.value.tuple(loop.innerContext)

  def getArrayWrapperTpe(componentType: Type) = {
    primArrayOpsClasses.get(componentType) match {
      case Some(t) =>
        t.tpe
      case None =>
        assert(componentType <:< AnyRefClass.tpe)
        appliedType(RefArrayOpsClass.tpe, List(componentType))
    }
  }
  trait WithResultWrapper {
    def wrapResultIfNeeded(result: Tree, expectedType: Type, componentType: Type): Tree = result
  }
  trait WithArrayResultWrapper extends WithResultWrapper {
    def isArrayType(tpe: Type) = {
      tpe match {
        case TypeRef(_, ArrayClass, List(_)) => true
        case _ => false
      }
    }
      
    override def wrapResultIfNeeded(result: Tree, expectedType: Type, componentType: Type) = {
      if (isArrayType(expectedType) || !isArrayType(result.tpe)) {
        //println("TREE TPE IS OK : " + expectedType)
        result
      } else {
        //println("TREE TPE NEEDS ARRAY WRAPPER : " + expectedType)
        val opsType = getArrayWrapperTpe(componentType)
        newInstance(opsType, List(result))
      }
    }
  }
  trait ArrayStreamSink extends WithArrayResultWrapper {
    def tree: Tree
    def outputArray(tree: Tree, value: StreamValue, index: TreeGen, size: TreeGen)(implicit loop: Loop): Unit = {
      import loop.{ unit, currentOwner }
      val pos = loop.pos

      val componentType = value.tpe
      val a = newVariable(unit, "out", currentOwner, pos, false, newArray(componentType, size()))
      loop.preOuter += a.definition
      loop.inner += newUpdate(pos, a(), index(), itemIdentGen(value)(loop)())
      
      loop.postOuter += wrapResultIfNeeded(a(), tree.tpe, componentType)
    }
    def output(value: StreamValue)(implicit loop: Loop): Unit = {
      val Some(index) = value.valueIndex
      val Some(size) = value.valuesCount
      outputArray(tree, value, index, size)
    }
  }
  class ArrayBuilderGen(componentType: Type, localTyper: analyzer.Typer) extends BuilderGen {
    val (builderType, mainArgs, needsManifest, manifestIsInMain, _builderResultGetter) = {
      primArrayBuilderClasses.get(componentType) match {
        case Some(t) =>
          (t.tpe, Nil, false, false, simpleBuilderResult _)
        case None =>
          if (componentType <:< AnyRefClass.tpe)
            (appliedType(RefArrayBuilderClass.tpe, List(componentType)), Nil, true, false, simpleBuilderResult _)
          else
            (appliedType(ArrayBufferClass.tpe, List(componentType)), List(newInt(16)), false, false, (tree: Tree) => {
              toArray(tree, componentType, localTyper)
            })
      }
    }
    override def builderResultGetter = _builderResultGetter
    
    override def builderCreation = localTyper.typed {
      val manifestList = if (needsManifest) {
        var t = componentType
        
        var manifest = localTyper.findManifest(t, false).tree
        if (manifest == EmptyTree)
          manifest = localTyper.findManifest(t.dealias.deconst.widen, false).tree // TODO remove me ?
        assert(manifest != EmptyTree, "Empty manifest for type : " + t + " = " + t.dealias.deconst.widen)
    
        // TODO: REMOVE THIS UGLY WORKAROUND !!!
        assertNoThisWithNoSymbolOuterRef(manifest, localTyper)
        List(manifest)
      } else
        null

      val args = if (needsManifest && manifestIsInMain)
        manifestList
      else
        mainArgs
        
      val n = newInstance(builderType, args)
      if (needsManifest && !manifestIsInMain)
        Apply(
          n,
          manifestList
        ).setSymbol(n.symbol)
      else
        n
    }
  }
  abstract class DefaultBuilderGen(rawBuilderSym: Symbol, componentType: Type) extends BuilderGen {
    val builderType = appliedType(rawBuilderSym.tpe, List(componentType))
    override def builderCreation =
      newInstance(builderType, Nil)
  }
  class ListBuilderGen(componentType: Type) extends DefaultBuilderGen(ListBufferClass, componentType)
  class VectorBuilderGen(componentType: Type) extends DefaultBuilderGen(VectorBuilderClass, componentType)
  
  trait BuilderGen {
    //def builderType: Type
    def builderResultGetter: Tree => Tree =
      simpleBuilderResult _
      
    //def builderNeedsManifest: Boolean
    //def builderManifest
    def builderCreation: Tree
    def builderAppend: (Tree, Tree) => Tree =
      addAssign(_, _)
  }
  trait BuilderStreamSink extends WithResultWrapper {
    def tree: Tree
    def createBuilderGen(value: StreamValue)(implicit loop: Loop): BuilderGen
    def outputBuilder(value: StreamValue)(implicit loop: Loop): Unit = {
      import loop.{ unit, currentOwner }
      val pos = loop.pos

      val builderGen = createBuilderGen(value)
      import builderGen._
      
      val a = newVariable(unit, "out", currentOwner, pos, false, builderCreation)
      loop.preOuter += a.definition
      loop.inner += builderAppend(a(), value.value())
      
      loop.postOuter += wrapResultIfNeeded(builderResultGetter(a()), tree.tpe, value.tpe)
    }
  }
  trait ArrayBuilderStreamSink extends BuilderStreamSink with WithArrayResultWrapper {
    def createBuilderGen(value: StreamValue)(implicit loop: Loop): BuilderGen =
      new ArrayBuilderGen(value.tpe, loop.localTyper)
      
    def output(value: StreamValue)(implicit loop: Loop): Unit = {
      outputBuilder(value)
    }
  }
  trait CanCreateArraySink extends CanCreateStreamSink {
    def tree: Tree
    
    override def createStreamSink(componentType: Type, outputSize: Option[TreeGen]): StreamSink = 
      new StreamSink 
      with ArrayStreamSink 
      with ArrayBuilderStreamSink
      {
        override def tree = CanCreateArraySink.this.tree
        
        override def output(value: StreamValue)(implicit loop: Loop): Unit = {
          (value.valueIndex, value.valuesCount) match {
            case (Some(index), Some(size)) =>
              outputArray(tree, value, index, size)
            case _ =>
              outputBuilder(value)
          }
        }
      }
  }
  trait CanCreateVectorSink extends CanCreateStreamSink {
    def tree: Tree
    
    override def createStreamSink(componentType: Type, outputSize: Option[TreeGen]): StreamSink = 
      new StreamSink 
      with BuilderStreamSink 
      {
        override def tree = CanCreateVectorSink.this.tree
      
        def createBuilderGen(value: StreamValue)(implicit loop: Loop): BuilderGen =
          new VectorBuilderGen(value.tpe)
          
        def output(value: StreamValue)(implicit loop: Loop): Unit =
          outputBuilder(value)
      }
  }
  trait CanCreateOptionSink extends CanCreateStreamSink {
    def tree: Tree
    
    override def createStreamSink(componentType: Type, outputSize: Option[TreeGen]): StreamSink = 
      new StreamSink 
      {
        override def tree = CanCreateOptionSink.this.tree
      
        //def createBuilderGen(value: StreamValue)(implicit loop: Loop): BuilderGen =
        //  new VectorBuilderGen(value.tpe)
          
        def output(value: StreamValue)(implicit loop: Loop): Unit = {
          import loop.{ unit, currentOwner }
          val pos = loop.pos
    
          val out = newVariable(unit, "out", currentOwner, pos, mutable = true, newNull(value.tpe))
          val presence = newVariable(unit, "hasOut", currentOwner, pos, mutable = true, newBool(false))
          loop.preOuter += out.definition
          loop.preOuter += presence.definition
          loop.inner += newAssign(out, value.value())
          loop.inner += newAssign(presence, newBool(true))
          
          loop.postOuter += typed {
            If(
              presence(),
              newSomeApply(value.tpe, out()),
              newNoneModuleTree.setType(tree.tpe)
            ).setType(UnitClass.tpe)
          }
        }
      }
  }
  case class ArrayStreamSource(tree: Tree, array: Tree, componentTpe: Type) extends StreamSource with CanCreateArraySink {
    override def unwrappedTree = array
    override def privilegedDirection = None
    //println("ArrayStreamSource with tree = " + tree + " and array = 
    def emit(direction: TraversalDirection, transform: Tree => Tree)(implicit loop: Loop) = {
      import loop.{ unit, currentOwner }
      val pos = array.pos

      val skipFirst = false // TODO
      val reverseOrder = direction == FromRight

      val aVar = newVariable(unit, "array$", currentOwner, pos, false, transform(array))
      val nVar = newVariable(unit, "n$", currentOwner, pos, false, newArrayLength(aVar()))
      val iVar = newVariable(unit, "i$", currentOwner, pos, true,
        if (reverseOrder) {
          if (skipFirst)
            intSub(nVar(), newInt(1))
          else
            nVar()
        } else {
          if (skipFirst)
            newInt(1)
          else
            newInt(0)
        }
      )
        
      val itemVar = newVariable(unit, "item$", currentOwner, pos, false, newApply(pos, aVar(), iVar()))
        
      loop.preOuter += aVar.definition
      loop.preOuter += nVar.definition
      loop.preOuter += iVar.definition
      loop.tests += (
        if (reverseOrder)
          binOp(iVar(), IntClass.tpe.member(nme.GT), newInt(0))
        else
          binOp(iVar(), IntClass.tpe.member(nme.LT), nVar())
      )
      
      loop.preInner += itemVar.definition
      loop.postInner += (
        if (reverseOrder)
          decrementIntVar(iVar, newInt(1))
        else
          incrementIntVar(iVar, newInt(1))
      )
      new StreamValue(
        value = itemVar,
        valueIndex = Some(iVar),
        valuesCount = Some(nVar)
      )
    }
  }
  
  case class RangeStreamSource(tree: Tree, from: Tree, to: Tree, byValue: Int, isUntil: Boolean) extends StreamSource with CanCreateVectorSink {
    override def privilegedDirection = Some(FromLeft)

    def emit(direction: TraversalDirection, transform: Tree => Tree)(implicit loop: Loop) = {
      assert(direction == FromLeft)
      import loop.{ unit, currentOwner }
      val pos = tree.pos
      
      val fromVar = newVariable(unit, "from$", currentOwner, tree.pos, false, transform(from).setType(IntClass.tpe))
      val toVar = newVariable(unit, "to$", currentOwner, tree.pos, false, transform(to).setType(IntClass.tpe))
      val itemVar = newVariable(unit, "item$", currentOwner, tree.pos, true, fromVar())
      val itemVal = newVariable(unit, "item$val$", currentOwner, tree.pos, false, itemVar())
      
      val size = {
        val span = intSub(toVar(), fromVar())
        val width = if (isUntil) 
          span
        else
          intAdd(span, newInt(1))
        
        if (byValue == 1)
          width
        else
          intDiv(width, newInt(byValue))
      }
      val sizeVal = newVariable(unit, "outputSize$", currentOwner, tree.pos, false, size)
      val iVar = newVariable(unit, "outputIndex$", currentOwner, tree.pos, true, newInt(0))//if (reverseOrder) intSub(outputSizeVar(), newInt(1)) else newInt(0))
      val iVal = newVariable(unit, "i", currentOwner, tree.pos, true, iVar())//if (reverseOrder) intSub(outputSizeVar(), newInt(1)) else newInt(0))

      loop.preOuter += fromVar.definition
      loop.preOuter += toVar.definition
      loop.preOuter += itemVar.definition
      loop.preOuter += (sizeVal.defIfUsed _)
      loop.preOuter += (iVar.defIfUsed _)
      loop.tests += (
        binOp(
          itemVar(),
          IntClass.tpe.member(
            if (isUntil) {
              if (byValue < 0) nme.GT else nme.LT
            } else {
              if (byValue < 0) nme.GE else nme.LE
            }
          ),
          toVar()
        )
      )
      loop.preInner += itemVal.definition // it's important to keep a non-mutable local reference !
      loop.preInner += (iVal.defIfUsed _)
      
      loop.postInner += incrementIntVar(itemVar, newInt(byValue))
      loop.postInner += (() => iVal.ifUsed { incrementIntVar(iVar, newInt(1)) })
      
      new StreamValue(
        value = itemVar,
        valueIndex = Some(iVal),
        valuesCount = Some(sizeVal)
      )
    }
  }
  case class OptionStreamSource(tree: Tree, componentOption: Option[Tree], onlyIfNotNull: Boolean, componentType: Type) 
  extends StreamSource 
  with CanCreateOptionSink 
  {
    def emit(direction: TraversalDirection, transform: Tree => Tree)(implicit loop: Loop) = {
      import loop.{ unit, currentOwner }
      val pos = tree.pos
      
      loop.isLoop = false
      
      val (valueVar: VarDef, isDefinedVar: VarDef, isAlwaysDefined: Boolean) = componentOption match {
        case Some(component) =>
          val valueVar = newVariable(unit, "value$", currentOwner, pos, false, transform(component))
          val (isDefinedValue, isAlwaysDefined) = 
            if (onlyIfNotNull && !isAnyVal(component.tpe)) 
              component match {
                case Literal(Constant(v)) =>
                  val isAlwaysDefined = v != null
                  (newBool(isAlwaysDefined), isAlwaysDefined)
                case _ =>
                  (newIsNotNull(valueVar()), false)
              }
            else 
              (newBool(true), true)
          val isDefinedVar = newVariable(unit, "isDefined$", currentOwner, pos, false, isDefinedValue)
          loop.preOuter += valueVar.definition
          (valueVar, isDefinedVar, isAlwaysDefined)
        case None =>
          val optionVar = newVariable(unit, "option$", currentOwner, pos, false, transform(tree))
          val isDefinedVar = newVariable(unit, "isDefined$", currentOwner, pos, false, newSelect(optionVar(), N("isDefined")))
          val valueVar = newVariable(unit, "value$", currentOwner, pos, false, newSelect(optionVar(), N("get")))
          loop.preOuter += optionVar.definition
          loop.preInner += valueVar.definition
          (valueVar, isDefinedVar, false)
      }
      if (!isAlwaysDefined) {
        loop.preOuter += isDefinedVar.definition
        loop.tests += isDefinedVar()
      } else
        loop.tests += newBool(true)
      
      new StreamValue(
        value = valueVar,
        valueIndex = Some(() => newInt(0)),
        valuesCount = Some(() => typed {
          if (isAlwaysDefined)
            newInt(1)
          else
            If(isDefinedVar(), newInt(1), newInt(0)) 
        })
      )
    }
  }
  object StreamSource {

    //TODO def unapply(tpe: Type): Option[CollectionRewriter] = tpe.dealias.deconst match {
    //  case ListTree(componentType)
    //}
    def unapply(tree: Tree): Option[StreamSource] = tree match {
      case ArrayTree(array, componentType) =>
        Some(ArrayStreamSource(tree, array, componentType))//ArrayRewriter, appliedType(ArrayClass.tpe, List(componentType)), array, componentType))
      //case ListTree(componentType) if options.deprecated =>
      //  Some(new CollectionRewriter(ListRewriter, appliedType(ListClass.tpe, List(componentType)), tree, componentType))
      case OptionApply(component) =>
        //println("Found option apply : " + tree)
        Some(OptionStreamSource(tree, Some(component), onlyIfNotNull = true, component.tpe))
      case OptionTree(componentType) =>
        //println("Found option tree : " + tree)
        Some(OptionStreamSource(tree, None, onlyIfNotNull = true, componentType))
      case IntRange(from, to, by, isUntil, filters) =>
        (
          by match {
            case None =>
              Some(1)
            case Some(Literal(Constant(v: Int))) =>
              Some(v)
            case _ =>
              None
          }
        ) match {
          case Some(byValue) =>
            Some(RangeStreamSource(tree, from, to, byValue, isUntil/*, filters*/))//, appliedType(ArrayClass.tpe, List(IntClass.tpe)), null, IntClass.tpe))
          case _ =>
            None
        }
      case _ =>
        None
    }
  }
}