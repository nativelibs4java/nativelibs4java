package com.nativelibs4java.scalacl;
import Expr._
object ScalaCLTest extends Application with ScalaCL with NIOUtils {
    class MyProg(n: Int) extends Program {
        val a = DoublesVar(n)
        val b = DoublesVar(n)
        var o = DoublesVar(n)
		
        override var root: Expr = o ~ a * sin(b) + 1;
    }
    var prog = new MyProg(10000)
  
    //prog.a.value.set(1, 0)
    prog !
}