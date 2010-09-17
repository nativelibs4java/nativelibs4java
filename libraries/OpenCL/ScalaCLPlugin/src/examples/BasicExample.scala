package scalacl.examples

import scalacl._

/** An example demonstrating the fancy features of the new
 *  compiler plugin.
 */
class BasicExample {
  /*def foo = {
    val i = add(10, 11)    
  }
  
  def add(v: Int, w: Int) = v + w
  
  val col = Seq[Int]()
  val mapped = col.map(_ * 2)
  
  val j = 15
  val mapped2 = col.map(v => {
      var r = 0
      for (i <- 0 until 100) {
          r += i
      }
      r * 2 * j
  })*/
  
  import com.nativelibs4java.opencl.JavaCL
  implicit val context = new ScalaCLContext(JavaCL.createBestContext)
  val a = CLArray[Int](1000)
  val m1 = a.map[Double]("_ * 2.0")
  val m = a.map((_: Int) * 2.0)
}
