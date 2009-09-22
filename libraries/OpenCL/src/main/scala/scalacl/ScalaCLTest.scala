package scalacl;
//import com.nativelibs4java.opencl.OpenCL4Java._
import java.nio._
import scala.reflect.Manifest
import SyntaxUtils._
import ScalaCL._

object ScalaCLTestRun extends Application {
    
  class VectAdd(i: Dim) extends Program(i) {
    val a = FloatsVar
    val b = FloatsVar
    val result = FloatsVar
    content = result := a + b
  }
  class VectSinCos(i: Dim) extends Program(i) {
    val x = FloatsVar
    val scx = FloatsVar
    content = List(
      scx(i * 2) := sin(x),
      scx(i * 2 + 1) := cos(x)
    )
  }
  class VectSinCos_better(i: Dim) extends Program(i) {
    val x = FloatsVar
    val scx = FloatsVar
    content = scx(i * 2) := sincos(x, scx(i * 2 + 1))
  }
  class VectATan(i: Dim) extends Program(i) {
    val x = FloatsVar
    val y = FloatsVar
    val result = FloatsVar
    content = result := atan2(x, y)
  }
  override def main(args: Array[String]) = {
    var prog1 = new VectAdd(Dim(1000))
    //prog1.alloc
    prog1.a.write(List(1, 2, 3, 4))
    prog1.b.write(List(1, 2, 3, 4))
    //prog1.a.write(ints)
    //prog1.b.write(ints)
    //prog.a.value.set(1, 0)
    prog1 !;
    for (i <- 0 to 10)
      println(prog1.result.get(i))

    var inc = 1 to 10
    println(inc.getClass.getName)
    
    var prog2 = new VectSinCos(Dim(10000))
    //prog2.alloc
    prog2.x.write(List(0.0f, 0.1f, 0.2f))
    prog2 !


    var prog3 = new VectSinCos_better(Dim(10000))
    //prog2.alloc
    prog3.x.write(List(0.0f, 0.1f, 0.2f))
    prog3 !
  }
}



