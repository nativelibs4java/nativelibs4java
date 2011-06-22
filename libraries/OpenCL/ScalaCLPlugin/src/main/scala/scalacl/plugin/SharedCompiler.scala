/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl ; package plugin

import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.reporters.Reporter
    
class SharedCompiler(enablePlugins: Boolean) {
  //val runner = Compile.newCompiler(settings, enablePlugins)
  case class Compiler(extraArgs: Array[String], settings: Settings, runner: ScalaCLPluginRunner)
  def createCompiler = {
    lazy val extraArgs = Array(
      "-optimise",
      "-bootclasspath", Compile.bootClassPath
    )
    lazy val settings = new Settings
    lazy val pluginOptions = new ScalaCLPlugin.PluginOptions(settings)
    lazy val runner = new ScalaCLPluginRunner(enablePlugins, settings, new ConsoleReporter(settings), pluginOptions)

    Compiler(extraArgs, settings, runner)
  }

  import scala.concurrent.ops._
  implicit val runner = new scala.concurrent.ThreadRunner

  /// A compiler and a compiler future
  var instances: (Compiler, () => Compiler) = null
  def newInstances = {
    //implicit val runner = new scala.concurrent.ThreadRunner
    val fut = future { createCompiler }
    if (instances == null) {
      instances = (createCompiler, fut)
    } else {
      // Take last future as new compiler
      instances = (instances._2(), fut)
    }
  }
  def compiler = {
    if (instances == null)
      newInstances
    instances._1
  }
  
  lazy val isAtLeastScala29 = {
    try {
      Class.forName("scala.sys.process.Process")
      true
    } catch { case _ => false }
  }
  
  def canReuseCompilers = {
    !isAtLeastScala29 &&
    System.getenv("SCALACL_DONT_REUSE_COMPILERS") == null
  }
  def compile(args: Array[String]) = {
    //val (extraArgs, settings, runner) = createRunner

    def run {
      val Compiler(extraArgs, settings, runner) = if (canReuseCompilers) compiler else createCompiler
      val command = new CompilerCommand((args ++ extraArgs).toList, settings) {
        override val cmdName = "scalacl"
      }
      if (command.ok) {
        val run = new runner.Run
        run.compile(command.files)
      }
    }
    try {
      run
    } catch {
      case _ =>
        //println("Compilation failed, retrying with a new compiler instance")
        newInstances
        run
    }
  }
}
