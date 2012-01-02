
package com.nativelibs4java.scalace ; package components
import common._
import pluginBase._

import scala.tools.nsc.Global

import scala.tools.nsc.plugins.PluginComponent
import scala.tools.nsc.transform.{Transform, TypingTransformers}

object MyComponent {
  val runsAfter = List[String](
    "typer"
  )
  val runsBefore = List[String]()// LoopsTransformComponent.phaseName)
  val phaseName = "myphase"
}

class MyComponent(val global: Global, val options: PluginOptions)
extends PluginComponent
   with Transform
   with TypingTransformers
   
   with MiscMatchers
   with TreeBuilders
   with WorkaroundsForOtherPhases
   
   with WithOptions
{
  import global._
  import global.definitions._
  import scala.tools.nsc.symtab.Flags._
  import typer.{typed, atOwner}    // methods to type trees

  override val runsAfter = StreamTransformComponent.runsAfter
  override val runsBefore = StreamTransformComponent.runsBefore
  override val phaseName = StreamTransformComponent.phaseName

  def newTransformer(compilationUnit: CompilationUnit) = new TypingTransformer(compilationUnit) {

    val unit = compilationUnit

    val ofDimName = N("ofDim")
  
    override def transform(tree: Tree): Tree = {
      tree match {
        case Apply(TypeApply(Select(_, ofDimName()), List(bTypeTree)), List(firstDimTree)) =>
          println("FOUND SOME ARRAY OFDIM = " + tree)
          super.transform(tree)
        case _ =>
          super.transform(tree)
      }
    }
  }
}
