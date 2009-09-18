package scalacl;
import com.nativelibs4java.opencl.OpenCL4Java._
import java.nio._
import scala.reflect.Manifest
import SyntaxUtils._
import ScalaCL._

object ScalaCLTestRun extends Application {
    
  class Prog1(i: Dim) extends Program(Context.BEST, i)
  {
    val a = new DoublesVar
    val b = new DoublesVar
    val o = new DoublesVar

    override var content: Stat = o ~ (a * sin(b) + 1)
  }
  class Prog1bis(i: Dim) extends Program(Context.BEST, i)
  {
    val a = new DoublesVar
    val b = new DoublesVar
    val o = new DoublesVar

    override var content: Stat = o(i) ~ (a(i) * sin(b(i)) + 1)
  }
  class Prog2(i: Dim) extends Program(Context.BEST, i)
  {
    val a = new DoublesVar(2 * i.size)
    val b = new DoublesVar
    val o = new DoublesVar

    //override var statements: Stat = o(i) ~ ((a(2 * i) + a(2 * i + 1)) / 2 * sin(b(i)) + 1)
    override var content: Stat =
      o ~ (
        (
          a(2 * i) + a(2 * i + 1)
        ) / 2 * sin(b)
        +
        1
      )
  }

  override def main(args: Array[String]) = {
    var prog1 = new Prog1(Dim(10000))
    //prog.a.value.set(1, 0)
    prog1 !

    var prog1bis = new Prog1bis(Dim(10000))
    //prog.a.value.set(1, 0)
    prog1bis !

    var prog2 = new Prog2(Dim(10000))
    //prog.a.value.set(1, 0)
    prog2 !
  }
}



