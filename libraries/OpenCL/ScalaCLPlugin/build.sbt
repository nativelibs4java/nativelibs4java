name := "scalacl-compiler-plugin"

version := "0.3-SNAPSHOT"

organization := "com.nativelibs4java"

scalaHome := Some(file("/Users/ochafik/bin/scala-2.10.0.r25286-b20110715023800"))

//scalaVersion := "2.9.0"

resolvers += "NativeLibs4Java Repository" at "http://nativelibs4java.sourceforge.net/maven/"

libraryDependencies ++= Seq(
	//"org.scala-lang" % "scala-compiler" % "2.10.0-SNAPSHOT",
	//"org.scala-lang" % "scala-library" % "2.10.0-SNAPSHOT",
  	"com.nativelibs4java" % "javacl" % "1.0.0-RC1",
	"com.nativelibs4java" % "scalacl" % "0.3-SNAPSHOT",
	"com.novocode" % "junit-interface" % "0.5" % "test->default"
)

//watchSources += Path.fromFile("examples")