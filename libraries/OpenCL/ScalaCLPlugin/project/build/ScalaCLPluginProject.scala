import sbt._
import reaktor.scct.ScctProject

class ScalaCLPluginProject(info: ProjectInfo) extends DefaultProject(info)
with ScctProject
with IdeaProject
{ 
  //override def name = "scalacl-compiler-plugin"
  override def watchPaths = super.watchPaths +++ Path.fromFile("examples")
  
  override def filterScalaJars = false
  override def compileOptions = super.compileOptions ++
     compileOptions("-optimise")
     
  val nativelibs4javaRepo = "NativeLibs4Java Repository" at "http://nativelibs4java.sourceforge.net/maven/"
  //val scalacl = compilerPlugin("com.nativelibs4java" % "scalacl-compiler-plugin" % "1.0-SNAPSHOT")
  
  //val BuSwing = "com.nativelibs4java" % "buswing_2.8.0" % "0.1.1-SNAPSHOT"
  val junitInterface = "com.novocode" % "junit-interface" % "0.5" % "test->default"
  val scalaCompiler = "org.scala-lang" % "scala-compiler" % buildScalaVersion
  val scalaLibrary = "org.scala-lang" % "scala-library" % buildScalaVersion
}
