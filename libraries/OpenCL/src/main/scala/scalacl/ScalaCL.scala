/*
 * ScalaCL2.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package scalacl
import com.nativelibs4java.opencl.OpenCL4Java._
import com.nativelibs4java.opencl._

//case class Dim(size: Int) extends Int1(size)


object ScalaCL extends Functions {
  //implicit def Dim2Int(v: Dim) = v.size

  implicit def Int2Int1(v: Int) = Int1(v)
  implicit def Int12Int(v: Int1) = v.value
  implicit def Double2Double1(v: Double) = Double1(v)
  implicit def Double12Double(v: Double1) = v.value

  implicit def quad2expr(v: (Expr, Expr, Expr, Expr)) = new Quad(v._1, v._2, v._3, v._4)
  implicit def duo2expr(v: (Expr, Expr)) = new Duo(v._1, v._2)
  implicit def trio2expr(v: (Expr, Expr, Expr)) = new Trio(v._1, v._2, v._3)

  def local[V <: AbstractVar](v: V): V = { v.scope = LocalScope; v }
  def global[V <: AbstractVar](v: V): V = { v.scope = GlobalScope; v }
  def priv[V <: AbstractVar](v: V): V = { v.scope = PrivateScope; v }

  implicit def Expr2Stat(expr: Expr) = ExprStat(expr)

  implicit def Stats2Statements(stats: Seq[Stat]) = Statements(stats)
  
  def BytesVar = new BytesVar(None)
  def ShortsVar = new ShortsVar(None)
  def IntsVar = new IntsVar(None)
  def LongsVar = new LongsVar(None)
  def FloatsVar = new FloatsVar(None)
  def DoublesVar = new DoublesVar(None)

  def BytesVar(sizeExpr: Expr) = new BytesVar(Some(sizeExpr))
  def ShortsVar(sizeExpr: Expr) = new ShortsVar(Some(sizeExpr))
  def IntsVar(sizeExpr: Expr) = new IntsVar(Some(sizeExpr))
  def LongsVar(sizeExpr: Expr) = new LongsVar(Some(sizeExpr))
  def FloatsVar(sizeExpr: Expr) = new FloatsVar(Some(sizeExpr))
  def DoublesVar(sizeExpr: Expr) = new DoublesVar(Some(sizeExpr))

  def ByteVar = new ByteVar
  def ShortVar = new ShortVar
  def IntVar = new IntVar
  def LongVar = new LongVar
  def FloatVar = new FloatVar
  def DoubleVar = new DoubleVar

}