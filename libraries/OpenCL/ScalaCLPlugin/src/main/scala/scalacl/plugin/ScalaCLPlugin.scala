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
import old._

import java.io.File
import scala.collection.immutable.Stack
import scala.reflect.generic.Names
import scala.reflect.generic.Trees

import scala.tools.nsc.Settings
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.util.parsing.input.Position

/**
 * http://www.scala-lang.org/node/140
 * http://lamp.epfl.ch/~emir/bqbase/2005/06/02/nscTutorial.html
 * http://code.google.com/p/simple-build-tool/wiki/CompilerPlugins
 * mvn scala:run -DmainClass=scalacl.plugin.Compile "-DaddArgs=-d|out|examples/Toto.scala|-Xprint:scalacl-functionstransform|-classpath|../ScalaCL/target/scalacl-0.3-SNAPSHOT-shaded.jar"
 * scala -cp target/scalacl-compiler-1.0-SNAPSHOT-shaded.jar scalacl.plugin.Compile -d out src/examples/BasicExample.scala
 * javap -c -classpath out/ scalacl.examples.BasicExample
 */
class ScalaCLPlugin(val global: Global) extends Plugin {
  override val name = "ScalaCL Optimizer"
  override val description =
    "This plugin transforms some Scala functions into OpenCL kernels (for CLCollection[T].map and filter's arguments), so they can run on a GPU.\n" +
  "It will also soon feature autovectorization of ScalaCL programs, detecting parallelizable loops and unnecessary collection creations."

  val runsAfter = List[String]("namer")

  lazy val explicitelyDisabled =
    "1".equals(System.getenv("SCALACL_DISABLE")) ||
    "1".equals(System.getenv("DISABLE_SCALACL_PLUGIN")) || "true".equals(System.getProperty("scalacl.plugin.disable"))

  var enabled = !explicitelyDisabled
  
  val pluginOptions = new ScalaCLPlugin.PluginOptions(global.settings)
  override def processOptions(options: List[String], error: String => Unit) = {
    for (option <- options) {
      println("Found option " + option)
      // WE NEVER PASS HERE, WTF ???
    }
  }
  override val optionsHelp: Option[String] = Some(
"""
  SCALACL_DISABLE=1                   Set this environment variable to disable the plugin
  SCALACL_SKIP=File1,File2:line2...   Do not optimize any of the listed files (or specific lines).
                                      Can contain absolute paths or file names (can omit trailing .scala).
                                      Each file (name) may be suffixed with :line.
  SCALACL_VERBOSE=1                   Print details about each successful code transformation to the standard output.
  SCALACL_TRACE=1                     Display stack trace of failed optimizations (for debugging purpose).
  SCALACL_EXPERIMENTAL=1              Perform experimental rewrites (often slower and buggier, use only when debugging ScalaCLPlugin).
  SCALACL_DEPRECATED=1                Perform rewrite that were deprecated (deemed or proved to be slower than the original)
"""
  )
  
  override val components = if (enabled)
    ScalaCLPlugin.components(global, pluginOptions)
  else
    Nil
}

object ScalaCLPlugin {
  private def hasEnv(name: String) =
    "1" == System.getenv(name)

  class PluginOptions(settings: Settings) {

    var test = 
      false
      
    var testOutputs = 
      collection.mutable.Map[Any, Any]()
    
    var stream = 
      hasEnv("SCALACL_STREAM")
      
    var trace =
      settings != null && settings.debug.value ||
      hasEnv("SCALACL_TRACE")
      
    var verbose = 
      settings != null && settings.verbose.value ||
      hasEnv("SCALACL_VERBOSE")
      
    var experimental = 
      hasEnv("SCALACL_EXPERIMENTAL")
    
    var deprecated = 
      hasEnv("SCALACL_DEPRECATED")
      
    var skip = System.getenv("SCALACL_SKIP")
      
    type FileAndLineOptimizationFilter = (String, Int) => Boolean
  
    lazy val fileAndLineOptimizationFilter: FileAndLineOptimizationFilter = {
      var skipVar = skip
      if (skipVar == null)
        skipVar = ""
      else
        skipVar = skip.trim
      //println("[scalacl] SCALACL_SKIP = " + skipVar)
      if (skipVar == "")
        (path: String, line: Int) => true
      else {
        skipVar.split(',').map(item => {
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
          if (s.length == 2 && (s(1) ne null)) {
            val skippedLine = s(1).toInt
            (path: String, line: Int) => { 
              val v = path == null || !(line == skippedLine && !pathFilter(path))
              //println(path + ":" + line + " = " + v)
              v
            }
          } else {
            (path: String, line: Int) => { 
              val v = path == null || pathFilter(path)
              //println(path + ":" + line + " = " + v)
              v
            }
          }
        }).reduceLeft[FileAndLineOptimizationFilter] {
          case (f1: FileAndLineOptimizationFilter, f2: FileAndLineOptimizationFilter) => 
            (path: String, line: Int) => {
              f1(path, line) && f2(path, line)
            }
        }
      }
    }
  }

  private def ifEnv[V <: AnyRef](name: String)(v: => V): V =
    if (hasEnv(name))
      v
    else
      null.asInstanceOf[V]

  def components(global: Global, options: PluginOptions) = List(
    //new MyComponent(global, options),
    new StreamTransformComponent(global, options),
    if (!options.stream)
      new LoopsTransformComponent(global, options)
    else
      null,
    try {
      new ScalaCLFunctionsTransformComponent(global, options)
    } catch { 
      case ex: scala.tools.nsc.MissingRequirementError =>
        if (options.verbose)
          println("[scalacl] ScalaCL Collections library not in the classpath : won't perform Scala -> OpenCL transforms.")
        //if (options.trace)
        //  ex.printStackTrace
        null
      case _ =>
        null // TODO
    }
  ).filter(_ != null)
}

trait WithOptions 
{
  val global: Global
  import global._
  
  val options: ScalaCLPlugin.PluginOptions
  
  def shouldOptimize(tree: Tree) = {
    val pos = tree.pos
    try {
      !pos.isDefined || options.fileAndLineOptimizationFilter(pos.source.path, pos.line)
    } catch {
      case ex =>
        //ex.printStackTrace
        true
    }
  }
}