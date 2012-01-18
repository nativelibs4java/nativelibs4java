package scalacltests

import scalacl._
import org.junit.{Assert, Test, Ignore}

/**
SCALACL_VERBOSE=1 SCALACL_TRACE=1 mvn scala:run -DmainClass=scalacl.plugin.Compile "-DaddArgs=Test209.scala|-Xprint:scalacl-stream|-cp|../ScalaCL/target/scalacl-0.3-SNAPSHOT-shaded.jar:/Users/ochafik/.m2/repository/junit/junit/4.10/junit-4.10.jar"

SCALACL_VERBOSE=1 SCALACL_TRACE=1 scalac -Xplugin:target/scalacl-compiler-plugin-0.3-SNAPSHOT-shaded.jar -cp ../ScalaCL/target/scalacl-0.3-SNAPSHOT-shaded.jar:/Users/ochafik/.m2/repository/junit/junit/4.10/junit-4.10.jar Test209.scala

scala -cp .:../ScalaCL/target/scalacl-0.3-SNAPSHOT-shaded.jar:/Users/ochafik/.m2/repository/junit/junit/4.10/junit-4.10.jar scalacltests.Test209
*/
class Issue209 {
  private implicit val context = Context.best
  println(context)

  def $mean(av : Array[CLArray[Float]]) : Array[Float] = { av.map( v => { v.sum / v.length } ) }
  def mean(av : Array[Array[Float]]) : Array[Float]  = { $mean(av.map(v => { v.cl })) }  //-- line 11

  @Test
  def testMean() : Unit = {
    val a = Array[Float](1.2F, 1.5F, 1.6F, 1.8F)
    val b = Array[Float](2.2F, 2.5F, 2.6F, 2.8F)
    val m = Array(a,b)
    mean(m).foreach(f => println(f))  //-- line 18
  }
  
  def main(args: Array[String]): Unit = {
    testMean()
  }
}
