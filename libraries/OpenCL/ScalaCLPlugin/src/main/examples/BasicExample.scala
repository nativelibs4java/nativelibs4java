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
      
  def main(args: Array[String]): Unit = {
    implicit val context = new ScalaCLContext(null)
  
    val a = CLArray[Int](10)
    //val m1 = a.mapFun(CLFun[Int, Double](Seq("_ * 2.0")))
    val m2 = a.map(_ + 2.0).map(_ * 2.0)
    println("m2 = " + m2.toSeq)
      
    val r: CLLongRange = 
          0 until 10
          
    val m: CLArray[(Long, Float)] = 
      r.map(i => (i, cos(i).toFloat))

    val mm = 
      m.map { case (i, c) => i + c }
    
    val oddM = m.filter((p: (Long, Float)) => (p._1 % 2) == 0) // compiler knows we're gonna compact this filter to a Seq (without ever calling refineFilter on it), so it can add a .precompact call here that will queue the compact operation in background 
    
    val oddM2 = m.filter { case (i, c) => (i % 2) == 0 } 
    
    println("m = " + m.toSeq)
    println("mm = " + mm.toSeq)
    println("oddM = " + oddM.toSeq)
    println("oddM2 = " + oddM2.toSeq)
    
  }
  
  
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
