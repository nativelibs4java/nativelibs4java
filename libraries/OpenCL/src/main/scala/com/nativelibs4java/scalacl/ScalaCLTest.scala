package com.nativelibs4java.scalacl;
import java.nio._
import Functions._

object ScalaCLTest extends Application {
    import ScalaCL._
    class MyProg(i: Int1) extends Program(Context.BEST) {
      
      val a = new ArrayVar[Double]
      val b = new ArrayVar[Double]
      val o = new ArrayVar[Double]
      val globalTot = new Var[Double4]
      val localTot = new Var[Double4]
      val x = new Var[Int]
      val img = new ImageVar

      override var root: Expr = o(i) ~ a(i) * sin(b(i)) + 1
      //(
                    //localTot ~ 0,
                    //For(j, 0, j.size,
                    //	localTot +~ img(i, j)
                    //)
                    //globalTot +~ localTot
            //)
    }
    var context = Context.BEST;
    
    var prog = new MyProg(10000)
    //prog.a.value.set(1, 0)
    prog !;
    
}



