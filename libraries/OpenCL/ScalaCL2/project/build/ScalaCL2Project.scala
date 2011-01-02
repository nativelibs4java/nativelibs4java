import sbt._

class ScalaCL2Project(info: ProjectInfo) extends DefaultProject(info)
{
  import java.io.File
  def writeFile(text: String, file: File) = {
    val p = file.getParentFile
    if (p != null && !p.exists)
      p.mkdirs
    val out = new java.io.PrintWriter(file)
    out.println(text)
    out.close
  }
  def readFile(file: File) = {
    import java.io._
    val in = new java.io.BufferedReader(new java.io.FileReader(file))
    var line = ""
    val b = new StringBuilder
    while ({ line = in.readLine(); line != null }) {
      b.append(line).append('\n')
    }
    in.close
    b.toString
  }
  def copyFile(file: File, dir: File) =
    writeFile(readFile(file), new File(dir, file.getName))
        
  lazy val copyPersistence = task {
    val classes = new File("target/scala_2.8.0/classes")
    val resources = new File("src/main/resources")
    val mi = "META-INF"
    val file = new File(new File(resources, mi), "persistence.xml")
    val dest = new File(classes, mi)
    println("Copying " + file + " to " + dest)
    copyFile(file, dest)
    None 
  }
  
  val nativelibs4javaRepo = "NativeLibs4Java Repository" at "http://nativelibs4java.sourceforge.net/maven/"
  //val BuSwing = "com.nativelibs4java" % "buswing_2.8.0" % "0.1.1-SNAPSHOT"
  //val JavaCLBridJ = "com.nativelibs4java" % "javacl-bridj" % "1.0-SNAPSHOT"
  val junitInterface = "com.novocode" % "junit-interface" % "0.4" % "test"//"test->default" 
}
