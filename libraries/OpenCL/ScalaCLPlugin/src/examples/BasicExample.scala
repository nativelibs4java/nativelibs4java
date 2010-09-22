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
  
  /*
  def test = {
      var i = 0
      val n = 100
      while (i <= n) {
          println(i)
          i += 1
      }
  }*/
  
  /*
  var t = 0
      for (i <- 0 to 100)
        t += 2 * i
      */
  import com.nativelibs4java.opencl.JavaCL
  implicit val context = new ScalaCLContext(JavaCL.createBestContext)
  val a = CLArray[Int](1000)
  val m1 = a.mapFun(CLFun[Int, Double](Seq("_ * 2.0")))
  val m2 = a.map((_: Int) * 2.0)
  /*
  val m3 = a.map((x: Int) => {
      var t = 0
      for (i <- 0 to 100)
        t += 2
      for (i <- 10 until 200)
        t += 2
      t
  })
  
  val aa = CLArray[(Int, Int)](1000)
  val f: ((Int, Int)) => Int = { case ((x, y)) => x + y }
  val maa = aa.map(f)// { case ((x: Int, y: Int)) => x + y }*/
  
}
