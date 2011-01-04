package scalacl
//import scalacl._
import collection._
import impl._

/**
 * mvn exec:java -Dexec.mainClass=Test
 */
object Test {
  def times[V](caption: String, n: Int)(block: => V): V = {
    val v = time(caption)(block)
    for (i <- 0 until (n - 1)) {
      time(caption)(block)
    }
    v
  }
  def time[V](caption: String)(block: => V) = {
    System.gc
    val start = System.nanoTime
    val v = block
    val time = System.nanoTime - start
    println("Time[" + caption + "] = " + (time / 1000000.0) + " msecs")
    System.gc
    v
  }
  def main(args: Array[String]) = {
    import com.nativelibs4java.opencl._
    //CLEvent.setNoEvents(true)
    
    if ("0" == System.getenv("JAVACL_CACHE_BINARIES"))
      JavaCL.setCacheBinaries(false)
      
    println("Starting...")
    implicit val context = new ScalaCLContext
    println("Got context " + context.context.getDevices().mkString(", "))

    import CLArray._
    import scala.math._
    
    val samples = 10
        
    val n = if (args.length == 1) args(0).toInt else 10
    val runs = 4;
    
    {
      val aa = (0 until n).toCLArray//new CLArray[Int](n)
      
      val ff = (x: Int) => (exp(x).toInt % 2) == 0
      val clff: CLFunction[Int, Boolean] = (ff, Seq("(((int)exp((float)_)) % 2) == 0"))

    
      val fm = (x: Int) => (x * 2 * exp(x)).toInt
      val clfm: CLFunction[Int, Int] = (fm, Seq("(int)(_ * 2 * exp((float)_))"))
      
      val r = aa.
        //filter(_ => false).
        filter(clff).
        //filter(clff).
        map(clfm)
        
      val s = r.toArray.take(samples).toSeq
      println(r.toCLArray)
      System.exit(0)
    }
    
    
    val cla = (0 until n).toCLArray
    val a = (0 until n).toArray
    //println("Range array " + rngarr)
    
    //val a = CLArray(1, 2, 3, 4)
    //println("Created array " + a)

    //context.queue.finish

    val f = (x: Int) => (exp(x).toInt % 2) == 0
    val clf: CLFunction[Int, Boolean] = (f, Seq("(((int)exp((float)_)) % 2) == 0"))

    val m = (x: Int) => (x * 2 * exp(x)).toInt
    val clm: CLFunction[Int, Int] = (m, Seq("(int)(_ * 2 * exp((float)_))"))

    val m2 = (x: Int) => (x, x * 2)
    val clm2: CLFunction[Int, (Int, Int)] = (m2, Seq("_", "_ * 2"))

    //println("Filtered array with CL function : " + fclf)
    def same[V](a: CLIndexedSeq[V], b: Traversable[V])(implicit v: ClassManifest[V]) {
      val aa = a.toArray.take(samples).toSeq
      val bb = b.toArray.take(samples).toSeq
      if (!(aa == bb)) {
        println("aa = " + aa)
        println("a = " + a)
        a match { 
          case fa: CLFilteredArray[V] =>
            println("aa.presence = " + fa.presence)
          case _ =>
        }
        println("bb = " + bb)
        assert(false)
      }
    }

    def finished[V <: impl.CLEventBoundContainer](block: => V): V = {
      val v = block
      //v.waitFor
      //context.queue.finish
      v
    }
    
    //if (false) {
      
    val fmclfm = times("Filter+Map in OpenCL", runs) { finished { cla.filter(clf).map(clm).toCLArray } }
    val sfmfm = times("Filter+Map  in Scala", runs) { a.filter(f).map(m) }
    same(fmclfm, sfmfm)
    
    val fmclfm2 = times("Filter+Map2 in OpenCL", runs) { finished { cla.filter(clf).map(clm2).toCLArray } }
    val sfmfm2 = times("Filter+Map2  in Scala", runs) { a.filter(f).map(m2) }
    same(fmclfm2, sfmfm2)
    
    val mclm = times("Map in OpenCL", runs) { finished { cla.map(clm) } }
    val smm = times("Map in Scala", runs) { a.map(m) }
    //val mm = time("Map in Scala on OpenCL data") { cla.map(m) }
    same(mclm, smm)
    
    val mclm2 = times("Map2 in OpenCL", runs) { finished { cla.map(clm2) } }
    val smm2 = times("Map2 in Scala", runs) { a.map(m2) }
    //val mm = time("Map in Scala on OpenCL data") { cla.map(m) }
    same(mclm2, smm2)
    //assert(mm.toArray.take(samples).toSeq == smm.take(samples).toArray.toSeq)

    val fclf = times("Filter in OpenCL", runs) { finished { cla.filter(clf).toCLArray } }
    val sff = times("Filter in Scala", runs) { a.filter(f) }
    //val ff = time("Filter in Scala on OpenCL data") { cla.filter(f).toArray }
    same(fclf, sff)
    
    //}
    //assert(ff.toArray.take(samples).toSeq == sff.take(samples).toArray.toSeq)

  }
}
