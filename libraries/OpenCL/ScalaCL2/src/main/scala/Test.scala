package scalacl
//import scalacl._
import collection._
import impl._

/**
 * mvn exec:java -Dexec.mainClass=scalacl.Test
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
  
  
  var f: Int => Boolean = _
  var m: Int => Int = _
  var m2: Int => (Int, Int) = _
  var m2a: ((Int, Int)) => Int = _
  
  val samples = 10
    
  def same[V](a: Array[V], b: Array[V])(implicit v: ClassManifest[V]): Unit = {
    val aa = a.take(samples).toSeq
    val bb = b.take(samples).toSeq
    if (!(aa == bb)) {
      println("aa = " + aa)
      //println("a = " + a)
      /*a match { 
        case fa: CLFilteredArray[V] =>
          println("aa.presence = " + fa.presence)
        case _ =>
      }*/
      println("bb = " + bb)
      assert(false)
    }
  }
  def same[V](a: CLIndexedSeq[V], b: Traversable[V])(implicit v: ClassManifest[V]): Unit =
    same(a.toArray, b.toArray)
    

  def main(args: Array[String]) = {
    import com.nativelibs4java.opencl._
    //CLEvent.setNoEvents(true)
    
    if ("0" == System.getenv("JAVACL_CACHE_BINARIES"))
      JavaCL.setCacheBinaries(false)
      
    println("Starting...")
    implicit val context = ScalaCLContext(CLPlatform.DeviceFeature.CPU)
    println("Got context " + context.context.getDevices().mkString(", "))

    import CLArray._
    import scala.math._
    
    val n = if (args.length == 1) args(0).toInt else 100000
    val runs = 4;
    
    /*if (false)
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
    }*/
    
    
    val cla = (n until 2 * n).toCLArray
    val a = (n until 2 * n).toArray
    //println("Range array " + rngarr)
    
    //val a = CLArray(1, 2, 3, 4)
    //println("Created array " + a)

    //context.queue.finish
    
    f = (
      (x: Int) => (exp(x).toInt % 2) == 0, 
      Seq("(((int)exp((float)_)) % 2) == 0")
    ): CLFunction[Int, Boolean]
    
    m = (
      (x: Int) => (x * 2 * exp(x)).toInt, 
      Seq("(int)(_ * 2 * exp((float)_))")
    ): CLFunction[Int, Int]
    
    m2 = (
      (x: Int) => (x, x * 2), 
      Seq("_", "_ * 2")
    ): CLFunction[Int, (Int, Int)]
 
    m2a = (
      (p: (Int, Int)) => p._1 + 2 * p._2, 
      Seq("_._1 + 2 * _._2")
    ): CLFunction[(Int, Int), Int]
 
    /*val f = (x: Int) => (exp(x).toInt % 2) == 0
    val clf: CLFunction[Int, Boolean] = (f, Seq("(((int)exp((float)_)) % 2) == 0"))

    val m = (x: Int) => (x * 2 * exp(x)).toInt
    val clm: CLFunction[Int, Int] = (m, Seq("(int)(_ * 2 * exp((float)_))"))

    val m2 = (x: Int) => (x, x * 2)
    val clm2: CLFunction[Int, (Int, Int)] = (m2, Seq("_", "_ * 2"))
    */
   
    //println("Filtered array with CL function : " + fclf)
    def finished[V](block: => V): V = {
      val v = block
      if (v.isInstanceOf[impl.CLEventBoundContainer])
        v.asInstanceOf[impl.CLEventBoundContainer].waitFor
      //context.queue.finish
      v
    }
    
    println("Zipped : " + cla.zip(cla.map(m)).toArray.take(10))//.asInstanceOf[Iterable[Int]]))
    //if (false) {
      
    val clzi = times("ZipWithIndex in OpenCL", runs) { finished { cla.zipWithIndex.toCLArray } }
    val zi = times("ZipWithIndex in Scala", runs) { finished { a.zipWithIndex } }
    same(clzi, zi)
    
    val mclm = times("Map in OpenCL", runs) { finished { cla.map(m) } }
    val smm = times("Map in Scala", runs) { a.map(m) }
    val smmOpt = times("Map in Scala optimized", runs) { 
      val length = a.length
      val b = new Array[Int](length)
      var i = 0
      while (i < length) {
        b(i) = m(a(i))
        i += 1
      }
      b
    }
    same(mclm, smm)
    assert(smm.toSeq == smmOpt.toSeq)
    
    val mclm2 = times("Map2 in OpenCL", runs) { finished { cla.map(m2).toCLArray } }
    val smm2 = times("Map2 in Scala", runs) { a.map(m2) }
    same(mclm2, smm2)

    val mclmm2 = times("Map+Map2 in OpenCL", runs) { finished { cla.map(m).map(m2).toCLArray } }
    val smmm2 = times("Map+Map2 in Scala", runs) { a.map(m).map(m2) }
    same(mclmm2, smmm2)
    
    val fclf = times("Filter in OpenCL", runs) { finished { cla.filter(f).toCLArray } }
    val sff = times("Filter in Scala", runs) { a.filter(f) }
    same(fclf, sff)
    
    val fmclfm = times("Filter+Map in OpenCL", runs) { finished { cla.filter(f).map(m).toCLArray } }
    val sfmfm = times("Filter+Map  in Scala", runs) { a.filter(f).map(m) }
    same(fmclfm, sfmfm)
    
    val fmclfm2 = times("Filter+Map2 in OpenCL", runs) { finished { cla.filter(f).map(m2).toCLArray } }
    val sfmfm2 = times("Filter+Map2  in Scala", runs) { a.filter(f).map(m2) }
    same(fmclfm2, sfmfm2)
    
    //}
    //assert(ff.toArray.take(samples).toSeq == sff.take(samples).toArray.toSeq)

  }
}
