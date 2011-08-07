/*
 * Created by IntelliJ IDEA.
 * User: ochafik
 * Date: 10/05/11
 * Time: 15:20
 */
package scalacl ; package plugin

import tools.nsc.plugins.PluginComponent
import tools.nsc.Global

trait StreamSources extends Streams with StreamSinks {
  this: PluginComponent with WithOptions with WorkaroundsForOtherPhases =>

  val global: Global
  import global._
  import definitions._
  import treeInfo.{ methPart }
  import typer.typed

  trait AbstractArrayStreamSource extends StreamSource {
    def tree: Tree
    def array: Tree
    def componentType: Type
    
    override def unwrappedTree = array
    override def privilegedDirection = None
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
  case class ArrayStreamSource(tree: Tree, array: Tree, componentType: Type) 
  extends AbstractArrayStreamSource with CanCreateArraySink
  
  abstract class ExplicitCollectionStreamSource(val tree: Tree, items: List[Tree], val componentType: Type) 
  extends AbstractArrayStreamSource {
    val array = newArrayApply(newTypeTree(componentType), items:_*)
  }
  case class ListStreamSource(tree: Tree, componentType: Type) extends StreamSource with CanCreateListSink {
    val list = tree // TODO 
      
    override def unwrappedTree = list
    override def privilegedDirection = Some(FromLeft)
    def emit(direction: TraversalDirection, transform: Tree => Tree)(implicit loop: Loop) = {
      import loop.{ unit, currentOwner }
      assert(direction == FromLeft)
      
      val pos = list.pos

      val skipFirst = false // TODO
      val colTpe = list.tpe
      
      val aVar = newVariable(unit, "list$", currentOwner, pos, true, transform(list))
      val itemVar = newVariable(unit, "item$", currentOwner, pos, false, newSelect(aVar(), headName))
      
      loop.preOuter += aVar.definition
      loop.tests += (
        if ("1" == System.getenv("SCALACL_LIST_TEST_ISEMPTY")) // Safer, but 10% slower
          boolNot(newSelect(aVar(), isEmptyName))
        else
          newIsInstanceOf(aVar(), appliedType(NonEmptyListClass.typeConstructor, List(componentType)))
      )
      
      loop.preInner += itemVar.definition
      loop.postInner += (
        Assign(
          aVar(),
          newSelect(aVar(), tailName)
        ).setType(UnitClass.tpe)
      )
      new StreamValue(itemVar)
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
    object By {
      def unapply(treeOpt: Option[Tree]) = treeOpt match {
        case None =>
          Some(1)
        case Some(Literal(Constant(v: Int))) =>
          Some(v)
        case _ =>
          None
      }
    }
    def unapply(tree: Tree): Option[StreamSource] = Option(tree) collect {
      case ArrayApply(components, componentType) =>
        new ExplicitCollectionStreamSource(tree, components, componentType) with CanCreateArraySink
      case SeqApply(components, componentType) =>
        new ExplicitCollectionStreamSource(tree, components, componentType) with CanCreateListSink
      case ListApply(components, componentType) =>
        new ExplicitCollectionStreamSource(tree, components, componentType) with CanCreateListSink
      case ArrayTree(array, componentType) =>
        ArrayStreamSource(tree, array, componentType)
      case ListTree(componentType) =>
        ListStreamSource(tree, componentType)
      case OptionApply(List(component), componentType) =>
        OptionStreamSource(tree, Some(component), onlyIfNotNull = true, component.tpe)
      case OptionTree(componentType) =>
        OptionStreamSource(tree, None, onlyIfNotNull = true, componentType)
      case IntRange(from, to, By(byValue), isUntil, filters) =>
        RangeStreamSource(tree, from, to, byValue, isUntil/*, filters*/)
    }
  }
}
