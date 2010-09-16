/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package object scalacl {
  import com.nativelibs4java.opencl._
  
  def clType[T](implicit m: ClassManifest[T]) = m.erasure.getSimpleName.toLowerCase

  implicit def expression2CLFunction[K, V](expression: String)(implicit k: ClassManifest[K], v: ClassManifest[V]) =
    new CLFunction[K, V](Seq(), expression, Seq())

  implicit def expression2CLFunction[K, V](declarationsAndExpression: (Seq[String], String))(implicit k: ClassManifest[K], v: ClassManifest[V]) =
    new CLFunction[K, V](declarationsAndExpression._1, declarationsAndExpression._2, Seq())
}