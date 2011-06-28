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

import scala.collection.JavaConversions._
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import scala.concurrent.ops
import scala.io.Source
import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.reporters.Reporter
//import scalacl.ScalaCLPlugin

/*
SCALACL_TRACE=1 mvn scala:run -DmainClass=scalacl.Compile "-DaddArgs=Test.scala|-Xprint:scalacl-loopstransform|-Ybrowse:scalacl-loopstransform|-classpath|../ScalaCL2/target/scalacl-0.2-SNAPSHOT-shaded.jar"
SCALACL_TRACE=1 mvn scala:run -DmainClass=scalacl.Compile "-DaddArgs=Test.scala|-classpath|../ScalaCL2/target/scalacl-0.2-SNAPSHOT-shaded.jar"
rm Test*.class ; SCALACL_TRACE=1 sbt 'run Test.scala -classpath ../ScalaCL2/target/scalacl-0.2-SNAPSHOT-shaded.jar -Xprint:scalacl-functionstransform -Yshow-trees'
scala -cp ../ScalaCL2/target/scalacl-0.2-SNAPSHOT-shaded.jar:. Test
*/
object Compile {

  def main(args: Array[String]) {
    /*val args = Array("/Users/ochafik/ScalaCLPlugin/Test.scala",
        "-Xprint:scalacl-loopstransform"
        //"-Ydebug"
        )*/
    /*
    val base = new File("/Users/ochafik/src/scala-2.8.x/src/compiler/")
    def list(f: File): Array[File] = {
      if (f.isDirectory)
        f.listFiles.flatMap(list)
      else
        Array(f)
    }
    val args = //if (args0.length != 0) args0 else
    Array(
      "-bootclasspath",
      "/Users/ochafik/src/scala-2.8.x/build/locker/classes/library",
      "-cp",
      Array(
        "/Users/ochafik/src/scala-2.8.x/build/locker/classes/compiler"
      ).mkString(java.io.File.separator),
      //"/Users/ochafik/src/scala-2.8.x/src/compiler/scala/tools/nsc/symtab/Types.scala",
      //"/Users/ochafik/src/scala-2.8.x/src/compiler/scala/tools/nsc/typechecker/Typers.scala",
      "/Users/ochafik/src/scala-2.8.x/src/compiler/scala/tools/nsc/typechecker/Namers.scala"
    ) ++ //list(base).filter(_.getName.endsWith(".scala")).map(_.toString) ++
    Array(
      //"-Xprint:" + LoopsTransformComponent.phaseName,
      //"-Ydebug",
      "-optimise"
    )
    //*/
    /*val args = Array(
      "-cp",
      "/Users/ochafik/src/Scalala/target/scala_2.8.0/scalala_2.8.0-0.4.1-SNAPSHOT.jar",
      "/Users/ochafik/src/Scalala/src/main/scala/scalala/library/Bug.scala",
      "-Xprint:" + LoopsTransformComponent.phaseName,
      "-optimise"
      //,"-Yshow-trees"
    )*/

    compilerMain(args, true)
    /*
    compilerMain(
      if (args.isEmpty)
        Array(
          "examples/Toto.scala",
          "-d", "examples/classes",
          "-Xprint:scalacl-functionstransform",
          "-Xprint:typer",
          "-classpath", "../ScalaCL2/target/scalacl-0.2-SNAPSHOT-shaded.jar"
        ) 
      else 
        args, 
      true
    )
    */
  }
  lazy val copyrightMessage: Unit = {
    println("""ScalaCL Compiler Plugin
Copyright Olivier Chafik 2010""")
  }

  
  val fileJarURLRx = "jar:file:([^!]+)!.*".r
  lazy val bootClassPath = {
    val fileJarURLRx(scalaLibraryJar) = getClass.getClassLoader.getResource("scala/collection/immutable/List.class").toString
    scalaLibraryJar
    /*
    import java.io.File
    var scalaHomeEnv = System.getenv("SCALA_HOME")
    if (scalaHomeEnv == null)
      error("SCALA_HOME is not defined !")
    
    val scalaHome = new File(scalaHomeEnv)
    val scalaLib = new File(scalaHome, "lib")
    val scalaLibraryJar = new File(scalaLib, "scala-library.jar")
    Array(
      System.getProperty("java.class.path","."),
      scalaLibraryJar.getAbsolutePath
    ).mkString(File.pathSeparator)
    */
  }
  def compilerMain(args: Array[String], enablePlugins: Boolean) = {
    copyrightMessage
    
    val extraArgs = List(
      "-optimise",
      //"-usejavacp",
      "-bootclasspath", bootClassPath
    )
    val settings = new Settings
    
    val command = new CompilerCommand((args ++ extraArgs).toList, settings) {
      override val cmdName = "scalacl"
    }
    val pluginOptions = new ScalaCLPlugin.PluginOptions(settings)
    val runner = new ScalaCLPluginRunner(enablePlugins, settings, new ConsoleReporter(settings), pluginOptions)
    val run = new runner.Run

    if (command.ok) {
      run.compile(command.files)
    }
  }
}

class ScalaCLPluginRunner(enablePlugins: Boolean, settings: Settings, reporter: Reporter, options: ScalaCLPlugin.PluginOptions) extends Global(settings, reporter) {
  override protected def computeInternalPhases() {
    super.computeInternalPhases
    if (//false)//
      enablePlugins)
      for (phase <- ScalaCLPlugin.components(this, options))
        phasesSet += phase
  }
}