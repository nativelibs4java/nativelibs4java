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
package scalacl ; package plugin

import scala.tools.nsc.Global

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}

object StreamTransformComponent {
  val runsAfter = List[String](
    "namer"
  )
  val runsBefore = List[String]("refchecks", LoopsTransformComponent.phaseName)
  val phaseName = "scalacl-stream"
}

class StreamTransformComponent(val global: Global, val options: ScalaCLPlugin.PluginOptions)
extends PluginComponent
   with Transform
   with TypingTransformers
   with MiscMatchers
   with TreeBuilders
   with TraversalOps
   with Streams with StreamImpls with StreamOps
   //with RewritingPluginComponent
   with WorkaroundsForOtherPhases
   with WithOptions
{
  import global._
  import global.definitions._
  import scala.tools.nsc.symtab.Flags._
  import typer.{typed, atOwner}    // methods to type trees

  override val runsAfter = StreamTransformComponent.runsAfter
  override val runsBefore = StreamTransformComponent.runsBefore
  override val phaseName = StreamTransformComponent.phaseName

  def newTransformer(compilationUnit: CompilationUnit) = new TypingTransformer(compilationUnit) {

    class OpsStream(val source: StreamSource, val colTree: Tree, val ops: List[StreamTransformer])
    object OpsStream {
      def unapply(tree: Tree) = {
        var ops = List[StreamTransformer]()
        var colTree = tree
        var source: StreamSource = null
        var finished = false
        while (!finished) {
          println("Trying to match " + colTree)
          colTree match {
            case StreamSource(cr) =>
              println("found streamSource " + cr + " (ops = " + ops + ")")
              source = cr
              if (colTree != cr.unwrappedTree)
                colTree = cr.unwrappedTree
              else
                finished = true
            case TraversalOp(traversalOp) if traversalOp.op.isInstanceOf[StreamTransformer] =>
              println("found op " + traversalOp + "\n\twith collection = " + traversalOp.collection)
              ops = traversalOp.op.asInstanceOf[StreamTransformer] :: ops
              colTree = traversalOp.collection
            case _ =>
              if (!ops.isEmpty)
                println("Finished with " + ops.size + " ops upon "+ tree)
              finished = true
          }
        }
        if (ops.isEmpty && source == null)
          None
        else
          Some(new OpsStream(source, colTree, ops))
      }
    }

    val unit = compilationUnit

    var matchedColTreeIds = Set[Long]()

    override def transform(tree: Tree): Tree = {
      if (!shouldOptimize(tree))
        super.transform(tree)
      else
        try {
          tree match {
            case OpsStream(opsStream) if !opsStream.ops.isEmpty && (opsStream ne null) && (opsStream.colTree ne null) && !matchedColTreeIds.contains(opsStream.colTree.id) =>
              import opsStream._
              
              val txt = "Streamed ops on " + (if (source == null) "UNKNOWN COL" else source.tree.tpe) + " : " + ops/*.map(_.getClass.getName)*/.mkString(", ")
              matchedColTreeIds += colTree.id
              msg(unit, tree.pos, "# " + txt) {
                (Seq(source) ++ ops).collect({ case ccss: CanCreateStreamSink => ccss }).lastOption match {
                  case Some(sinkCreator) =>
                    val asm = assembleStream(source, ops, sinkCreator, super.transform _, unit, tree.pos, currentOwner, localTyper)
                    println("Found ops = " + ops)
                    //println("assembled stream with " + ops.size + " operations and sink " + sinkCreator + " out of tree " + tree + " : " + asm)
                    //super.transform(tree)
                    
                    asm
                  case _  =>
                    println("Failed to find any CanCreateStreamSink instance in source + ops !")
                    super.transform(tree)
                }
              }
            case _ =>
              super.transform(tree)//toMatch)
          }
        } catch {
          case ex =>
            ex.printStackTrace
            super.transform(tree)
        }
    }
  }
}
