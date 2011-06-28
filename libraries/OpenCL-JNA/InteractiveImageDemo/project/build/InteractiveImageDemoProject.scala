import sbt._

class InteractiveImageDemoProject(info: ProjectInfo) extends DefaultProject(info) with AutoCompilerPlugins
{
  val nativelibs4javaRepo = "NativeLibs4Java Repository" at "http://nativelibs4java.sourceforge.net/maven/"
  
  val jnaeratorVersion = "0.9.6-SNAPSHOT"
  val javaclVersion = "1.0-SNAPSHOT"
  
  val javacl = "com.nativelibs4java" % "javacl" % javaclVersion
  val ochafikSwing = "com.ochafik" % "ochafik-swing" % jnaeratorVersion
}
