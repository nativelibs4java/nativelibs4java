/*
 * ScalaCL - putting Scala on the GPU with JavaCL / OpenCL
 * http://scalacl.googlecode.com/
 *
 * Copyright (c) 2009-2010, Olivier Chafik (http://ochafik.free.fr/)
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

import scala.tools.nsc.Global

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}

object OpsFuserTransformComponent {
  val runsAfter = List[String](
    "namer"
  )
  val phaseName = "scalacl-opsfuser"
}
class OpsFuserTransformComponent(val global: Global, val fileAndLineOptimizationFilter: ScalaCLPlugin.FileAndLineOptimizationFilter)
extends PluginComponent
   with Transform
   with TypingTransformers
   with MiscMatchers
   with TreeBuilders
   with WithOptimizationFilter
{
  import global._
  import global.definitions._
  import scala.tools.nsc.symtab.Flags._
  import typer.{typed, atOwner}    // methods to type trees

  override val runsAfter = OpsFuserTransformComponent.runsAfter
  override val phaseName = OpsFuserTransformComponent.phaseName

  def newTransformer(unit: CompilationUnit) = new TypingTransformer(unit) {
    var currentClassName: Name = null

    def freshName(prefix: String) = unit.fresh.newName(NoPosition, prefix)

    override def transform(tree: Tree): Tree = tree match {
      // collection.map(f).map(g) -> collection.map(x => { val fx = f(x); g(fx) })
      case
        Apply(
          Apply(
            TypeApply(
              Select(
                fCollection,
                mapName()
              ),
              List(_, mappedCollectionType)
            ),
            List(g @ Func(List(gArg), gBody))
          ),
          List(CanBuildFromArg())
        )
        /*MapTree(
          fCollection,
          g @ Function(List(gArg), gBody),
          _,
          mappedCollectionType,
          _
        )*/ =>
        val cc = fCollection
        println("fCollection = " + fCollection)
        fCollection match {
          case
            Apply(
              Apply(
                TypeApply(
                  Select(
                    collection,
                    mapName()
                  ),
                  aa//List(fArgType, _)
                ),
                ff //List(f)
              ),
              ll//List(canBuildFrom @ CanBuildFromArg())
            )
            /*MapTree(
              collection,
              f @ Function(List(fArg), fBody),
              fArgType,
              _,
              canBuildFrom
            )*/ =>
            val List(fArgType, _) = aa
            val List(f) = ff
            val List(canBuildFrom @ CanBuildFromArg()) = ll
            f match {
              case Func(List(fArg), fBody) =>
                val compFuncSym = currentOwner.newMethod(f.pos, freshName("comp$"))
                val compArgName = freshName("compArg$")
                val compArgSym = compFuncSym.newValueParameter(f.pos, compArgName)
                val compArgIdentGen = () => ident(compArgSym, compArgName)
                val (fRetIdentGen, fRetSym, fRetDef) = newVariable(
                  unit,
                  "fRet$",
                  compFuncSym,
                  f.pos,
                  true,
                  replaceOccurrences(
                    fBody,
                    Map(fArg.symbol -> compArgIdentGen),
                    Map(f.symbol -> compFuncSym),
                    unit
                  )
                )
                val comp = treeCopy.Function(
                  tree,
                  List(
                    ValDef(Modifiers(0), compArgName, fArg.tpt, fArg.rhs).setSymbol(compArgSym).setType(fArg.tpe)
                  ),
                  Block(
                    fRetDef,
                    replaceOccurrences(
                      gBody,
                      Map(gArg.symbol -> fRetIdentGen),
                      Map(g.symbol -> compFuncSym),
                      unit
                    )
                  ).setType(gBody.tpe)
                )
                println("composed function\nf = " + f + "\ng = " + g + "\ncomp = " + comp)
                val m = MapTree(collection, comp, fArgType, mappedCollectionType, canBuildFrom)
                val tm = typed {
                  m
                }
                super.transform(tm)
              case _ =>
                super.transform(tree)
            }
          case Apply(a, b) =>
            val aa = a
            val bb = b
            println(a)
            println(b)
            super.transform(tree)
          case _ =>
            super.transform(tree)
        }
      case _ =>
        super.transform(tree)
    }
  }
}
