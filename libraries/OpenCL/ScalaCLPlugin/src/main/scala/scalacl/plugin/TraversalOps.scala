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
  this: PluginComponent with WithOptions with WorkaroundsForOtherPhases =>
  
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
  
  def refineComponentType(componentType: Type, collectionTree: Tree): Type = {
    collectionTree.tpe match {
      case TypeRef(_, _, List(t)) =>
        t
      case _ =>
        componentType
    }
  }
  
  import TraversalOps._
  
  def traversalOpWithoutArg(n: Name, tree: Tree) = Option(n) collect {
    case toListName() =>
      ToListOp(tree)
    case toArrayName() =>
      ToArrayOp(tree)
    //case toSeqName() =>
    //  ToSeqOp(tree) // TODO !!!
    case toIndexedSeqName() =>
      ToIndexedSeqOp(tree)
    case toVectorName() =>
      ToVectorOp(tree)
    //case reverseName() =>
    //  ReverseOp(tree) // TODO !!!
    case sumName() =>
      SumOp(tree)
    case productName() =>
      ProductOp(tree)
    case minName() =>
      MinOp(tree)
    case maxName() =>
      MaxOp(tree)
  }
  
  def basicTypeApplyTraversalOp(tree: Tree, collection: Tree, name: Name, typeArgs: List[Tree], args: Seq[List[Tree]]): Option[TraversalOp] = {
    (name, typeArgs, args) match {
      case // Option.map[B](f)
        (
          mapName(), 
          List(mappedComponentType), 
          Seq(
            List(function)
          )
        ) =>
        Some(new TraversalOp(MapOp(tree, function, null), collection, refineComponentType(mappedComponentType.tpe, tree), null, true, null))
      case // map[B, That](f)(canBuildFrom)
        (
          mapName(), 
          List(mappedComponentType, mappedCollectionType), 
          Seq(
            List(function),
            List(canBuildFrom @ CanBuildFromArg())
          )
        ) =>
        Some(new TraversalOp(MapOp(tree, function, canBuildFrom), collection, refineComponentType(mappedComponentType.tpe, tree), mappedCollectionType.tpe, true, null))
      case 
        (
          foreachName(), 
          List(fRetType), 
          Seq(
            List(function)
          )
        ) =>
        Some(new TraversalOp(ForeachOp(tree, function), collection, null, null, true, null))
        
      case // scanLeft, scanRight
        (
          ScanName(isLeft),
          List(functionResultType, mappedArrayType),
          Seq(
            List(initialValue),
            List(function),
            List(CanBuildFromArg())
          )
        ) =>
        Some(new TraversalOp(ScanOp(tree, function, initialValue, isLeft), collection, functionResultType.tpe, null, isLeft, initialValue))
      case // foldLeft, foldRight
        (
          FoldName(isLeft), 
          List(functionResultType), 
          Seq(
            List(initialValue),
            List(function)
          )
        ) =>
        Some(new TraversalOp(FoldOp(tree, function, initialValue, isLeft), collection, functionResultType.tpe, null, isLeft, initialValue))
      case // toArray
        (
          toArrayName(), 
          List(functionResultType @ TypeTree()), 
          Seq(
            List(manifest)
          )
        ) =>
        Some(new TraversalOp(new ToArrayOp(tree), collection, functionResultType.tpe, null, true, null))
      case // sum, min, max
        (
          n @ (sumName() | minName() | maxName()),
          List(functionResultType @ TypeTree()),
          Seq(
            List(isNumeric)
          )
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
      case // reduceLeft, reduceRight
        (
          ReduceName(isLeft),
          List(functionResultType),
          Seq(
            List(function)
          )
        ) =>
        Some(new TraversalOp(ReduceOp(tree, function, isLeft), collection, functionResultType.tpe, null, isLeft, null))
      case // zip(col)(canBuildFrom)
        (
          mapName(),
          List(mappedComponentType, otherComponentType, mappedCollectionType),
          Seq(
            List(zippedCollection),
            List(canBuildFrom @ CanBuildFromArg())
          )
        ) =>
        Some(new TraversalOp(ZipOp(tree, zippedCollection), collection, refineComponentType(mappedComponentType.tpe, tree), mappedCollectionType.tpe, true, null))
      case // zipWithIndex(canBuildFrom)
        (
          zipWithIndexName(),
          List(mappedComponentType, mappedCollectionType),
          Seq(
            List(canBuildFrom @ CanBuildFromArg())
          )
        ) =>
        Some(new TraversalOp(ZipWithIndexOp(tree), collection, refineComponentType(mappedComponentType.tpe, tree), mappedCollectionType.tpe, true, null))
      case _ => 
        None 
    }
  }
  object TraversalOp {
    
    def unapply(tree: Tree): Option[TraversalOp] = tree match {
      case // Option.map[B](f)
        BasicTypeApply(
          collection, 
          name, 
          typeArgs, 
          args
        ) =>
        // Having a separate matcher helps avoid "jump offset too large for 16 bits integers" errors when generating bytecode
        basicTypeApplyTraversalOp(tree, collection, name, typeArgs, args)
      case TypeApply(Select(collection, toSetName()), List(resultType)) =>
        Some(new TraversalOp(ToSetOp(tree), collection, resultType.tpe, tree.tpe, true, null))        
      case // reverse, toList, toSeq, toIndexedSeq
        Select(collection, n @ (reverseName() | toListName() | toSeqName() | toIndexedSeqName() | toVectorName())) =>
        traversalOpWithoutArg(n, tree).collect { case op => new TraversalOp(op, collection, null, null, true, null) }
        //Some(new TraversalOp(Reverse, collection, null, null, true, null))
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
    
  }
}