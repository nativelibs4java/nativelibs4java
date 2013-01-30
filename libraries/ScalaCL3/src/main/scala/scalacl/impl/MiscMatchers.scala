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

trait MiscMatchers extends ConversionNames {
  val global: reflect.api.Universe
  import global._
  import definitions._
  
  object WhileLoop {
    def unapply(tree: Tree) = tree match {
      case
        LabelDef(
          lab,
          List(),
          If(
            condition,
            Block(
              content,
              Apply(
                Ident(lab2),
                List()
              )
            ),
            Literal(Constant(()))
          )
        ) if (lab == lab2) =>
        Some(condition, content)
      case _ =>
        None
    }
  }
  
  def predefModule = PredefModule
  
  object Foreach {
    def unapply(tree: Tree): Option[(Tree, Function)] = Option(tree) collect {
      case Apply(TypeApply(Select(collection, foreachName()), typeArgs), function @ Function(_, _)) =>
        (collection, function)
    }
  }
  
  object IntRange {
    def apply(from: Tree, to: Tree, by: Option[Tree], isUntil: Boolean, filters: List[Tree]) = sys.error("not implemented")
    def unapply(tree: Tree): Option[(Tree, Tree, Option[Tree], Boolean, List[Tree])] = tree match {
      case Apply(Select(Apply(Select(Select(predefModule), intWrapperName()), List(from)), funToName @ (toName() | untilName())), List(to)) =>
        Option(funToName) collect {
          case toName() =>
            (from, to, None, false, Nil)
          case untilName() =>
            (from, to, None, true, Nil)
        }
      case Apply(Select(tg, n @ (byName() | withFilterName() | filterName())), List(arg)) =>
       tg match {
          case IntRange(from, to, by, isUntil, filters) =>
            Option(n) collect {
                case byName() if by == None =>
                    (from, to, Some(arg), isUntil, filters)
                case withFilterName() | filterName() =>
                    (from, to, by, isUntil, filters :+ arg)
            }
          case _ =>
            None
        }
      case _ => 
        None
    }
  }
}
