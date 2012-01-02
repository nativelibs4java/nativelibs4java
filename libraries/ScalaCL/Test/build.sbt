//name := "scalacl-test"

//version := "0.3-SNAPSHOT"

//organization := "com.nativelibs4java"

//scalaVersion := "2.9.0"

resolvers += "NativeLibs4Java Repository" at "http://nativelibs4java.sourceforge.net/maven/"

libraryDependencies += "com.nativelibs4java" % "scalacl" % "0.2"

autoCompilerPlugins := true

addCompilerPlugin("com.nativelibs4java" % "scalacl" % "0.2")

