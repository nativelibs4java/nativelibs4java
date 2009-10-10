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
  class Greyer(x: Dim, y: Dim) extends Program(x, y) {
	  val im = new ImageVar[Int4](classOf[Int4], x, y, 0)
	  val result = new ImageVar[Int4](classOf[Int4], x, y, 0)
	  val v = IntVar local;
	  content = List(
		v := (im.x + im.y + im.z) / 3,
		result := (v, v, v, im.w)
	  )
  }

    class SimpleAvg(i: Dim) extends Program(i) {
       val input = IntsVar(i)
       var output = IntsVar(i)
       content = output(i) :=
            (
                input(If(i == 0, i, i - 1)) +
                input(i) +
                input(If(i == (i.size - 1), i, i + 1))
            ) / 3
    }

  override def main(args: Array[String]) = {
    var prog1 = new VectAdd(Dim(1000))
    //prog1.alloc
	prog1.a.write(1 to 10) // slow + unoptimized
    prog1.b.write(1000 to 1011) // slow + unoptimized
	
	var list = List(1f, 2f, 3f, 4f);
    prog1.a.write(list)
    prog1.b.write(list)
    //prog1.a.write(ints)
    //prog1.b.write(ints)
    //prog.a.value.set(1, 0)
    prog1 !;
    for (i <- 0 to 10)
      println(prog1.result.get(i))

    //var inc = 1 to 10
    //println(inc.getClass.getName)
    
    var prog2 = new VectSinCos(Dim(10000))
    //prog2.alloc
    prog2.x.write(List(0.0f, 0.1f, 0.2f))
    prog2 !

    var av = new SimpleAvg(Dim(10));
    av.input.write(Array(10, 2, 1, 5, 6, 33, 7, 9, 20));
    av !

    for (i <- 0 until 10)
      println(av.output.get(i))


    var prog3 = new VectSinCos_better(Dim(10000))
    //prog2.alloc
    prog3.x.write(List(0.0f, 0.1f, 0.2f))
    prog3 !

	new Greyer(Dim(100), Dim(100)) !
  }
}



