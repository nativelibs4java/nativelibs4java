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
package scalaxy.common

trait Tuploids extends CommonScalaNames {
  val global: reflect.api.Universe
  import global._
  import definitions._
  
  private lazy val primTypes = Set(IntTpe, LongTpe, ShortTpe, CharTpe, BooleanTpe, DoubleTpe, FloatTpe, ByteTpe)
  
  def isPrimitiveType(tpe: Type) = primTypes.contains(tpe.normalize)
  
  def getTupleComponentTypes(tpe: Type): List[Type] = { 
    tpe match {
      case ref @ TypeRef(pre, sym, args) 
      if isTupleTypeRef(ref) => args
    }
  }
  
  def isTupleTypeRef(ref: TypeRef): Boolean = {
    !ref.args.isEmpty &&
    ref.pre.typeSymbol == ScalaPackageClass && 
    ref.sym.name.toString.matches("Tuple\\d+")
  }
  
  def isTupleType(tpe: Type): Boolean = {
    tpe match {
      case ref @ TypeRef(pre, sym, args)
      if isTupleTypeRef(ref) =>
        true
      case _ =>
        //if (tpe.toString.contains("Tuple"))
        println(s"tpe($tpe: ${tpe.getClass.getName}).typeConstructor = ${tpe.typeConstructor} (${tpe.typeSymbol})")
        false
    }
  }
  
  private def isValOrVar(s: Symbol): Boolean =
    s.isTerm && !s.isMethod && !s.isJava
  
  private def isStableNonLazyVal(ts: TermSymbol): Boolean = {
    //println(s"""
    //  isVal = ${ts.isVal}
    //  isStable = ${ts.isStable}
    //  isVar = ${ts.isVar}
    //  isSetter = ${ts.isSetter}
    //  isGetter = ${ts.isGetter}
    //  isLazy = ${ts.isLazy}
    //""")
    val res = ts.isStable && ts.isVal && !ts.isLazy
    //println("res = " + res)
    res
  }
  private def isImmutableClassMember(s: Symbol): Boolean = {
    //println(s + " <- " + s.owner + " overrides " + s.allOverriddenSymbols)
    //println(s"\tisFinal = ${s.isFinal}, isMethod = ${s.isMethod}, isTerm = ${s.isTerm}")
    if (isValOrVar(s)) {
      isStableNonLazyVal(s.asTerm)
    } else {
      // Either a method or a sub-type
      true
    }
  }
  
  // A tuploid is a scalar, a tuple of tuploids or an immutable case class with tuploid fields.
  def isTuploidType(tpe: Type): Boolean = { 
    isPrimitiveType(tpe) ||
    isTupleType(tpe) && getTupleComponentTypes(tpe).forall(isTuploidType _) ||
    {
      tpe.declarations.exists(isValOrVar _) &&
      tpe.declarations.forall(isImmutableClassMember _)
    }
  }
}
