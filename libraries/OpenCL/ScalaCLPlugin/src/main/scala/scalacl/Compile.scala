package scalacl

import java.io.File
import scala.tools.nsc.CompilerCommand
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.reporters.Reporter
//import scalacl.ScalaCLPlugin

object Compile {
  def main(args: Array[String]) {
      println("""ScalaCL Compiler Plugin
Copyright Olivier Chafik 2010""")
      
    val settings = new Settings

    val scalaLib = "/Users/ochafik/bin/scala-2.8.0.final/lib/"
    val extraArgs = List(
      "-bootclasspath", scalaLib + "scala-library.jar:" + scalaLib + "scala-compiler.jar",
      //"-Ybrowse:scalaclfunctionstransform",
      //"-uniqid",
      "-classpath",
      Seq(
        //"/Users/ochafik/nativelibs4javaBridJed/OpenCL/ScalaCL2/target/classes",
        //"/Users/ochafik/nativelibs4javaBridJed/OpenCL/ScalaCL2/target/scala_2.8.0/classes"
        "/Users/ochafik/nativelibs4javaBridJed/OpenCL/ScalaCL2/target/scalacl2-bridj-1.0-SNAPSHOT-shaded.jar"
      ).mkString(File.pathSeparator)
    )
    settings.debug.value = true

    val command = new CompilerCommand((args ++ extraArgs).toList, settings) {
      override val cmdName = "scalacl"
    }

    if (command.ok) {
      //settings.debug.value = false

      class ScalaCLPluginRunner(settings: Settings, reporter: Reporter) extends Global(settings, reporter) {
        /*import scalacl.ScalaCLAnnotationChecker
        val annotChecker = new ScalaCLAnnotationChecker { val global: ScalaCLPluginRunner.this.type = ScalaCLPluginRunner.this }
        addAnnotationChecker(annotChecker.checker)*/
        override protected def computeInternalPhases() {
          super.computeInternalPhases
          for (phase <- ScalaCLPlugin.components(this))
            phasesSet += phase

          for (phase <- NaiveOptimizerPlugin.components(this))
            phasesSet += phase
        }
      }
      val runner = new ScalaCLPluginRunner(settings, new ConsoleReporter(settings))
      val run = new runner.Run
      run.compile(command.files)
    }
  }
}
