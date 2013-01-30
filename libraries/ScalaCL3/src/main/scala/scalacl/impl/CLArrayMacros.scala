package scalacl.impl
import scalacl.CLArray
import scalacl.CLFilteredArray

import language.experimental.macros
import scala.reflect.macros.Context

private[scalacl] object CLArrayMacros {
  def foreachImpl[T: c.WeakTypeTag](c: Context)(f: c.Expr[T => Unit]): c.Expr[Unit] = {
    val ff = CLFunctionMacros.convertFunction(c)(f) // TODO pass static input + output collection type
    c.universe.reify {
      val input = c.prefix.asInstanceOf[c.Expr[CLArray[T]]].splice
      input.foreach(ff.splice)
    }
  }
  def mapImpl[T: c.WeakTypeTag, U: c.WeakTypeTag](c: Context)(f: c.Expr[T => U])(io2: c.Expr[DataIO[U]], m2: c.Expr[ClassManifest[U]]): c.Expr[CLArray[U]] = {
    val ff = CLFunctionMacros.convertFunction(c)(f) // TODO pass static input + output collection type
    c.universe.reify {
      val input = c.prefix.asInstanceOf[c.Expr[CLArray[T]]].splice
      input.map(ff.splice)(io2.splice, m2.splice)
    }
  }
  def filterImpl[T: c.WeakTypeTag](c: Context)(f: c.Expr[T => Boolean]): c.Expr[CLFilteredArray[T]] = {
    val ff = CLFunctionMacros.convertFunction(c)(f) // TODO pass static input + output collection type
    c.universe.reify {
      val input = c.prefix.asInstanceOf[c.Expr[CLArray[T]]].splice
      input.filter(ff.splice)
    }
  }
}