import sbt._

class ScalaCLTest(info: ProjectInfo) extends DefaultProject(info) with AutoCompilerPlugins
{
  val nativelibs4javaRepo = "NativeLibs4Java Repository" at "http://nativelibs4java.sourceforge.net/maven/"
  val scalacl = compilerPlugin("com.nativelibs4java" % "scalacl-compiler-plugin" % "0.1") //1.0-SNAPSHOT")
}
