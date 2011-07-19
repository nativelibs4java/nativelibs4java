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
  this: PluginComponent with WithOptions =>

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
  trait CanCreateArraySink extends CanCreateStreamSink {
    def tree: Tree
    override def createStreamSink(componentType: Type, outputSize: Option[IdentGen]): StreamSink = new StreamSink {
      override def tree = CanCreateArraySink.this.tree
      def output(value: StreamValue)(implicit loop: Loop): Unit = {
        import loop.{ unit, currentOwner }
        val pos = loop.pos

        val Some(index) = value.valueIndex
        val Some(size) = value.valuesCount

        val a = newVariable(unit, "out", currentOwner, pos, false, newArray(componentType, size()))
        loop.preOuter += a.definition
        loop.inner += newUpdate(pos, a(), index(), itemIdentGen(value)(loop)())
        
        tree.tpe match {
          case TypeRef(_, ArrayClass, List(_)) =>
            println("TREE TPE IS OK : " + tree.tpe)
            loop.postOuter += a()
          case _ =>
            val opsType = getArrayWrapperTpe(componentType)
            
            val sym = opsType.typeSymbol.primaryConstructor
            val newWrapper = typed { 
              Apply(
                Select(
                  New(TypeTree(opsType)),
                  sym
                ).setSymbol(sym),
                List(a())
              )
            }
              
            loop.postOuter += newWrapper
        }

        /*
        def doIt(createBuilder: TreeGen, builderAppend: (IdentGen, IdentGen) => Tree, builderResult: IdentGen => Tree) = {
          val a = newVariable(unit, "out", owner, NoPosition, false, newArray(componentType, size()))
          loop.preOuter(a.definition)
          loop.postInner(builderAppend(a(), value))
          loop.postOuter(a())
        }
        
        outputSize match {
          case Some(size) =>
            doIt(newArray(componentType, size()), out => out)
          case None =>
            if (componentType <:< AnyRefClass.tpe)
              doIt(appliedType(RefArrayBuilderClass.tpe, List(componentType)), simpleBuilderResult _)
            else
              doIt(appliedType(ArrayBufferClass.tpe, List(componentType)), toArray(tree, componentType, localTyper))
        }*/
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
      loop.loopTests += (
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
      new SimpleStreamValue(
        itemVar,
        Some(iVar),
        Some(nVar)
      )
    }
  }

  trait CanCreateVectorSink extends CanCreateStreamSink {
    def tree: Tree
    override def createStreamSink(componentType: Type, outputSize: Option[IdentGen]): StreamSink = new StreamSink {
      override def tree = CanCreateVectorSink.this.tree
      def output(value: StreamValue)(implicit loop: Loop): Unit = {
        import loop.{ unit, currentOwner }
        val pos = loop.pos

        val builderType = appliedType(VectorBuilderClass.tpe, List(componentType))
        val sym = builderType.typeSymbol.primaryConstructor

        val addAssignMethod = (builderType member addAssignName).alternatives.head// filter (_.paramss.size == 1)
        val builderVar = newVariable(unit, "out", currentOwner, pos, false,
          typed {
            Apply(
              Select(
                New(TypeTree(builderType)),
                sym
              ).setSymbol(sym),
              Nil
            ).setSymbol(sym)
          }
        )
        loop.preOuter += builderVar.definition
        loop.postInner += typed {
          Apply(
            Select(
              builderVar(),
              addAssignName
            ).setSymbol(addAssignMethod).setType(addAssignMethod.tpe),
            List(itemIdentGen(value)(loop)())
          ).setSymbol(addAssignMethod).setType(UnitClass.tpe)
        }
        loop.postOuter += simpleBuilderResult(builderVar())
      }
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
      loop.loopTests += (
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
      
      new SimpleStreamValue(
        itemVar,
        Some(iVal),
        Some(sizeVal)
      )
    }
  }
  object StreamSource {

    //TODO def unapply(tpe: Type): Option[CollectionRewriter] = tpe.dealias.deconst match {
    //  case ListTree(componentType)
    //}
    def unapply(tree: Tree): Option[StreamSource] = tree match {
      case ArrayTree(array, componentType) =>
        Some(new ArrayStreamSource(tree, array, componentType))//ArrayRewriter, appliedType(ArrayClass.tpe, List(componentType)), array, componentType))
      //case ListTree(componentType) if options.deprecated =>
      //  Some(new CollectionRewriter(ListRewriter, appliedType(ListClass.tpe, List(componentType)), tree, componentType))
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