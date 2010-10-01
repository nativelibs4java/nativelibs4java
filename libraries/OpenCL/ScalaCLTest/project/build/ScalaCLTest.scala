import sbt._

class ScalaCLTest(info: ProjectInfo) extends DefaultProject(info) with AutoCompilerPlugins
{
  val nativelibs4javaRepo = "NativeLibs4Java Repository" at "http://nativelibs4java.sourceforge.net/maven/"
  val scalacl = compilerPlugin("com.nativelibs4java" % "scalacl-compiler" % "1.0-SNAPSHOT")
  //override def compileOptions = super.compileOptions ++ compileOptions("-P:scalacl")
  override def compileOptions = super.compileOptions ++ compileOptions("-Xplugin:lib_managed\\scala_2.8.0\\plugin\\scalacl-compiler-1.0-SNAPSHOT.jar", "-Xplugin-require:scalacl")
  //-P:scalacl")
}
