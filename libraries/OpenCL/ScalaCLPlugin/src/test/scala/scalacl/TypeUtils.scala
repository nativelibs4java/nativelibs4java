/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl

object TypeUtils {
  val primValues = Map(
    "Double" -> "1.0",
    "Float" -> "1f",
    "Int" -> "1",
    "Short" -> "(1: Short)",
    "Long" -> "1L",
    "Byte" -> "(1: Byte)",
    "Char" -> "'a'",
    "Boolean" -> "true")
  
  val refValues = Map(
    "String" -> "\"hello\"",
    "(Int, Int)" -> "(1, 1)"
  )

  val typeValues = primValues ++ refValues
}
