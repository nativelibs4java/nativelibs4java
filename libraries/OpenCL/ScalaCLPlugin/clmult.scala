import scalacl._

object CLLinAlg {
  //case class Matrix(data: CLArray[Double], rows: Int, columns: Int)
  
  type Matrix =
    CLArray[Double]
    
  def newMatrix(rows: Int, cols: Int)(implicit context: Context) =
    new Matrix(rows * cols)
    
  def fill(m: Matrix, rows: Int, cols: Int)(implicit context: Context) = {
    for (idx <- (0 until (rows * cols)).cl) {
      val i = idx / cols
      val j = idx - i * cols
      m(idx) = i - j
    }
  }
  
  def multiply(a: Matrix, aRows: Int, aCols: Int, b: Matrix, bRows: Int, bCols: Int)(implicit context: Context): Matrix = {
    assert(aCols == bRows)
    
    val outRows = aRows
    val outCols = bCols
    val out = newMatrix(outRows, outCols)
    for (idx <- (0 until (outRows * outCols)).cl) {
      val i = idx / outCols
      val j = idx - i * outCols
      
      out(idx) = 
        (0 until aCols).map(
          k => a(i * aCols + k) * b(k * bCols + j)
        ).sum
    }
    out
  }
}
object CLLinAlgTest extends App {
  import CLLinAlg._
  
  implicit val context = Context.best
  
  val m = 3
  val n = 4
  val o = 5
  
  val a = newMatrix(m, n)
  fill(a, m, n)
  val b = newMatrix(n, o)
  fill(b, n, o)
  
  val out = multiply(a, m, n, b, n, o)
    
  println("a = " + a.toSeq)
  println("b = " + b.toSeq)
  println("out = " + out.toSeq)
}
