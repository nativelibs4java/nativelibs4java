/*
 * Created by IntelliJ IDEA.
 * User: ochafik
 * Date: 10/05/11
 * Time: 21:40
 */
package scalacl ; package plugin

import tools.nsc.plugins.PluginComponent
import tools.nsc.Global

trait TraversalOps 
extends PluginNames 
   with StreamOps
   with MiscMatchers
{
  this: PluginComponent with WithOptions =>
  
  val global: Global
  import global._
  import definitions._
  import treeInfo.{ methPart }
  import typer.typed

  object ReduceName {
    def apply(isLeft: Boolean) = error("not implemented")
    def unapply(name: Name) = Option(name) collect {
      case reduceLeftName() => true
      case reduceRightName() => false
    }
  }
  object ScanName {
    def apply(isLeft: Boolean) = error("not implemented")
    def unapply(name: Name) = Option(name) collect {
      case scanLeftName() => true
      case scanRightName() => false
    }
  }
  object FoldName {
    def apply(isLeft: Boolean) = error("not implemented")
    def unapply(name: Name) = Option(name) collect {
      case foldLeftName() => true
      case foldRightName() => false
    }
  }
  
  object TraversalOp {
    import TraversalOps._
    
    def refineComponentType(componentType: Type, collectionTree: Tree): Type = {
      collectionTree.tpe match {
        case TypeRef(_, _, List(t)) =>
          t
        case _ =>
          componentType
      }
    }
    //def apply(op: TraversalOpType, array: Tree, resultType: Type, mappedCollectionType: Type, function: Tree, isLeft: Boolean, initialValue: Tree) = error("not implemented")
    def unapply(tree: Tree): Option[TraversalOp] = tree match {
      case // map[B, That](f)(canBuildFrom)
        Apply(
          Apply(
            TypeApply(
              Select(collection, mapName()),
              List(mappedComponentType, mappedCollectionType)
            ),
            List(function)
          ),
          List(canBuildFrom @ CanBuildFromArg())
        ) =>
        //println("collection.tpe = " + collection.tpe)
        //println("collection.tpe.typeSymbol = " + collection.tpe.typeSymbol)
        //println("trivialCollectionSymbol = " + trivialCollectionSymbol)
        //println("mappedCollectionType = " + mappedCollectionType)
        //println("trivialResultType = " + trivialResultType)
        //println("mappedComponentType = " + mappedComponentType)
        //println("\t-> " + (collection.tpe.typeSymbol == trivialCollectionSymbol))
        Some(new TraversalOp(MapOp(tree, function, canBuildFrom), collection, refineComponentType(mappedComponentType.tpe, tree), mappedCollectionType.tpe, true, null))
      case // map[B](f)
        Apply(
          TypeApply(
            Select(collection, mapName()),
            List(mappedComponentType)
          ),
          List(function)
        ) =>
        Some(new TraversalOp(MapOp(tree, function, null), collection, refineComponentType(mappedComponentType.tpe, tree), null, true, null))
      case Apply(TypeApply(Select(collection, foreachName()), List(fRetType)), List(function)) =>
        Some(new TraversalOp(ForeachOp(tree, function), collection, null, null, true, null))
      case // scanLeft, scanRight
        Apply(
          Apply(
            Apply(
              TypeApply(
                Select(collection, ScanName(isLeft)),
                List(functionResultType, mappedArrayType)
              ),
              List(initialValue)
            ),
            List(function)
          ),
          List(CanBuildFromArg())
        ) =>
        Some(new TraversalOp(ScanOp(tree, function, isLeft), collection, functionResultType.tpe, null, isLeft, initialValue))
      case // foldLeft, foldRight
        Apply(
          Apply(
            TypeApply(
              Select(collection, FoldName(isLeft)),
              List(functionResultType)
            ),
            List(initialValue)
          ),
          List(function)
        ) =>
        Some(new TraversalOp(FoldOp(tree, function, isLeft), collection, functionResultType.tpe, null, isLeft, initialValue))
      case // toArray
        Apply(
          TypeApply(
            Select(collection, toArrayName()),
            List(functionResultType @ TypeTree())
          ),
          List(manifest)
        ) =>
        Some(new TraversalOp(new ToCollectionOp(tree, ArrayType, tree.tpe), collection, functionResultType.tpe, null, true, null))
      case // sum, min, max
        Apply(
          TypeApply(
            Select(collection, n @ (sumName() | minName() | maxName())),
            List(functionResultType @ TypeTree())
          ),
          List(isNumeric)
        ) =>
        isNumeric.toString match {
          case
            "math.this.Numeric.IntIsIntegral" |
            "math.this.Numeric.ShortIsIntegral" |
            "math.this.Numeric.LongIsIntegral" |
            "math.this.Numeric.ByteIsIntegral" |
            "math.this.Numeric.CharIsIntegral" |
            "math.this.Numeric.FloatIsFractional" |
            "math.this.Numeric.DoubleIsFractional" |
            "math.this.Numeric.DoubleAsIfIntegral" |
            "math.this.Ordering.Int" |
            "math.this.Ordering.Short" |
            "math.this.Ordering.Long" |
            "math.this.Ordering.Byte" |
            "math.this.Ordering.Char" |
            "math.this.Ordering.Double" |
            "math.this.Ordering.Float"
            =>
            traversalOpWithoutArg(n, tree).collect { case op => new TraversalOp(op, collection, functionResultType.tpe, null, true, null) }
          case _ =>
            None
        }
      //case // sum, min, max, reverse
      //  Select(collection, n @ (sumName() | minName() | maxName() | reverseName())) =>
      //  traversalOpWithoutArg(n).collect { case op => new TraversalOp(op, collection, null, null, true, null) }
      case // reverse, toList, toSeq, toIndexedSeq
        Select(collection, n @ (reverseName() | toListName() | toSeqName() | toSetName() | toIndexedSeqName())) =>
        traversalOpWithoutArg(n, tree).collect { case op => new TraversalOp(op, collection, null, null, true, null) }
        //Some(new TraversalOp(Reverse, collection, null, null, true, null))
      case // reduceLeft, reduceRight
        Apply(
          TypeApply(
            Select(collection, ReduceName(isLeft)),
            List(functionResultType)
          ),
          List(function)
        ) =>
        Some(new TraversalOp(ReduceOp(tree, function, isLeft), collection, functionResultType.tpe, null, isLeft, null))
      case // zip(col)(canBuildFrom)
        Apply(
          Apply(
            TypeApply(
              Select(collection, mapName()),
              List(mappedComponentType, otherComponentType, mappedCollectionType)
            ),
            List(zippedCollection)
          ),
          List(canBuildFrom @ CanBuildFromArg())
        ) =>
        Some(new TraversalOp(ZipOp(tree, zippedCollection), collection, refineComponentType(mappedComponentType.tpe, tree), mappedCollectionType.tpe, true, null))
      case // zipWithIndex(canBuildFrom)
        Apply(
          TypeApply(
            Select(collection, zipWithIndexName()),
            List(mappedComponentType, mappedCollectionType)
          ),
          List(canBuildFrom @ CanBuildFromArg())
        ) =>
        Some(new TraversalOp(ZipWithIndexOp(tree), collection, refineComponentType(mappedComponentType.tpe, tree), mappedCollectionType.tpe, true, null))
      case // filter, filterNot, takeWhile, dropWhile, forall, exists
        Apply(Select(collection, n), List(function @ Func(List(param), body))) =>
        (
          n match {
            case withFilterName() =>
              //println("FOUND WITHFILTER")
              collection match {
                case IntRange(_, _, _, _, _) =>
                  //println("FOUND IntRange")
                  Some(FilterOp(tree, function, false), collection.tpe)
                case _ =>
                  //println("FOUND None")
                  None
              }
            case filterName() =>
              Some(FilterOp(tree, function, false), collection.tpe)
            case filterNotName() =>
              Some(FilterOp(tree, function, true), collection.tpe)

            case takeWhileName() =>
              Some(FilterWhileOp(tree, function, true), collection.tpe)
            case dropWhileName() =>
              Some(FilterWhileOp(tree, function, false), collection.tpe)

            case forallName() =>
              Some(AllOrSomeOp(tree, function, true), BooleanClass.tpe)
            case existsName() =>
              Some(AllOrSomeOp(tree, function, false), BooleanClass.tpe)

            case countName() =>
              Some(CountOp(tree, function), IntClass.tpe)
            case updateName() =>
              Some(UpdateAllOp(tree, function), collection.tpe)
            case _ =>
              None
          }
        ) match {
          case Some((op, resType)) =>
            Some(new TraversalOp(op, collection, resType, null, true, null))
          case None =>
            None
        }
      case _ =>
        None
    }
    def traversalOpWithoutArg(n: Name, tree: Tree) = Option(n) collect {
      case toListName() =>
        ToCollectionOp(tree, ListType, tree.tpe)
      case toArrayName() =>
        ToCollectionOp(tree, ArrayType, tree.tpe)
      case toSeqName() =>
        ToCollectionOp(tree, SeqType, tree.tpe)
      case toSetName() =>
        ToCollectionOp(tree, SetType, tree.tpe)
      case toIndexedSeqName() =>
        ToCollectionOp(tree, IndexedSeqType, tree.tpe)
      case toMapName() =>
        ToCollectionOp(tree, MapType, tree.tpe)
      case reverseName() =>
        ReverseOp(tree)
      case sumName() =>
        SumOp(tree)
      case minName() =>
        MinOp(tree)
      case maxName() =>
        MaxOp(tree)
    }
  }
}