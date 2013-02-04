/*
 * Created by IntelliJ IDEA.
 * User: ochafik
 * Date: 10/05/11
 * Time: 15:20
 */
package scalaxy.common

import scala.reflect.api.Universe

trait StreamSources
extends Streams
with StreamSinks
with CommonScalaNames 
{
  val global: Universe
  import global._
  import definitions._

  trait AbstractArrayStreamSource extends StreamSource {
    def tree: Tree
    def array: Tree
    def componentType: Type
    
    override def unwrappedTree = array
    override def privilegedDirection = None
    def emit(direction: TraversalDirection)(implicit loop: Loop) = {
      import loop.{ currentOwner, transform }
      val pos = array.pos

      val skipFirst = false // TODO
      val reverseOrder = direction == FromRight

      val aVar = newVariable("array$", currentOwner, pos, false, transform(array))
      val nVar = newVariable("n$", currentOwner, pos, false, newArrayLength(aVar()))
      val iVar = newVariable("i$", currentOwner, pos, true,
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
        
      val itemVar = newVariable("item$", currentOwner, pos, false, newApply(pos, aVar(), iVar()))
        
      loop.preOuter += aVar.definition
      loop.preOuter += nVar.definition
      loop.preOuter += iVar.definition
      loop.tests += (
        if (reverseOrder)
          binOp(iVar(), IntTpe.member(GT), newInt(0))
        else
          binOp(iVar(), IntTpe.member(LT), nVar())
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
  case class WrappedArrayStreamSource(tree: Tree, array: Tree, componentType: Type) 
  extends AbstractArrayStreamSource 
  with CanCreateArraySink
  with SideEffectFreeStreamComponent
  {
    override def isResultWrapped = true 
  }
  
  abstract class ExplicitCollectionStreamSource(val tree: Tree, items: List[Tree], val componentType: Type) 
  extends AbstractArrayStreamSource {
    val array = newArrayApply(newTypeTree(componentType), items:_*)
    
    override def analyzeSideEffectsOnStream(analyzer: SideEffectsAnalyzer) =
      analyzer.analyzeSideEffects(tree, items:_*)
  }
  case class ListStreamSource(tree: Tree, componentType: Type) 
  extends StreamSource 
  with CanCreateListSink
  with SideEffectFreeStreamComponent {
    val list = tree // TODO 
      
    override def unwrappedTree = list
    override def privilegedDirection = Some(FromLeft)
    def emit(direction: TraversalDirection)(implicit loop: Loop) = {
      import loop.{ currentOwner, transform }
      assert(direction == FromLeft)
      
      val pos = list.pos

      val skipFirst = false // TODO
      val colTpe = list.tpe
      
      val aVar = newVariable("list$", currentOwner, pos, true, transform(list))
      val itemVar = newVariable("item$", currentOwner, pos, false, newSelect(aVar(), headName))
      
      loop.preOuter += aVar.definition
      loop.tests += (
        if ("1" == System.getenv("SCALACL_LIST_TEST_ISEMPTY")) // Safer, but 10% slower
          boolNot(newSelect(aVar(), isEmptyName))
        else
          newIsInstanceOf(aVar(), appliedType(NonEmptyListClass.asType.toType.typeConstructor, List(componentType)))
      )
      
      loop.preInner += itemVar.definition
      loop.postInner += (
        typeCheck(
          Assign(
            aVar(),
            newSelect(aVar(), tailName)
          ),
          UnitTpe
        )
      )
      new StreamValue(itemVar)
    }
  }
  
  case class RangeStreamSource(tree: Tree, from: Tree, to: Tree, byValue: Int, isUntil: Boolean) 
  extends StreamSource 
  with CanCreateVectorSink
  with SideEffectFreeStreamComponent {
    override def privilegedDirection = Some(FromLeft)

    def emit(direction: TraversalDirection)(implicit loop: Loop) = {
      assert(direction == FromLeft)
      import loop.{ currentOwner, transform }
      val pos = tree.pos
      
      val fromVar = newVariable("from$", currentOwner, tree.pos, false, typeCheck(transform(from), IntTpe))
      val toVar = newVariable("to$", currentOwner, tree.pos, false, typeCheck(transform(to), IntTpe))
      val itemVar = newVariable("item$", currentOwner, tree.pos, true, fromVar())
      val itemVal = newVariable("item$val$", currentOwner, tree.pos, false, itemVar())
      
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
      val sizeVal = newVariable("outputSize$", currentOwner, tree.pos, false, size)
      val iVar = newVariable("outputIndex$", currentOwner, tree.pos, true, newInt(0))//if (reverseOrder) intSub(outputSizeVar(), newInt(1)) else newInt(0))
      val iVal = newVariable("i", currentOwner, tree.pos, true, iVar())//if (reverseOrder) intSub(outputSizeVar(), newInt(1)) else newInt(0))

      loop.preOuter += fromVar.definition
      loop.preOuter += toVar.definition
      loop.preOuter += itemVar.definition
      loop.preOuter += (sizeVal.defIfUsed _)
      loop.preOuter += (() => if (iVal.identUsed) Some(iVar.definition) else None)
      loop.tests += (
        binOp(
          itemVar(),
          IntTpe.member(
            if (isUntil) {
              if (byValue < 0) GT else LT
            } else {
              if (byValue < 0) GE else LE
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
        value = itemVal,
        valueIndex = Some(iVal),
        valuesCount = Some(sizeVal)
      )
    }
  }
  case class OptionStreamSource(tree: Tree, componentOption: Option[Tree], onlyIfNotNull: Boolean, componentType: Type) 
  extends StreamSource 
  with CanCreateOptionSink
  with SideEffectFreeStreamComponent 
  {
    def emit(direction: TraversalDirection)(implicit loop: Loop) = {
      import loop.{ currentOwner, transform }
      val pos = tree.pos
      
      loop.isLoop = false
      
      val (valueVar: VarDef, isDefinedVar: VarDef, isAlwaysDefined: Boolean) = componentOption match {
        case Some(component) =>
          val valueVar = newVariable("value$", currentOwner, pos, false, transform(component))
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
          val isDefinedVar = newVariable("isDefined$", currentOwner, pos, false, isDefinedValue)
          loop.preOuter += valueVar.definition
          (valueVar, isDefinedVar, isAlwaysDefined)
        case None =>
          val optionVar = newVariable("option$", currentOwner, pos, false, transform(tree))
          val isDefinedVar = newVariable("isDefined$", currentOwner, pos, false, newSelect(optionVar(), N("isDefined")))
          val valueVar = newVariable("value$", currentOwner, pos, false, newSelect(optionVar(), N("get")))
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
  
  case class ArrayApplyStreamSource(override val tree: Tree, components: List[Tree], override val componentType: Type) 
  extends ExplicitCollectionStreamSource(tree, components, componentType) 
  with CanCreateArraySink
  {
    override def isResultWrapped = false 
  }
  
  case class SeqApplyStreamSource(override val tree: Tree, components: List[Tree], override val componentType: Type) 
  extends ExplicitCollectionStreamSource(tree, components, componentType) with CanCreateListSink // default Seq implementation is List
  
  case class IndexedSeqApplyStreamSource(override val tree: Tree, components: List[Tree], override val componentType: Type) 
  extends ExplicitCollectionStreamSource(tree, components, componentType) with CanCreateVectorSink // default IndexedSeq implementation is Vector
  
  case class ListApplyStreamSource(override val tree: Tree, components: List[Tree], override val componentType: Type) 
  extends ExplicitCollectionStreamSource(tree, components, componentType) with CanCreateListSink
  
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
        new ArrayApplyStreamSource(tree, components, componentType)
      case SeqApply(components, componentType) =>
        new SeqApplyStreamSource(tree, components, componentType)
      case IndexedSeqApply(components, componentType) =>
        new IndexedSeqApplyStreamSource(tree, components, componentType)
      case ListApply(components, componentType) =>
        new ListApplyStreamSource(tree, components, componentType)
      case WrappedArrayTree(array, componentType) =>
        WrappedArrayStreamSource(tree, array, componentType)
      case ListTree(componentType) =>
        ListStreamSource(tree, componentType)
      case TreeWithType(_, TypeRef(_, c, List(componentType))) if c == ListClass | c == ImmutableListClass =>
        ListStreamSource(tree, componentType)
      case OptionApply(List(component), componentType) =>
        OptionStreamSource(tree, Some(component), onlyIfNotNull = true, component.tpe)
      case OptionTree(componentType) =>
        OptionStreamSource(tree, None, onlyIfNotNull = true, componentType)
      case IntRange(from, to, By(byValue), isUntil, filters) =>
        assert(filters.isEmpty, "Filters are not empty !!!")
        RangeStreamSource(tree, from, to, byValue, isUntil/*, filters*/)
    }
  }
}
