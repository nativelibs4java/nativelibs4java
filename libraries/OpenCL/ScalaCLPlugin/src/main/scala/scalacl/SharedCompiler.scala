/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl

import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.reporters.Reporter


class SharedCompiler(enablePlugins: Boolean) {
  //val runner = Compile.newCompiler(settings, enablePlugins)
  def createRunner = {
    lazy val extraArgs = Array(
      "-optimise",
      "-bootclasspath", System.getProperty("java.class.path",".")
    )
    lazy val settings = new Settings
    lazy val runner = new ScalaCLPluginRunner(enablePlugins, settings, new ConsoleReporter(settings))

    (extraArgs, settings, runner)
  }
  
  var instance: (Array[String], Settings, ScalaCLPluginRunner) = null

  def compile(spawnNewInstance: Boolean, args: Array[String]) = {
    //val (extraArgs, settings, runner) = createRunner

    if (instance == null || spawnNewInstance)
      instance = createRunner
    
    val (extraArgs, settings, runner) = instance
    val command = new CompilerCommand((args ++ extraArgs).toList, settings) {
      override val cmdName = "scalacl"
    }
    if (command.ok) {
      val run = new runner.Run
      run.compile(command.files)
    }
  }
}
object SharedCompilerWithPlugins extends SharedCompiler(true)
object SharedCompilerWithoutPlugins extends SharedCompiler(false)
