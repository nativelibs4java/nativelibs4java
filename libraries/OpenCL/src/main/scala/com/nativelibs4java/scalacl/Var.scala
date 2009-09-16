/*
 * ScalaCL2.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.nativelibs4java.scalacl

import scala.reflect.Manifest
import scala.collection.immutable._

import com.nativelibs4java.opencl.OpenCL4Java._
import com.nativelibs4java.scalacl.SyntaxUtils._

/// fwd declare
class CLEvent

abstract sealed class VarMode {
  def union(mode: VarMode): VarMode;
}
case object UnknownMode extends VarMode {
  override def union(mode: VarMode) = mode
}

case object ReadMode extends VarMode {
  override def union(mode: VarMode) = if (mode == AggregatedMode) AggregatedMode else if (mode == WriteMode) ReadWriteMode else this
}
case object WriteMode extends VarMode {
  override def union(mode: VarMode) = if (mode == AggregatedMode) AggregatedMode else if (mode == WriteMode) ReadWriteMode else this
}
case object ReadWriteMode extends VarMode {
  override def union(mode: VarMode) = if (mode == AggregatedMode) AggregatedMode else this
}
case object AggregatedMode extends VarMode {
  override def union(mode: VarMode) = this
}

abstract class AbstractVar extends Expr {
  var kernel: CLKernel = null;
  var argIndex = -2;
  var name: String = null;
  var mode: VarMode = UnknownMode;

  def setup
  override def toString() = name
  override def accept(visitor: (Expr, Stack[Expr]) => Unit, stack: Stack[Expr]) : Unit = {
    visitor(this, stack)
  }
  
  def getTypeDesc[T](implicit t: Manifest[T], valueType: ValueType) = {
    var ch = {
      if (valueType.isInstanceOf[Val1]) 1
      else if (valueType.isInstanceOf[Val2]) 2
      else if (valueType.isInstanceOf[Val4]) 4
      else throw new IllegalArgumentException("Unable to guess channels for valueType " + valueType)
    }
    var c = t.erasure
    var pt = {
      if (c.isAnyOf(classOf[Int1], classOf[Int2], classOf[Int4])) IntType
      else if (c.isAnyOf(classOf[Double1], classOf[Double2], classOf[Double4])) DoubleType
      else throw new IllegalArgumentException("Unable to guess primType for class " + c.getName)
    }
    TypeDesc(ch, valueType, pt)
  }
}
class Var[T](implicit t: Manifest[T]) extends AbstractVar {
  private var value: Option[T] = None;
  def apply() : T = {
    value getOrElse { throw new RuntimeException("Cannot get variable value before setting things up !")}
  }
  override def typeDesc = getTypeDesc[T](t, Scalar)

  def defaultValue[K](implicit k: Manifest[K]): K = {
    var c = k.erasure;
    (
      if (c == classOf[Int])
        0
      else if (c == classOf[Double])
        0.0
      else if (c == classOf[Float])
        0.0f
      else if (c == classOf[Long])
        0l
      else if (c == classOf[Short])
        0.asInstanceOf[Short]
      else
        c.newInstance()
    ).asInstanceOf[K]
  }
  override def setup = {
    var value = if (this.value == None) defaultValue[T] else this.value
    kernel.setObjectArg(argIndex, value)
  }
}
import java.nio._
class ArrayVar[T](implicit t: Manifest[T]) extends AbstractVar {
  override def setup = {

  }
  override def typeDesc = getTypeDesc[T](t, Parallel)
  private var mem: Option[CLMem] = None;
  private var value: Option[T] = None;

  def apply(index: Expr) : Expr = new ArrayElement[T](this, index)
  
  def apply(index: Int) : T = {
    var value = this.value getOrElse { throw new RuntimeException("Cannot get variable value before setting things up !")}
    return value.asInstanceOf[DoubleBuffer].get(index).asInstanceOf[T];
  }
}

class ArrayElement[T](/*implicit t: Manifest[T], */var array: ArrayVar[T], var index: Expr) extends Expr {
  override def typeDesc = {
    var td = array.typeDesc
    TypeDesc(td.channels, Scalar, td.primType)
  }
  override def toString() = array + "[" + index + "]"
  override def accept(visitor: (Expr, Stack[Expr]) => Unit, stack: Stack[Expr]) : Unit = {
    stack push this;
    visitor(array, stack)
    visitor(index, stack)
    stack pop;
    visitor(this, stack)
  }
  
}

import java.awt.image._
import java.awt._
class ImageVar extends AbstractVar {
  override def setup = throw new UnsupportedOperationException("IMPLEMENT ME: ImageVar.setup")
  override def typeDesc = throw new UnsupportedOperationException("IMPLEMENT ME: ImageVar.typeDesc")
}