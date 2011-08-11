/*
 * Created by IntelliJ IDEA.
 * User: ochafik
 * Date: 10/05/11
 * Time: 15:20
 */
package scalacl ; package plugin

import tools.nsc.plugins.PluginComponent
import tools.nsc.Global

trait StreamSinks extends Streams {
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
      typed { result }
      if (!isArrayType(expectedType) && isArrayType(result.tpe)) {
        //println("TREE TPE IS OK : expectedType " + expectedType + ", got " + result.tpe)
        result
      } else {
        //println("TREE TPE NEEDS ARRAY WRAPPER : expected " + expectedType + ", got " + result.tpe)
        val opsType = getArrayWrapperTpe(componentType)
        newInstance(opsType, List(result))
      }
    }
  }
  trait ArrayStreamSink extends WithArrayResultWrapper {
    def tree: Tree
    def outputArray(expectedType: Type, value: StreamValue, index: TreeGen, size: TreeGen)(implicit loop: Loop): Unit = {
      import loop.{ unit, currentOwner }
      val pos = loop.pos

      val componentType = value.tpe
      val hasExtraValue = value.extraFirstValue.isDefined
      val a = newVariable(unit, "out", currentOwner, pos, false, 
        newArray(
          componentType, 
          if (hasExtraValue)
            intAdd(size(), newInt(1))
          else
            size()
        )
      )
      loop.preOuter += a.definition
      for (v <- value.extraFirstValue)
        loop.preOuter += newUpdate(pos, a(), newInt(0), v()) 
      
      loop.inner += newUpdate(
        pos, 
        a(), 
        if (hasExtraValue)
          intAdd(index(), newInt(1))
        else
          index(), 
        itemIdentGen(value)(loop)()
      )
      
      loop.postOuter += 
        wrapResultIfNeeded(a(), expectedType, componentType)
    }
    def output(value: StreamValue, expectedType: Type)(implicit loop: Loop): Unit = {
      val Some(index) = value.valueIndex
      val Some(size) = value.valuesCount
      outputArray(expectedType, value, index, size)
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
  class SetBuilderGen(componentType: Type) extends BuilderGen {
    private val setClass = SetClass
    private val setModule = SetModule
    
    private val setType = appliedType(setClass.tpe, List(componentType))
    val builderType = appliedType(SetBuilderClass.tpe, List(componentType, setType))
    override def builderCreation =
      newInstance(builderType, List(newApply(newSetModuleTree, applyName, List(newTypeTree(componentType)), Nil)))
  }
  
  trait BuilderGen {
    def builderResultGetter: Tree => Tree =
      simpleBuilderResult _
      
    def builderCreation: Tree
    def builderAppend: (Tree, Tree) => Tree =
      addAssign(_, _)
  }
  trait BuilderStreamSink extends WithResultWrapper {
    def tree: Tree
    //def privilegedDirection = Some(FromLeft)
    def createBuilderGen(value: StreamValue)(implicit loop: Loop): BuilderGen
    def outputBuilder(expectedType: Type, value: StreamValue)(implicit loop: Loop): Unit = {
      import loop.{ unit, currentOwner }
      val pos = loop.pos

      //if (direction != FromLeft)
      //  throw new UnsupportedOperationException("TODO")
        
      val builderGen = createBuilderGen(value)
      import builderGen._
      
      val a = newVariable(unit, "out", currentOwner, pos, false, builderCreation)
      loop.preOuter += a.definition
      for (v <- value.extraFirstValue)
        loop.preOuter += builderAppend(a(), v())
        
      loop.inner += builderAppend(a(), value.value())
      
      loop.postOuter += wrapResultIfNeeded(builderResultGetter(a()), expectedType, value.tpe)
    }
    
    def output(value: StreamValue, expectedType: Type)(implicit loop: Loop): Unit =
      outputBuilder(expectedType, value)
  }
  trait ArrayBuilderStreamSink extends BuilderStreamSink with WithArrayResultWrapper {
    def createBuilderGen(value: StreamValue)(implicit loop: Loop): BuilderGen =
      new ArrayBuilderGen(value.tpe, loop.localTyper)
  }
  trait CanCreateArraySink extends CanCreateStreamSink {
    def tree: Tree
    
    override def createStreamSink(expectedType: Type, componentType: Type, outputSize: Option[TreeGen]): StreamSink = 
      new StreamSink 
      with ArrayStreamSink 
      with ArrayBuilderStreamSink
      {
        override def tree = CanCreateArraySink.this.tree
        
        override def output(value: StreamValue, expectedType: Type)(implicit loop: Loop): Unit = {
          (value.valueIndex, value.valuesCount) match {
            case (Some(index), Some(size)) =>
              outputArray(expectedType, value, index, size)
            case _ =>
              outputBuilder(expectedType, value)
          }
        }
      }
  }
  trait CanCreateVectorSink extends CanCreateStreamSink {
    def tree: Tree
    
    override def createStreamSink(expectedType: Type, componentType: Type, outputSize: Option[TreeGen]): StreamSink = 
      new StreamSink 
      with BuilderStreamSink 
      {
        override def tree = CanCreateVectorSink.this.tree
      
        def createBuilderGen(value: StreamValue)(implicit loop: Loop): BuilderGen =
          new VectorBuilderGen(value.tpe)
      }
  }
  trait CanCreateListSink extends CanCreateStreamSink {
    def tree: Tree
    
    override def createStreamSink(expectedType: Type, componentType: Type, outputSize: Option[TreeGen]): StreamSink = 
      new StreamSink 
      with BuilderStreamSink 
      {
        override def tree = CanCreateListSink.this.tree
      
        def createBuilderGen(value: StreamValue)(implicit loop: Loop): BuilderGen =
          new ListBuilderGen(value.tpe)
      }
  }
  
  trait CanCreateSetSink extends CanCreateStreamSink {
    def tree: Tree
    
    override def createStreamSink(expectedType: Type, componentType: Type, outputSize: Option[TreeGen]): StreamSink = 
      new StreamSink 
      with BuilderStreamSink 
      {
        override def tree = CanCreateSetSink.this.tree
      
        def createBuilderGen(value: StreamValue)(implicit loop: Loop): BuilderGen =
          new SetBuilderGen(value.tpe)
      }
  }
  
  trait CanCreateOptionSink extends CanCreateStreamSink {
    def tree: Tree
    override def consumesExtraFirstValue = false // TODO
    
    override def createStreamSink(expectedType: Type, componentType: Type, outputSize: Option[TreeGen]): StreamSink = 
      new StreamSink 
      {
        override def tree = CanCreateOptionSink.this.tree
      
        //def createBuilderGen(value: StreamValue)(implicit loop: Loop): BuilderGen =
        //  new VectorBuilderGen(value.tpe)
          
        def output(value: StreamValue, expectedType: Type)(implicit loop: Loop): Unit = {
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
            )
          }
        }
      }
  }
}
