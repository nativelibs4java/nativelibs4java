package scalacl
//import scalacl._

import impl._
import scala.math._
    
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
  var m2join: ((Int, Int)) => Int = _
  var m2join2: ((Int, Int)) => Int = _
  
  val samples = 10
    
  def same[V](a: Array[V], b: Array[V], fa: CLFilteredArray[V] = null)(implicit v: ClassManifest[V]): Unit = {
    val aa = a.take(samples).toSeq
    val bb = b.take(samples).toSeq
    if (!(aa == bb)) {
      println("aa = " + aa)
      //println("a = " + a)
      if (fa != null) {
        println("aa.presence = " + fa.presence.toArray.take(samples).toSeq)
      }
      println("bb = " + bb)
      assert(false)
    }
  }
  def same[V](a: CLIndexedSeq[V], b: Traversable[V])(implicit v: ClassManifest[V]): Unit =
    same(a.toArray, b.toArray, if (a.isInstanceOf[CLFilteredArray[V]]) a.asInstanceOf[CLFilteredArray[V]] else null)

  def main(args: Array[String]) = {
    import com.nativelibs4java.opencl._
    //CLEvent.setNoEvents(true)
    
    //if ("0" == System.getenv("JAVACL_CACHE_BINARIES"))
    //  JavaCL.setCacheBinaries(false)
      
    println("Starting...")
    /*val clContext = JavaCL.createBestContext(
      CLPlatform.DeviceFeature.CPU,
      CLPlatform.DeviceFeature.OutOfOrderQueueSupport, 
      CLPlatform.DeviceFeature.MaxComputeUnits
    )*/
    //val clQueue = clContext.createDefaultOutOfOrderQueueIfPossible
    implicit val context = ScalaCLContext(
      //CLPlatform.DeviceFeature.CPU,
      CLPlatform.DeviceFeature.OutOfOrderQueueSupport, 
      CLPlatform.DeviceFeature.MaxComputeUnits
    )
    //implicit val context = new ScalaCLContext(clContext, clQueue)//CLPlatform.DeviceFeature.GPU)
    println("Got context " + context.context.getDevices().mkString(", "));

    if (System.getenv("TEST") != null)
    {
      val filt = (
        (x: Int) => (x % 2) == 0, 
        Seq("(_ % 2) == 0")
      ): CLFunction[Int, Boolean]
      
      for (dim <- 1 until 1000) {
        for (offset <- 0 to 1) {
          val opencl = (0 until dim).toCLArray.filter(filt).toArray.toSeq
          val scala = (0 until dim).toArray.filter(filt).toArray.toSeq
          
          if (opencl != scala) {
            println("ERROR dim = " + dim + ", offset = " + offset + " !")
            println("\tExpected : " + scala)
            println("\t   Found : " + opencl)
          } //else println("OK dim = " + dim)
        }
      }
      System.exit(0)
    }
    val aaa = (1 to 100000).cl
    val bbb = Array(1, 2, 3).cl
    
    import CLArray._
    
    val n = if (args.length >= 1) args.last.toInt else 100000
    val runs = 5;
    
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
    
    
    val cla = (0 until n).toCLArray
    val a = (0 until n).toArray
    //TODO TEST val cla = (n until 2 * n).toCLArray
    //val a = (n until 2 * n).toArray
    
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
 
    m2join = (
      (p: (Int, Int)) => p._1 + 2 * p._2, 
      Seq("_._1 + 2 * _._2")
    ): CLFunction[(Int, Int), Int]
 
    m2join2 = (
      (p: (Int, Int)) => (atan2(p._1, p._2) * 1000).toInt, 
      Seq("(int)(atan2((float)_._1, (float)_._2) * 1000)")
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
    
    //println("Zipped : " + cla.zip(cla.map(m)).toArray.take(10).toSeq)//.asInstanceOf[Iterable[Int]]))
      
    same(
      times("Map2+MapJoin2 in OpenCL", runs) { finished { cla.map(m2).map(m2join2).toArray } },
      times("Map2+MapJoin2 in Scala", runs) { a.map(m2).map(m2join2) }
    )
    times("Map2+MapJoin2 in Scala+views", runs) { a.view.map(m2).map(m2join2).toArray }

    same(
      times("Map2+MapJoin1 in OpenCL", runs) { finished { cla.map(m2).map(m2join).toArray } },
      times("Map2+MapJoin1 in Scala", runs) { a.map(m2).map(m2join) }
    )
    times("Map2+MapJoin1 in Scala+views", runs) { a.view.map(m2).map(m2join).toArray }

    same(
      times("Zip in OpenCL", runs) { finished { cla.zip(cla).toArray } },
      times("Zip in Scala", runs) { finished { a.zip(a) } }
    )
    times("Zip in Scala+views", runs) { finished { a.view.zip(a).toArray } }
    
    same(
      times("Zip+MapJoin1 in OpenCL", runs) { finished { cla.zip(cla).map(m2join).toArray } },
      times("Zip+MapJoin1 in Scala", runs) { finished { a.zip(a).map(m2join) } }
    )
    times("Zip+MapJoin1 in Scala+views", runs) { finished { a.view.zip(a).map(m2join).toArray } }
    
    same(
      times("ZipWithIndex in OpenCL", runs) { finished { cla.zipWithIndex.toArray } },
      times("ZipWithIndex in Scala", runs) { finished { a.zipWithIndex } }
    )
    
    same(
      times("ZipWithIndex+MapJoin1 in OpenCL", runs) { finished { cla.zipWithIndex.map(m2join).toArray } },
      times("ZipWithIndex+MapJoin1 in Scala", runs) { finished { a.zipWithIndex.map(m2join) } }
    )
    times("ZipWithIndex+MapJoin1 in Scala+views", runs) { finished { a.view.zipWithIndex.map(m2join).toArray } }
    
    /*val smmOpt = times("Map in Scala optimized", runs) { 
      val length = a.length
      val b = new Array[Int](length)
      var i = 0
      while (i < length) {
        b(i) = m(a(i))
        i += 1
      }
      b
    }*/
    same(
      times("Map in OpenCL", runs) { finished { cla.map(m).toArray } }, 
      times("Map in Scala", runs) { a.map(m) }
    )
    //assert(smm.toSeq == smmOpt.toSeq)
    
    same(
      times("Map2 in OpenCL", runs) { finished { cla.map(m2).toArray } }, 
      times("Map2 in Scala", runs) { a.map(m2) }
    )

    same(
      times("Map+Map2 in OpenCL", runs) { finished { cla.map(m).map(m2).toArray } }, 
      times("Map+Map2 in Scala", runs) { a.map(m).map(m2) }
    )
    times("Map+Map2 in Scala+views", runs) { a.view.map(m).map(m2).toArray }
    
    same(
      times("Map+Map2+MapJoin1 in OpenCL", runs) { finished { cla.map(m).map(m2).map(m2join).toArray } },
      times("Map+Map2+MapJoin1 in Scala", runs) { a.map(m).map(m2).map(m2join) }
    )
    times("Map+Map2+MapJoin1 in Scala+views", runs) { a.view.map(m).map(m2).map(m2join).toArray }
    
    same(
      times("Sum in OpenCL", runs) { finished { Array(cla.sum) } },
      times("Sum in Scala", runs) { Array(a.sum) }
    )
    
    same(
      times("Filtered Sum in OpenCL", runs) { finished { Array(cla.filter(f).sum) } },
      times("Filtered Sum in Scala", runs) { Array(a.filter(f).sum) }
    )
    
    same(
      times("Filter in OpenCL", runs) { finished { cla.filter(f).toArray } },
      times("Filter in Scala", runs) { a.filter(f) }
    )
    
    same(
      times("Filter+Map+Map2+MapJoin1 in OpenCL", runs) { finished { cla.filter(f).map(m).map(m2).map(m2join).toArray } },
      times("Filter+Map+Map2+MapJoin1 in Scala", runs) { a.filter(f).map(m).map(m2).map(m2join) }
    )
    times("Filter+Map+Map2+MapJoin1 in Scala+views", runs) { a.view.filter(f).map(m).map(m2).map(m2join).toArray }
    
    same(
      times("Filter+Map in OpenCL", runs) { finished { cla.filter(f).map(m).toArray } },
      times("Filter+Map  in Scala", runs) { a.filter(f).map(m) }
    )
    times("Filter+Map  in Scala+views", runs) { a.view.filter(f).map(m).toArray }
    
    //}
    //assert(ff.toArray.take(samples).toSeq == sff.take(samples).toArray.toSeq)

  }
}
