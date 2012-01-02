/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.scalaxy ; package test
import plugin._

trait TypeUtils {

  lazy val primValues = Array(
    "Double" -> "1.0",
    "Float" -> "1f",
    "Int" -> "1",
    "Short" -> "(1: Short)",
    "Long" -> "1L",
    "Byte" -> "(1: Byte)",
    "Char" -> "'a'",
    "Boolean" -> "true")

  lazy val primValuesList =
    primTypeNames.map(p => (p, if (p == "Boolean") "true, true, false" else Array(1, 2, 3).map("(" + _ + ": " + p + ")").mkString(", ")))

  lazy val refValuesList = Array(
    "String" -> "\"a\", \"b\", \"c\"",
    "List[Int]" -> "List(1), List(2), List(3)",
    "(Int, Int)" -> "(1, 1), (2, 2), (3, 3)"
  )

  lazy val trivialRefValues = Array(
    "String" -> "\"hello\""
  )
  lazy val refValues = trivialRefValues ++ Array(
    "List[Int]" -> "List(1)",
    "(Int, Int)" -> "(1, 1)"
  )
  lazy val refTypeNames = refValues.map(_._1)
  lazy val primTypeNames = primValues.map(_._1)

  lazy val typeValues = primValues ++ refValues
  lazy val typeNames = primTypeNames ++ refTypeNames

}
