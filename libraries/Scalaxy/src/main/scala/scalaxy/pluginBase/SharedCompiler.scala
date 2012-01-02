package com.nativelibs4java.scalaxy ; package pluginBase

import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.reporters.Reporter
    

class SharedCompiler(enablePlugins: Boolean, pluginDef: PluginDef) {
  case class Compiler(
    extraArgs: Array[String], 
    settings: Settings,
    pluginOptions: PluginOptions,
    runner: PluginRunner
  )
  def createCompiler: Compiler = {
    val settings = new Settings
    val pluginOptions = pluginDef.createOptions(settings)
    pluginOptions.test = true
    
    val runner = new PluginRunner(if (enablePlugins) Some(pluginDef) else None, pluginOptions, settings, new ConsoleReporter(settings))
    Compiler(CompilerMain.extraArgs, settings, pluginOptions, runner) 
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
    System.getenv("SCALAXY_DONT_REUSE_COMPILERS") == null
  }
  def compile(args: Array[String]): PluginOptions = {
    //val (extraArgs, settings, runner) = createRunner

    def run = {
      val Compiler(extraArgs, settings, pluginOptions, runner) = if (canReuseCompilers) compiler else createCompiler
      val command = new CompilerCommand((args ++ extraArgs).toList, settings) {
        override val cmdName = "scalacl"
      }
      if (command.ok) {
        val run = new runner.Run
        run.compile(command.files)
      }
      pluginOptions
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

