/*
 * ScalaCL2.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package scalacl
import com.nativelibs4java.opencl.OpenCL4Java._

//case class Dim(size: Int) extends Int1(size)


object ScalaCL extends Functions {
  //implicit def Dim2Int(v: Dim) = v.size

  implicit def Int2Int1(v: Int) = Int1(v)
  implicit def Int12Int(v: Int1) = v.value
  implicit def Double2Double1(v: Double) = Double1(v)
  implicit def Double12Double(v: Double1) = v.value

  def local[V <: AbstractVar](v: V): V = { v.scope = LocalScope; v }
  def global[V <: AbstractVar](v: V): V = { v.scope = GlobalScope; v }
  def priv[V <: AbstractVar](v: V): V = { v.scope = PrivateScope; v }

  implicit def Expr2Stat(expr: Expr) = ExprStat(expr)

  implicit def Stats2Statements(stats: Seq[Stat]) = Statements(stats)
  
  def BytesVar = new BytesVar
  def ShortsVar = new ShortsVar
  def IntsVar = new IntsVar
  def LongsVar = new LongsVar
  def FloatsVar = new FloatsVar
  def DoublesVar = new DoublesVar

  def BytesVar(sizeExpr: Expr) = new BytesVar(sizeExpr)
  def ShortsVar(sizeExpr: Expr) = new ShortsVar(sizeExpr)
  def IntsVar(sizeExpr: Expr) = new IntsVar(sizeExpr)
  def LongsVar(sizeExpr: Expr) = new LongsVar(sizeExpr)
  def FloatsVar(sizeExpr: Expr) = new FloatsVar(sizeExpr)
  def DoublesVar(sizeExpr: Expr) = new DoublesVar(sizeExpr)

  def ByteVar = new ByteVar
  def ShortVar = new ShortVar
  def IntVar = new IntVar
  def LongVar = new LongVar
  def FloatVar = new FloatVar
  def DoubleVar = new DoubleVar

}