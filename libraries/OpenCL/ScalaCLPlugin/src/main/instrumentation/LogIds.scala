package scalacl.instrumentation

import java.io._

case class LogId(qualifiedName: String, source: String, line: Int, offset: Int)

class LogIds {
  import scala.collection.mutable.HashMap
  private var nextId = 1L
  private val ids = new HashMap[LogId, Long]
  def apply(id: LogId) = ids.getOrElseUpdate(id, this synchronized {
    val id = nextId
    nextId += 1
    id
  })
  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run {
      var fileProp = System.getProperty("scalacl.log.ids.file", System.getenv("SCALACL_LOG_IDS_FILE"))
      if (fileProp == null)
        fileProp = "log.ids.properties"
      
      val file = new File(fileProp).getAbsoluteFile
      file.getParentFile.mkdirs
      val out = new PrintStream(file)
      def line(values: Any*) = out.println(values.mkString("\t"))
      line("Id", "Qualified Name", "Source File", "Line", "Offset")
      try {
          for ((logId, id) <- ids)
            line(id, logId.qualifiedName, logId.source, logId.line, logId.offset)
      } finally {
          out.close
      }
      println("Finished writing log ids")
    }
  })
}
