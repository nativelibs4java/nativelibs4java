/*
 * Created by IntelliJ IDEA.
 * User: ochafik
 * Date: 10/05/11
 * Time: 15:20
 */
package scalacl ; package plugin

import tools.nsc.plugins.PluginComponent
import tools.nsc.Global

trait StreamImpls extends Streams {
  this: PluginComponent with WithOptions with WorkaroundsForOtherPhases =>

  val global: Global
  import global._
  import definitions._
  import treeInfo.{ methPart }
  import typer.typed

}