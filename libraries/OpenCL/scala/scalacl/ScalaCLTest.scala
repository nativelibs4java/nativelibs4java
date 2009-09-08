package scalacl;
import Expr._
object ScalaCLTest extends Application with ScalaCL with NIOUtils {
  class MyProg(n: Int) extends Program {
		val a = IntsVar(n)
		val b = IntsVar(n)
		val d = IntVar()
		var o = DoublesVar(n)
		
		override var root: Expr = 
			o ~ a + sin(b) * 0.7 + d;
	}
  var prog = new MyProg(null, 10000)
  //prog.a.value.set(1, 0)
  prog !
}