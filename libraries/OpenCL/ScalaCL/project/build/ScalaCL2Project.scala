import sbt._

class ScalaCL2Project(info: ProjectInfo) extends DefaultProject(info)
{
  val nativelibs4javaRepo = "NativeLibs4Java Repository" at "http://nativelibs4java.sourceforge.net/maven/"
  val JavaCLBridJ = "com.nativelibs4java" % "javacl-bridj" % "1.0-SNAPSHOT"
  val junitInterface = "com.novocode" % "junit-interface" % "0.5" % "test->default"
}
