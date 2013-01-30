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
package scalacl
package impl

import reflect.NameTransformer

trait ConversionNames {
  val global: reflect.api.Universe
  import global._
  import definitions._

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
  val toVectorName = N("toVector")
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
  val productName = N("product")
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
  val thisName = N("this")
  val superName = N("super")

  /*
  def C(name: String) = definitions.getClass(name)
  def M(name: String) = definitions.getModule(name)
  
  lazy val ScalaReflectPackage = M("scala.reflect")
  lazy val ScalaCollectionPackage = M("scala.collection")
  lazy val ScalaMathPackage = M("scala.math")
  lazy val ScalaMathPackageClass  = ScalaMathPackage.tpe.typeSymbol
  lazy val ScalaMathCommonClass = C("scala.MathCommon")
  
  lazy val SeqModule          = M("scala.collection.Seq")
  lazy val SeqClass           = C("scala.collection.Seq")
  lazy val SetModule          = M("scala.collection.Set")
  lazy val SetClass           = C("scala.collection.Set")
  lazy val VectorClass        = C("scala.collection.Set")
  lazy val ListClass          = C("scala.List")
  lazy val ImmutableListClass = C("scala.collection.immutable.List")
  lazy val NonEmptyListClass  = C("scala.collection.immutable.$colon$colon")
  lazy val IndexedSeqModule   = M("scala.collection.IndexedSeq")
  lazy val IndexedSeqClass    = C("scala.collection.IndexedSeq")
  lazy val OptionModule       = M("scala.Option")
  lazy val OptionClass        = C("scala.Option")
  lazy val SomeModule         = M("scala.Some")
  lazy val NoneModule         = M("scala.None")
  lazy val StringOpsClass     = C("scala.collection.immutable.StringOps")
  
  lazy val VectorBuilderClass         = C("scala.collection.immutable.VectorBuilder")
  lazy val ListBufferClass            = C("scala.collection.mutable.ListBuffer")
  lazy val ArrayBufferClass           = C("scala.collection.mutable.ArrayBuffer")
  lazy val WrappedArrayBuilderClass   = C("scala.collection.mutable.WrappedArrayBuilder")
  lazy val RefArrayBuilderClass       = C("scala.collection.mutable.ArrayBuilder.ofRef")
  lazy val RefArrayOpsClass           = C("scala.collection.mutable.ArrayOps.ofRef")
  lazy val SetBuilderClass            = C("scala.collection.mutable.SetBuilder")
  
  lazy val RichWrappers: Set[Symbol] = 
    Array("Byte", "Short", "Int", "Char", "Long", "Float", "Double", "Boolean").
    map(n => C("scala.runtime.Rich" + n)).toSet
  
  lazy val CanBuildFromClass = C("scala.collection.generic.CanBuildFrom")

  lazy val ArrayIndexOutOfBoundsExceptionClass = C("java.lang.ArrayIndexOutOfBoundsException")
  
  lazy val primArrayNames = Array(
    (IntClass.tpe, "ofInt"),
    (LongClass.tpe, "ofLong"),
    (ShortClass.tpe, "ofShort"),
    (ByteClass.tpe, "ofByte"),
    (CharClass.tpe, "ofChar"),
    (BooleanClass.tpe, "ofBoolean"),
    (FloatClass.tpe, "ofFloat"),
    (DoubleClass.tpe, "ofDouble"),
    (UnitClass.tpe, "ofUnit")
  )
  
  lazy val primArrayBuilderClasses = primArrayNames.map { 
    case (sym, n) => 
      (sym, C("scala.collection.mutable.ArrayBuilder." + n)) 
  } toMap

  lazy val primArrayOpsClasses = primArrayNames.map { 
    case (sym, n) => 
      (sym, C("scala.collection.mutable.ArrayOps." + n)) 
  } toMap
  */
}
