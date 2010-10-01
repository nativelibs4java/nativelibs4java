package scalacl

import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin

/**
 * http://www.scala-lang.org/node/140
 * http://lamp.epfl.ch/~emir/bqbase/2005/06/02/nscTutorial.html
 * http://code.google.com/p/simple-build-tool/wiki/CompilerPlugins
 * mvn scala:run -DmainClass=scalacl.Compile "-DaddArgs=-d|out|src/main/examples/BasicExample.scala|-Xprint:scalaclfunctionstransform"
 * scala -cp target/scalacl-compiler-1.0-SNAPSHOT-shaded.jar scalacl.Main -d out src/examples/BasicExample.scala
 * javap -c -classpath out/ scalacl.examples.BasicExample
 */
class NaiveOptimizerPlugin(val global: Global) extends Plugin {
  override val name = "Naive Optimizer Plugin"
  override val description =
    "This plugin transforms foreach loops on integer ranges into equivalent while loops, which execute much faster."

  val runsAfter = RangeForeach2WhileTransformComponent.runsAfter
  override val components = NaiveOptimizerPlugin.components(global)
}

object NaiveOptimizerPlugin {
  def components(global: Global) = List(new RangeForeach2WhileTransformComponent(global))
}
