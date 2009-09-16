/*
 * SyntaxUtils.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.nativelibs4java.scalacl
import java.nio._

object SyntaxUtils {


  def implode(elts: List[String], sep: String) = {
    if (elts == Nil) ""
    else elts reduceLeft { _ + sep + _ } //map { _.toString }

  }

  implicit def class2ClassUtils(c: Class[_]) = ClassUtils(c)
  case class ClassUtils(var target: Class[_]) {
    def isBuffer() = classOf[Buffer] isAssignableFrom target
    def isAnyOf(matches: Class[_]*) : Boolean = {
      for (m <- matches.elements)
      if (m isAssignableFrom target)
      return true
      return false
    }
  }

}
