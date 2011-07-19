/*
 * Created by IntelliJ IDEA.
 * User: ochafik
 * Date: 10/05/11
 * Time: 15:10
 */
package scalacl ; package plugin

import tools.nsc.Global
import tools.nsc.plugins.PluginComponent

trait Streams extends TreeBuilders with TupleAnalysis {
  this: PluginComponent with WithOptions =>
  
  val global: Global
  import global._
  import definitions._
  import treeInfo.{ methPart }
  import typer.typed

  trait LocalContext {
    def unit: CompilationUnit
    def localTyper: analyzer.Typer
    def currentOwner: Symbol
    def addDefinition(tree: Tree): Unit
  }
  case class Loop(unit: CompilationUnit, pos: Position, currentOwner: Symbol, localTyper: analyzer.Typer) {
    type OptTreeGen = () => Option[Tree]
    class TreeGenList {
      var data = Seq[Either[Tree, OptTreeGen]]()
      def +=(tree: Tree) = 
        data ++= Seq(Left(tree))
      
      def ++=(trees: Seq[Tree]) = 
        data ++= trees.map(Left(_))
      
      def +=(treeGen: OptTreeGen) = 
        data ++= Seq(Right(treeGen))
      
      //def +=(treeGen: TreeGen) =
      //  data ++= Seq(Right(() => Some(treeGen())))
      
      def toSeq: Seq[Tree] = data flatMap(_ match {
        case Left(tree) => Some(tree)
        case Right(treeGen) => treeGen()
      })
      def toList = toSeq.toList
    }
    val preOuter = new TreeGenList
    val loopTests = new TreeGenList
    val preInner = new TreeGenList
    val inner = new TreeGenList
    val postInner = new TreeGenList
    val postOuter = new TreeGenList

    class SubContext(list: TreeGenList) extends LocalContext {
      override def unit = Loop.this.unit
      override def localTyper = Loop.this.localTyper
      override def currentOwner = Loop.this.currentOwner
      override def addDefinition(tree: Tree) =
        list += tree
    }
    val innerContext = new SubContext(preInner)
    val outerContext = new SubContext(preOuter)
    
    def tree: Tree = {
      val postOuterSeq = postOuter.toSeq
      val (postStats, postVal) =
        if (postOuterSeq.isEmpty)
          (Seq(), EmptyTree)
        else
          (postOuterSeq.dropRight(1), postOuterSeq.last)

      val ret = Block(
        preOuter.toList ++
        Seq(
          whileLoop(
            owner = currentOwner,
            unit = unit,
            pos = pos,
            cond = loopTests.toSeq.reduceLeft(boolAnd),
            body = Block(preInner.toList ++ inner.toList ++ postInner.toList, EmptyTree)
          )
        ) ++
        postStats,
        postVal
      )
      typed { ret }
    }
  }
  trait TupleValue {
    def apply(): Tree
    def tpe: Type
    def elements: Seq[IdentGen]
    def fibersCount: Int
    def componentsCount: Int

    def tuple(implicit context: LocalContext): IdentGen =
      subValue(0, fibersCount)

    def fiber(index: Int)(implicit context: LocalContext): IdentGen =
      subValue(index, 1)

    def subValue(fiberOffset: Int, fiberLength: Int)(implicit context: LocalContext): IdentGen
    def subTuple(fiberOffset: Int, fiberLength: Int)(implicit context: LocalContext): TupleValue
  }
  class DefaultTupleValue(val tpe: Type, val elements: IdentGen*) extends TupleValue {
    override def apply() = {
      if (elements.size != 1)
        throw new UnsupportedOperationException("TODO")
      else
        elements.first.apply()
    }
    val tupleInfo = getTupleInfo(tpe)
    def fibersCount = tupleInfo.componentSize
    def componentsCount = elements.size
    
    protected def hasOneFiber =
      fibersCount == 1 && elements.size == 1
        
    def subValue(fiberOffset: Int, fiberLength: Int)(implicit context: LocalContext): IdentGen =
      if (fiberLength == 1 && fiberOffset == 0 && hasOneFiber)
        elements(0)
      else
        throw new RuntimeException("not implemented")
    def subTuple(fiberOffset: Int, fiberLength: Int)(implicit context: LocalContext): TupleValue =
      if (fiberLength == fibersCount && fiberOffset == 0)
        this
      else
        throw new RuntimeException("not implemented")
  }
  
  trait StreamValue {
    def value: TupleValue
    def valueIndex: Option[IdentGen]
    def valuesCount: Option[IdentGen]
    def copyWithValue(newValue: TupleValue) = {
      new SimpleStreamValue(newValue, valueIndex, valuesCount)
    }
  }
  class SimpleStreamValue(val value: TupleValue, val valueIndex: Option[IdentGen], val valuesCount: Option[IdentGen]) extends StreamValue {
    def this(value: VarDef, valueIndex: Option[VarDef], valuesCount: Option[VarDef]) =
      this(new DefaultTupleValue(value.definition.tpe, value), valueIndex.map(_.identGen), valuesCount.map(_.identGen))
  }
   
  sealed trait TraversalDirection
  object FromLeft extends TraversalDirection
  object FromRight extends TraversalDirection

  sealed trait Order
  object SameOrder extends Order
  object ReverseOrder extends Order
  object Unordered extends Order

  trait StreamComponent {
    def tree: Tree
    
    /// Used to chain stream detection : give the unwrapped content of the tree
    def unwrappedTree = tree
    def privilegedDirection: Option[TraversalDirection] = None
  }
  trait CanCreateStreamSink {
    def createStreamSink(componentTpe: Type, outputSize: Option[IdentGen]): StreamSink
  }
  trait StreamSource extends StreamComponent {
    def emit(direction: TraversalDirection, transform: Tree => Tree)(implicit loop: Loop): StreamValue
  }
  trait StreamTransformer extends StreamComponent {
    def order: Order
    def reverses = false
    
    def transform(value: StreamValue)(implicit loop: Loop): StreamValue
  }
  trait StreamSink extends StreamComponent {
    def output(value: StreamValue)(implicit loop: Loop): Unit
  }
  def assembleStream(source: StreamSource, transformers: Seq[StreamTransformer], sinkCreator: CanCreateStreamSink, transform: Tree => Tree, unit: CompilationUnit, pos: Position, currentOwner: Symbol, localTyper: analyzer.Typer): Tree = {
    implicit val loop = new Loop(unit, pos, currentOwner, localTyper)
    val direction = FromLeft // TODO choose depending on preferred directions...
    var value = source.emit(direction, transform)
    for (transformer <- transformers)
      value = transformer.transform(value)
      
    val sink = sinkCreator.createStreamSink(value.value.tpe, value.valuesCount)
    sink.output(value)
    loop.tree
  }
}