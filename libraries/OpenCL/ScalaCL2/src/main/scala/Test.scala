package scalacl
//import scalacl._
import collection._

// In IntelliJ, launch with : Ctrl + Shift + F10
object Test {
  def main(args: Array[String]) = {
    println("Starting...")
    implicit val context = new ScalaCLContext
    println("Got context " + context.context.getDevices().mkString(", "))

    import CLArray._

    val rngarr = (10 until 2000).toCLArray
    println("Range array " + rngarr)
    
    val a = CLArray(1, 2, 3, 4)
    println("Created array " + a)

    //context.queue.finish

    val f = (x: Int) => (x % 2) == 0
    val ff = a.filter(f)
    println("Filtered array with normal function : " + ff)

    val clf: CLFunction[Int, Boolean] = (f, Seq("(_ % 2) == 0"))
    val fclf = a.filter(clf)
    
    println("Filtered array with CL function : " + fclf)

    val m = (x: Int) => x * 2
    val clm: CLFunction[Int, Int] = (m, Seq("_ * 2"))
    val mm = a.map(m)
    println("Mapped array with normal function " + mm)
    val mclm = a.map(clm)
    println("Mapped array with CL function " + mclm)
  }
}
