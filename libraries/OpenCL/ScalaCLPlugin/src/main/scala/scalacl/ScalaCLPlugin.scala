package scalacl

import scala.collection.immutable.Stack
import scala.reflect.generic.Names
import scala.reflect.generic.Trees
import scala.tools.nsc.Global
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.symtab.Definitions

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

  val runsAfter = List[String]("namer")//refchecks")
  override val components = ScalaCLPlugin.components(global)
}

object ScalaCLPlugin {
  def components(global: Global) = List(
    new ScalaCLFunctionsTransformComponent(global),
    new RangeForeach2WhileTransformComponent(global),
    new ArrayLoopsTransformComponent(global)
  )
}
