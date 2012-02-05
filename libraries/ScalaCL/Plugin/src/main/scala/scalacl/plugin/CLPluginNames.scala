package com.nativelibs4java.scalaxy ; package common
import pluginBase._

import tools.nsc.Global
import reflect.NameTransformer
;
trait CLPluginNames extends PluginNames {
  import global._
  import definitions._
  import treeInfo.{ methPart }
  import typer.typed

  val ScalaCLPackage       = M("scalacl")
  val ScalaCLPackageClass  = ScalaCLPackage.tpe.typeSymbol
  val CLDataIOClass = C("scalacl.impl.CLDataIO")
  val CLArrayClass = C("scalacl.CLArray")
  //val CLArrayModule = getModule("scalacl.CLArray")
  val CLFunctionClass = C("scalacl.impl.CLFunction")
  val CLRangeClass = C("scalacl.CLRange")
  val CLCollectionClass = C("scalacl.CLCollection")
  val CLFilteredArrayClass = C("scalacl.CLFilteredArray")
  val scalacl_ = N("scalacl")
  val getCachedFunctionName = N("getCachedFunction")
  val Function2CLFunctionName = N("Function2CLFunction")
  val withCaptureName = N("withCapture")
}
