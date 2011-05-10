package scalacl

import tools.nsc.Global
import reflect.NameTransformer
;
trait PluginNames {
  val global: Global
  import global._
  import definitions._
  import treeInfo.{ methPart }
  import typer.typed

  class N(val s: String) {
    def unapply(n: Name): Boolean = n.toString == s
  }
  object N {
    def apply(s: String) = new N(s)
  }
  implicit def N2Name(n: N) = newTermName(n.s)

  val addAssignName = N(NameTransformer.encode("+="))
  val toArrayName = N("toArray")
  val toListName = N("toList")
  val toSeqName = N("toSeq")
  val toSetName = N("toSet")
  val toIndexedSeqName = N("toIndexedSeq")
  val toMapName = N("toMap")
  val resultName = N("result")
  val scalaName = N("scala")
  val ArrayName = N("Array")
  val intWrapperName = N("intWrapper")
  val tabulateName = N("tabulate")
  val toName = N("to")
  val byName = N("by")
  val withFilterName = N("withFilter")
  val untilName = N("until")
  val isEmptyName = N("isEmpty")
  val sumName = N("sum")
  val minName = N("min")
  val maxName = N("max")
  val headName = N("head")
  val tailName = N("tail")
  val foreachName = N("foreach")
  val foldLeftName = N("foldLeft")
  val foldRightName = N("foldRight")
  val zipWithIndexName = N("zipWithIndex")
  val zipName = N("zip")
  val reverseName = N("reverse")
  val reduceLeftName = N("reduceLeft")
  val reduceRightName = N("reduceRight")
  val scanLeftName = N("scanLeft")
  val scanRightName = N("scanRight")
  val mapName = N("map")
  val collectName = N("collect")
  val canBuildFromName = N("canBuildFrom")
  val filterName = N("filter")
  val filterNotName = N("filterNot")
  val takeWhileName = N("takeWhile")
  val dropWhileName = N("dropWhile")
  val countName = N("count")
  val forallName = N("forall")
  val existsName = N("exists")
  val findName = N("find")
  val updateName = N("update")
  val toSizeTName = N("toSizeT")
  val toLongName = N("toLong")
  val toIntName = N("toInt")
  val toShortName = N("toShort")
  val toByteName = N("toByte")
  val toCharName = N("toChar")
  val toDoubleName = N("toDouble")
  val toFloatName = N("toFloat")
  val mathName = N("math")
  val packageName = N("package")
  val applyName = N("apply")
  val Function2CLFunctionName = N("Function2CLFunction")

  lazy val ScalaCollectionPackage = definitions.getModule(N("scala.collection"))
  lazy val ScalaMathPackage = definitions.getModule(N("scala.math"))
  lazy val ScalaReflectPackage = definitions.getModule(N("scala.reflect"))
  lazy val ScalaMathCommonClass = definitions.getClass(N("scala.MathCommon"))
  lazy val SeqClass = definitions.getClass(N("scala.collection.Seq"))
  lazy val SeqModule = definitions.getModule(N("scala.collection.Seq"))
  lazy val OptionClass = definitions.getClass(N("scala.Option"))
  lazy val OptionModule = definitions.getModule(N("scala.Option"))
  lazy val CanBuildFromClass = definitions.getClass("scala.collection.generic.CanBuildFrom")
  lazy val ArrayBufferClass = definitions.getClass("scala.collection.mutable.ArrayBuffer")
  lazy val RefArrayBuilderClass = definitions.getClass("scala.collection.mutable.ArrayBuilder.ofRef")
  lazy val WrappedArrayBuilderClass = definitions.getClass("scala.collection.mutable.WrappedArrayBuilder")
  lazy val VectorBuilderClass = definitions.getClass("scala.collection.immutable.VectorBuilder")
  lazy val CollectionImmutableModule = definitions.getModule("scala.collection.immutable")
  //lazy val NonEmptyListClass = definitions.getClass2("scala.$colon$colon$", "scala.collection.immutable.$colon$colon$")
  //lazy val NonEmptyListClass = definitions.getClass2("scala.$colon$colon", "scala.collection.immutable.$colon$colon")
  lazy val NonEmptyListClass = definitions.getClass("scala.collection.immutable.$colon$colon")
  //lazy val NonEmptyListClass = definitions.getMember(CollectionImmutableModule, "::")
  //lazy val NonEmptyListClass = definitions.getClass2("scala.::", "scala.collection.immutable.::")
  //lazy val NonEmptyListClass = definitions.getMember(ScalaPackageClass, "::")
  lazy val ListBufferClass = definitions.getClass("scala.collection.mutable.ListBuffer")
  lazy val primArrayBuilderClasses = Array(
    (IntClass.tpe, "ofInt"),
    (LongClass.tpe, "ofLong"),
    (ShortClass.tpe, "ofShort"),
    (ByteClass.tpe, "ofByte"),
    (CharClass.tpe, "ofChar"),
    (BooleanClass.tpe, "ofBoolean"),
    (FloatClass.tpe, "ofFloat"),
    (DoubleClass.tpe, "ofDouble")
  ).map { case (sym, n) => (sym, definitions.getClass("scala.collection.mutable.ArrayBuilder." + n)) } toMap

}