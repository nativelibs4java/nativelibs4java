package scalacl;

import java.nio._

import scala.List
import scala.Nil
import scalacl._


trait ScalaCL {

abstract case class PrimScal(v: Number, t: PrimType) extends TypedExpr(TypeDesc(1, Scalar, t)) with CLValue {
  override def toString = v.toString
}
case class Int1(value: Int) extends PrimScal(value, IntType) 
case class Double1(value: Double) extends PrimScal(value, DoubleType) 

case class Int2(x: Int, y: Int) extends TypedExpr(TypeDesc(1, Scalar, IntType)) with CLValue
case class Int4(x: Int, y: Int, z: Int, w: Int) extends TypedExpr(TypeDesc(4, Scalar, IntType)) with CLValue {
	def this(xy: Int2, zw: Int2) = {
		this(xy.x, xy.y, zw.x, zw.y)
	}
}

implicit def Int2Int1(v: Int) = Int1(v)
implicit def Int12Int(v: Int1) = v.value
implicit def Double2Double1(v: Double) = Double1(v)
implicit def Double12Double(v: Double1) = v.value

case class IntVar extends Var[Int1](TypeDesc(1, Scalar, IntType)) {
	override var value = Int1(0)
}
case class IntsVar(size: Int) extends Var[IntBuffer](TypeDesc(1, Parallel, IntType)) {
	override var value = ByteBuffer.allocateDirect(size * 4).asIntBuffer()
}
case class DoubleVar extends Var[Double1](TypeDesc(1, Scalar, DoubleType)) {
	override var value = Double1(0)
}
case class DoublesVar(size: Int) extends Var[DoubleBuffer](TypeDesc(1, Parallel, DoubleType)) {
	override var value = ByteBuffer.allocateDirect(size * 8).asDoubleBuffer()
}
abstract class Program {
	var root: Expr

	private var source: String = "";
 
	def !() {
		if (source == "") {
			val variables = root variables;
   
			(variables zipWithIndex) foreach { case (v, i) => 
			  v.argumentIndex = i
			  v.name = "var" + i 
			  if (v.typeDesc.valueType == Parallel)
				  v.parallelIndexName = "gid"
			}
   
			root accept { x => x match {
			  case v: Var[_] => v.isRead = true
//			  case BinOp(""".*~""", v: Var[_], _) => v.isWritten = true
			  case BinOp("=", v: Var[_], _) => v.isWritten = true
			  case _ => 
			} }
   
			variables foreach { v => 
			  if (v isAggregated)
				  throw new UnsupportedOperationException("Reductions not implemented yet !")
			}
   
			
	//		for (i <- 0 to variables.length)
	//			variables(i).argumentIndex = i;
	
	        var doc = new StringBuilder;
         
         
	        def implode(elts: List[String], sep: String) = {
			  if (elts == Nil) "" 
			  else elts reduceLeft { _ + sep + _ } //map { _.toString } 
			  	
			}
   

	        doc ++ implode(root.includes.map { "#include <" + _ + ">" }, "\n")
         	doc ++ "\n"
         	var argDefs = variables.map { v => 
         	  "__global " + 
              (if (v.isWritten) "" else "const ") + 
              v.typeDesc.globalCType + " " + v.name 
         	}
	        doc ++ ("void function(" + 
	          implode(argDefs, ", ") + ")\n");
	        doc ++ "{\n\t"
	        doc ++ "int gid = get_global_id(0);\n\t"
	        doc ++ root.toString
	        doc ++ "\n}\n"
	        
	        source = doc.toString
         }
		println(source)
		// bind In variables
		// exec
		// read Out variables
	}
}

}