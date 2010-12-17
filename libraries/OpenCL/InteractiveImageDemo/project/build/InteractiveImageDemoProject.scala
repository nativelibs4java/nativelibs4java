import sbt._

class InteractiveImageDemoProject(info: ProjectInfo) extends DefaultProject(info) with AutoCompilerPlugins
{
  val nativelibs4javaRepo = "NativeLibs4Java Repository" at "http://nativelibs4java.sourceforge.net/maven/"
  val javacl = "com.nativelibs4java" % "javacl" % "1.0-SNAPSHOT" 
}
