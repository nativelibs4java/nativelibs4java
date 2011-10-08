import java.io._
import collection.mutable.{ ArrayBuffer, HashMap }

def files(base: File, out: ArrayBuffer[File]): Unit = {
  if (base.isFile)
    out += base
  else if (base.isDirectory)
    base.listFiles.foreach(files(_, out))
}

def files(base: File = new File(".")): ArrayBuffer[File] = {
  val out = new ArrayBuffer[File]
  files(base, out)
  out
}

val f = files().map(f => (f, f.length)).sortBy(-_._2)
for ((f, l) <- f)
  println(l + "\t" + f)

