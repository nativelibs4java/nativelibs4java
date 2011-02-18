object Toto {
  def main(args: Array[String]) {
    //new scala.collection.mutable.ArrayBuilder.ofRef[Array[Int]]
    //for ((i, j) <- Array(1 -> 1, 2 -> 2)) println(i, j)
    
    //Seq(1, 2, 3).zipWithIndex map { case (a, i) => a + i }
    
    import scalacl._
    import scala.math._
    import impl._
    implicit val context = new Context//(com.nativelibs4java.opencl.CLPlatform.DeviceFeature.CPU)
    //val ff: CLFunction[Int, Int] = (_: Int) + 1
    val array = CLArray(1, 2, 3, 4)
    
    val initTotal = 10
    
    println(array.map(v => {
      var total = 0//initTotal
      for (i <- -5 until 5 by 2) {
        total += (if (cos(v * i) < 0) (i * v) else (i - v))
        if ((total % 2) == 0)
          total += 1
      }
      total
    }))
    
    //val ((a, b), i) = ((1, 2), 3)
    /*
    Array(1, 2, 3).map(_.toDouble)
    List(1, 2, 3).map(_.toDouble)
    (0 until 2).map(_.toDouble)
    
    val a: Set[Int] = List(1).map(_ + 1)(collection.breakOut)
    */
  }
}
