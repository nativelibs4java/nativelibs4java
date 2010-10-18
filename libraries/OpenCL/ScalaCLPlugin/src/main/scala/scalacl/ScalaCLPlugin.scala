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

import java.io.File
import scala.collection.immutable.Stack
import scala.reflect.generic.Names
import scala.reflect.generic.Trees
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.symtab.Definitions
import scala.util.parsing.input.Position

/**
 * http://www.scala-lang.org/node/140
 * http://lamp.epfl.ch/~emir/bqbase/2005/06/02/nscTutorial.html
 * http://code.google.com/p/simple-build-tool/wiki/CompilerPlugins
 * mvn scala:run -DmainClass=scalacl.Compile "-DaddArgs=-d|out|src/main/examples/BasicExample.scala|-Xprint:scalaclfunctionstransform"
 * scala -cp target/scalacl-compiler-1.0-SNAPSHOT-shaded.jar scalacl.Main -d out src/examples/BasicExample.scala
 * javap -c -classpath out/ scalacl.examples.BasicExample
 */
class ScalaCLPlugin(val global: Global) extends Plugin {
  override val name = "ScalaCL Optimizer"
  override val description =
    "This plugin transforms some Scala functions into OpenCL kernels (for CLCol[T].map and filter's arguments), so they can run on a GPU.\n" +
  "It will also soon feature autovectorization of ScalaCL programs, detecting parallelizable loops and unnecessary collection creations."

  val runsAfter = List[String]("namer")

  lazy val explicitelyDisabled = "1".equals(System.getenv("DISABLE_SCALACL_PLUGIN")) || "true".equals(System.getProperty("scalacl.plugin.disable"))

  var enabled = !explicitelyDisabled
  override def processOptions(options: List[String], error: String => Unit) = {
    for (option <- options) {
      println("Found option " + option)
      // WE NEVER PASS HERE, WTF ???
    }
  }
  override val optionsHelp: Option[String] = Some(
"""
  DISABLE_SCALACL_PLUGIN=1            Set this environment variable to disable the plugin
  SCALACL_SKIP=File1,File2:line2...   Do not optimize any of the listed files (or specific lines).
                                      Can contain absolute paths or file names (can omit trailing .scala).
                                      Each file (name) may be suffixed with :line.
  SCALACL_TRACE=1                     Display stack trace of failed optimizations (for debugging purpose).
"""
  )
  
  import ScalaCLPlugin._
  val fileAndLineOptimizationFilter: (String, Int) => Boolean = {
    var skip = System.getenv("SCALACL_SKIP")
    if (skip == null)
      skip = ""
    else
      skip = skip.trim
    //println("[scalacl] SCALACL_SKIP = " + skip)
    if (skip == "")
      (path: String, line: Int) => true
    else {
      skip.split(',').map(item => {
        val s = item.split(':')
        val f = s(0)
        val pathFilter: String => Boolean = {
          val file = new File(f)
          if (file.exists) {
            val absFile = file.getAbsolutePath
            (path: String) => new File(path).getAbsolutePath != absFile
          } else {
            val n = file.getName
            if (!n.toLowerCase.endsWith(".scala")) {
              val ns = n + ".scala"
              (path: String) => {
                val fn = new File(path).getName
                fn != n && fn != ns
              }
            } else {
              (path: String) => {
                val fn = new File(path).getName
                fn != n
              }
            }
          }
        }
        if (s.length == 2) {
          val skippedLine = s(1).toInt
          (path: String, line: Int) => path == null || line != skippedLine && pathFilter(path)
        } else {
          (path: String, line: Int) => path == null || pathFilter(path)
        }
      }).reduceLeft[FileAndLineOptimizationFilter] {
        case (f1: FileAndLineOptimizationFilter, f2: FileAndLineOptimizationFilter) => (path: String, line: Int) => f1(path, line) && f2(path, line)
      }
    }

  }
  override val components = if (enabled)
    ScalaCLPlugin.components(global, fileAndLineOptimizationFilter)
  else
    Nil
}

object ScalaCLPlugin {
  lazy val trace = //true
    "1".equals(System.getenv("SCALACL_TRACE"))
  
  type FileAndLineOptimizationFilter = (String, Int) => Boolean
  def components(global: Global, fileAndLineOptimizationFilter: FileAndLineOptimizationFilter) = List(
    /*
    if (System.getenv("SCALACL_FUSEOPS") == null) null else
      new OpsFuserTransformComponent(global, fileAndLineOptimizationFilter),
    if (System.getenv("SCALACL_SEQ2ARRAY") == null) null else
      new Seq2ArrayTransformComponent(global, fileAndLineOptimizationFilter),
    */
    new ScalaCLFunctionsTransformComponent(global, fileAndLineOptimizationFilter),
    new LoopsTransformComponent(global, fileAndLineOptimizationFilter)
  ).filter(_ != null)
}

trait WithOptimizationFilter {
  val global: Global
  import global._
  val fileAndLineOptimizationFilter: ScalaCLPlugin.FileAndLineOptimizationFilter

  def shouldOptimize(tree: Tree) = {
    val pos = tree.pos
    try {
      !pos.isDefined || fileAndLineOptimizationFilter(pos.source.path, pos.line)
    } catch {
      case ex =>
        ex.printStackTrace
        true
    }
  }
}