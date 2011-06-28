/*
SCALACL_VERBOSE=1 sbt '~run examples/Toto.scala -d examples/classes -classpath ../ScalaCL2/target/scalacl-0.2.Beta11-shaded.jar'

-Xprint:scalacl-functionstransform",
          "-Xprint:typer",
          "-classpath", "../ScalaCL2/target/scalacl-0.2-SNAPSHOT-shaded.jar"

*/
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
    
    array.map(v => {
      def sq(x: Int) = x * x
      (v, sq(v))
    })
    
    // Map to / from tuples, zip(WithIndex) :
    array.zip(array.map(_ * 2f)).zipWithIndex.map(t => {
      val ((a, b), i) = t
      (exp(a / 1000.0f).toFloat + b + i, a)
    })
    
    // Same :
    array.zip(array.map(_ * 2f)).zipWithIndex map { 
      case ((a, b), i) => 
        (exp(a / 1000.0f).toFloat + b + i, a) 
    }
    
    array.map(v => {
      var total = 0
      for (i <- 0 until 5) {
        total += 0//if (cos(v * i) < 0) (i * v) else (i - v)
      }
      total
    })
    
    /*
    array.map(v => {
  
      def someFun(x: Int) = // becomes a top-level function inside the OpenCL kernel
        exp(x / 1000).toInt
  
      var pair @ (init, foo) = { // tuples will be flattened in the OpenCL kernel
         val d = v - 10     // unless they match an OpenCL tuple type like int2
         (d * d, 1 / d)
      }
      var sum = init + 1.0
      for (i <- 0 until 10; if (i % 2) != 0) {
         sum += cos(v) * i + someFun(pair._2 - foo)
      }
      (sum, foo)
    })
    */
    
    //val ((a, b), i) = ((1, 2), 3)
    /*
    Array(1, 2, 3).map(_.toDouble)
    List(1, 2, 3).map(_.toDouble)
    (0 until 2).map(_.toDouble)
    
    val a: Set[Int] = List(1).map(_ + 1)(collection.breakOut)
    */
  }
}
