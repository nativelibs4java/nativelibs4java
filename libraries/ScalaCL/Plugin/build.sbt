name := "scalacl-compiler-plugin"

mainClass := Some("scalacl.plugin.Compile")

version := "0.3-SNAPSHOT"

//fork := true

organization := "com.nativelibs4java"

//scalaHome := Some(file("/Users/ochafik/bin/scala-2.10.0.r25286-b20110715023800"))
//scalaHome := Some(file("/Users/ochafik/bin/scala-2.9.0.final"))

scalaVersion := "2.9.1"

resolvers += "NativeLibs4Java Repository" at "http://nativelibs4java.sourceforge.net/maven/"

libraryDependencies ++= Seq(
	"org.scala-lang" % "scala-compiler" % "2.9.1",
	"org.scala-lang" % "scala-library" % "2.9.1",
  	//"com.nativelibs4java" % "javacl" % "1.0.0-RC1",
	"com.nativelibs4java" % "scalacl" % "0.3-SNAPSHOT" classifier "shaded",
	"com.novocode" % "junit-interface" % "0.5" % "test->default"
)
