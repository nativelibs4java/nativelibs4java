name := "scalaxy"

mainClass := Some("com.nativelibs4java.scalaxy.plugin.Compile")

version := "0.3-SNAPSHOT"

organization := "com.nativelibs4java"

//scalaHome := Some(file("/Users/ochafik/bin/scala-2.9.0.final"))

scalaVersion := "2.9.2"

resolvers += "Sonatype OSS Snapshots Repository" at "http://oss.sonatype.org/content/groups/public/"

libraryDependencies ++= Seq(
	"org.scala-lang" % "scala-compiler" % "2.9.1",
	"org.scala-lang" % "scala-library" % "2.9.1",
	"com.novocode" % "junit-interface" % "0.5" % "test->default"
)
