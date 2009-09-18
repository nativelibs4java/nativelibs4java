package scalacl;
import com.nativelibs4java.opencl.OpenCL4Java._
import java.nio._
import scala.reflect.Manifest
import SyntaxUtils._

object ScalaCLTestRun extends Application {
    import ScalaCL._
    class MyProg(i: Dim) extends Program(Context.BEST, i) {
      
      val a = new ArrayVar[Double] alloc 2 * i.size
      val b = new ArrayVar[Double] alloc i.size
      val o = new ArrayVar[Double] alloc i.size
      
      //override var statements: Stat = o(i) ~ ((a(2 * i) + a(2 * i + 1)) / 2 * sin(b(i)) + 1)
      override var statements: Stat = o ~ ((a(2 * i) + a(2 * i + 1)) / 2 * sin(b) + 1)
      //(
                    //localTot ~ 0,
                    //For(j, 0, j.size,
                    //	localTot +~ img(i, j)
                    //)
                    //globalTot +~ localTot
            //)


      //val x = new Var[Int]
      //val img = new ImageVar
      //val globalTot = new Var[Double4]
      //val localTot = new Var[Double4]

    }
    
    override def main(args: Array[String]) = {
        var prog = new MyProg(Dim(10000))
	    //prog.a.value.set(1, 0)
	    prog !;
	    prog.source
    	
    }
}



