
object LinAlg
{
  type Matrix = 
    Array[Array[Double]]
    
  @inline 
  def columns(m: Matrix): Int = 
    m(0).length
    
  @inline 
  def rows(m: Matrix): Int =
    m.length
    
  @inline 
  def multiply(a: Matrix, b: Matrix): Matrix = {
    val aRows = rows(a)
    val aCols = columns(a)
    val bCols = columns(b)
    Array.tabulate[Double](aRows, bCols)((i, j) => {
      (0 until aCols).map(k => a(i)(k) * b(k)(j)).sum
    })
  }
    
  def describe(m: Matrix) =
    "[\n" + m.map("\t[ \t" + _.mkString(",\t") + "\t ]").mkString(",\n") + "\n]"
}

object TestLinAlg extends App 
{
  import LinAlg._
  
  val m = 3
  val n = 4
  val o = 5
  val a = Array.tabulate[Double](m, n)(_ + _) // a(i)(j) = i + j
  val b = Array.tabulate[Double](n, o)(_ - _) // b(i)(j) = i - j
  val ab = multiply(a, b)
  
  assert(rows(a) == rows(ab))
  assert(columns(b) == columns(ab))
    
  println(describe(ab))
}
