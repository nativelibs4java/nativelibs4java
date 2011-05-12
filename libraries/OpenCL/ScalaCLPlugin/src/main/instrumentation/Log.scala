package scalacl.instrumentation

import java.io._
import scala.collection.mutable.ArrayBuffer
class Log {
  private val arrays = new ArrayBuffer[Array[Long]]
  private var currentArray: Array[Long] = _
  private val arraySize = 1024
  private var currentSize = 0
  
  @inline private def nextArray = {
      val a = new Array[Long](arraySize << 1)
      currentArray = a
      arrays += a
      a
  }
  nextArray
  
  def enter(id: Long) = log(id, true)
  def exit(id: Long) = log(id, false)
  
  @inline private def log(id: Long, isEnter: Boolean): Unit = {
      if (currentSize >= arraySize) {
          nextArray 
      }
      val a = currentArray
      val offset = currentSize << 1
      a(offset) = id
      val t = System.nanoTime
      a(offset + 1) = (if (isEnter) -t else t)
      currentSize += 1
  }
  
  def dump(dir: File) {
    val f = new File(dir, "thread-" + Thread.currentThread.getId)
    println("Writing '" + f + "'...")
    val out = new PrintStream(f)
    def line(values: Any*) = out.println(values.mkString("\t"))
    line("Id", "Nano Time")
    try {
        arrays.foreach(a => {
            for (i <- 0 until (if (a == currentArray) currentSize else arraySize)) {
                val offset = i << 1
                line(a(offset), a(offset + 1))
            }
        })
    } finally {
        out.close
    }
  }
}
object Log {
  private val logs = new ArrayBuffer[Log]
  private val local = new ThreadLocal[Log] {
      override def initialValue = {
          val log = new Log
          logs synchronized {
              logs += log
          }
          log
      }
  }
  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run {
      var dirProp = System.getProperty("scalacl.log.dir", System.getenv("SCALACL_LOG_DIR"))
      if (dirProp == null)
        dirProp = "logs"
      
      val dir = new File(dirProp)
      dir.mkdirs
      logs.foreach(_.dump(dir))
      println("Finished writing log dumps")
    }
  })
  def apply(): Log = local.get
}
