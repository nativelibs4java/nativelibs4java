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
package com.nativelibs4java.scalace ; package plugin
import pluginBase._
import components._

import java.io.File
import scala.collection.immutable.Stack
import scala.reflect.generic.Names
import scala.reflect.generic.Trees

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.Settings
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.util.parsing.input.Position

/**
 * http://www.scala-lang.org/node/140
 * http://lamp.epfl.ch/~emir/bqbase/2005/06/02/nscTutorial.html
 * http://code.google.com/p/simple-build-tool/wiki/CompilerPlugins
 
 * mvn scala:run -DmainClass=com.nativelibs4java.scalace.plugin.Compile "-DaddArgs=-d|out|examples/Test.scala|-Xprint:scalacl-stream"
 
 |-classpath|../ScalaCL/target/scalacl-0.3-SNAPSHOT-shaded.jar"
 * scala -cp target/scalacl-compiler-1.0-SNAPSHOT-shaded.jar scalacl.plugin.Compile -d out src/examples/BasicExample.scala
 * javap -c -classpath out/ scalacl.examples.BasicExample
 */
object ScalacePluginDef extends PluginDef {
  override val name = "Scalace"
  override val description =
    "This plugin rewrites some Scala loop-like constructs so they execute faster (e.g. Array.map, .foreach, .filter...)."
    
  override def envVarPrefix = "SCALACE_"
  
    //"This plugin transforms some Scala functions into OpenCL kernels (for CLCollection[T].map and filter's arguments), so they can run on a GPU.\n" +
    //"It will also soon feature autovectorization of ScalaCL programs, detecting parallelizable loops and unnecessary collection creations."

  override def createOptions(settings: Settings): PluginOptions =
    new PluginOptions(this, settings)
    
  override def createComponents(global: Global, options: PluginOptions): List[PluginComponent] =
    List(
      //new MyComponent(global, options),
      new StreamTransformComponent(global, options),
      if (!options.stream)
        new LoopsTransformComponent(global, options)
      else
        null
    )
      
  override def getCopyrightMessage: String =
    "Scalace Plugin\nCopyright Olivier Chafik 2010-2012"
}

class ScalacePlugin(override val global: Global) 
extends PluginBase(global, ScalacePluginDef)

object Compile extends CompilerMain {
  override def pluginDef = ScalacePluginDef
  override def commandName = "scalace"
}

