import sbt._
import Keys._

object ScalaCLBuild extends Build {
	val sharedSettings = Defaults.defaultSettings ++ Seq(
		organization := "com.nativelibs4java",
    version := "0.3-SNAPSHOT",
    
    fork := true,
    
    scalaVersion := "2.10.0",
    scalacOptions ++= Seq(
      "-language:experimental.macros",
      "-deprecation"
      //"-Ymacro-debug-lite"
      //"-Xlog-free-terms", 
      //"-unchecked",
    ),
    
		resolvers += "Sonatype OSS Repository" at "http://oss.sonatype.org/content/repositories/snapshots",
		
		libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-compiler" % _),
    libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _),
      
		libraryDependencies ++= Seq(
      //"com.nativelibs4java" % "javacl" % "1.0-SNAPSHOT",
      "com.novocode" % "junit-interface" % "0.5" % "test->default"
    ),    

    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _)
	)

	lazy val root = Project(
		id = "ScalaCL",
		base = file("."),
		settings = sharedSettings ++ Seq(
		  name := "scalacl"
		)
	)
}
