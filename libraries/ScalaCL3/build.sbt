name := "scalacl"

version := "0.3-SNAPSHOT"

organization := "com.nativelibs4java"

//scalaHome := Some(file("/Users/ochafik/bin/scala-2.10.0.r25286-b20110715023800"))

scalaVersion := "2.10.0-M4"

resolvers += "Sonatype OSS Repository" at "http://oss.sonatype.org/content/groups/public/"

libraryDependencies ++= Seq(
  	//"com.nativelibs4java" % "javacl" % "1.0-SNAPSHOT",
	"com.novocode" % "junit-interface" % "0.5" % "test->default"
)

