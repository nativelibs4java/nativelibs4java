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
package com.nativelibs4java.scalaxy ; package pluginBase

import java.io.File
import scala.collection.immutable.Stack
import scala.reflect.generic.Names
import scala.reflect.generic.Trees

import scala.tools.nsc.Settings
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.util.parsing.input.Position

class PluginOptions(pluginDef: PluginDef, settings: Settings) {
  import pluginDef.envVarPrefix
  
  private def hasEnv(name: String, default: Boolean = false) = {
    val v = System.getenv(name)
    if (default)
      v != "0"
    else
      v == "1"
  }
  
  var test = 
    false
    
  var testOutputs = 
    collection.mutable.Map[Any, Any]()
  
  var stream = 
    hasEnv(envVarPrefix + "STREAM", true)
    
  var trace =
    settings != null && settings.debug.value ||
    hasEnv(envVarPrefix + "TRACE")
    
  var veryVerbose = 
    hasEnv(envVarPrefix + "VERY_VERBOSE")
    
  var debug = 
    hasEnv(envVarPrefix + "DEBUG")
    
  var verbose = 
    settings != null && settings.verbose.value ||
    veryVerbose ||
    hasEnv(envVarPrefix + "VERBOSE")
    
  var experimental = 
    hasEnv(envVarPrefix + "EXPERIMENTAL")
  
  var deprecated = 
    hasEnv(envVarPrefix + "DEPRECATED")
    
  var skip = System.getenv(envVarPrefix + "SKIP")
    
  lazy val explicitelyDisabled =
    "1".equals(System.getenv(envVarPrefix + "DISABLE"))

  def envVarHelp = 
    """
      """ + envVarPrefix + """DISABLE=1                   Set this environment variable to disable the plugin
      """ + envVarPrefix + """SKIP=File1,File2:line2...   Do not optimize any of the listed files (or specific lines).
                                          Can contain absolute paths or file names (can omit trailing .scala).
                                          Each file (name) may be suffixed with :line.
      """ + envVarPrefix + """VERBOSE=1                   Print details about each successful code transformation to the standard output.
      """ + envVarPrefix + """VERY_VERBOSE=1              Verbose + give details on why optimizations were not performed, and on what possible side-effects were detected.
      """ + envVarPrefix + """TRACE=1                     Display stack trace of failed optimizations (for debugging purpose).
      """ + envVarPrefix + """EXPERIMENTAL=1              Perform experimental rewrites (often slower and buggier, use only when debugging the plugin).
      """ + envVarPrefix + """DEPRECATED=1                Perform rewrite that were deprecated (deemed or proved to be slower than the original)
    """
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

  private def ifEnv[V <: AnyRef](name: String)(v: => V): V =
    if (hasEnv(name))
      v
    else
      null.asInstanceOf[V]
      
}

trait WithOptions 
{
  val global: Global
  import global._
  
  val options: PluginOptions
  
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
