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
package scalacl.instrumentation

import scala.tools.nsc.Global

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}

import scalacl._

/*
set SCALACL_INSTRUMENT=1
mvn scala:run -DmainClass=scalacl.Compile "-DaddArgs=t.scala|-Xprint:scalacl-instrument"
*/
object InstrumentationTransformComponent {
  val runsAfter = List[String](
    "namer"
  )
  val phaseName = "scalacl-instrument"
}
class InstrumentationTransformComponent(val global: Global, val fileAndLineOptimizationFilter: ScalaCLPlugin.FileAndLineOptimizationFilter)
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

  override val runsAfter = InstrumentationTransformComponent.runsAfter
  override val phaseName = InstrumentationTransformComponent.phaseName

  val ids = new LogIds
  val logClassName = "scalacl.instrumentation.Log"
  val LogClass = definitions.getClass(logClassName)
  val LogModule = definitions.getModule(logClassName)
  val logApplyMethod = LogModule.tpe member "apply"
  val logEnterMethod = LogClass.tpe member "enter"
  val logExitMethod = LogClass.tpe member "exit"
  
  def newTransformer(unit: CompilationUnit) = new TypingTransformer(unit) {
    var currentClassName: Name = null

    override def transform(tree: Tree): Tree = tree match {
      case block: Block =>
        val pos = tree.pos
        val logId = LogId(currentOwner.ownerChain.mkString(","), pos.source.file.path, pos.line, pos.offset.getOrElse(-1))
        msg(unit, pos, "instrumented " + logId) {
            val id = ids(logId)
            val (logIdentGen, logSym, logDef) = newVariable(unit, "log", currentOwner, tree.pos, false, 
                Apply(
                  Select(
                     TypeTree(LogModule.tpe),
                     N("apply")
                  ).setSymbol(logApplyMethod).setType(LogClass.tpe),
                  Nil
                ).setType(LogClass.tpe)
            )
            typed {
              treeCopy.Block(
                tree,
                List(
                    logDef,
                    Apply(
                        typed { Select( // sym=method enter, sym.owner=class Log, sym.tpe=(id: Long)Unit, tpe=(id: Long)Unit, tpe.sym=<none>
                          logIdentGen(),
                          N("enter")
                        ).setSymbol(logEnterMethod) },
                        List(newLong(id))
                    ).setSymbol(logEnterMethod).setType(UnitClass.tpe)
                ),
                Try(
                    super.transform(block),
                    Nil,
                    Apply(
                        typed { Select( // sym=method enter, sym.owner=class Log, sym.tpe=(id: Long)Unit, tpe=(id: Long)Unit, tpe.sym=<none>
                          logIdentGen(),
                          N("exit")
                        ).setSymbol(logExitMethod) },
                        List(newLong(id))
                    ).setSymbol(logExitMethod).setType(UnitClass.tpe)
                )
              )
            }
        }
      case _ =>
        super.transform(tree)
    }
  }
}
