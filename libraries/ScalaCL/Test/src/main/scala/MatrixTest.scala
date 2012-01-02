object MatrixTest {
  type Matrix = Array[Array[Double]]
  /*@inline def Matrix(rows: Int, columns: Int)(f: (Int, Int) => Double): Matrix = Array.tabulate(rows, columns)(f)
  @inline def Diagonal(n: Int): Matrix = Matrix(n, n, (i, j) => if (i == j) 1 else 0)
  @inline def Zero(rows: Int, columns: Int): Matrix = Array.ofDim[Double](rows, columns)
  @inline def rowCount(m: Matrix) = m.length
  @inline def columnCount(m: Matrix) = m(0).length
  */
  //Matrix(rows: Int, columns: Int, f: (Int, Int) => Double): Matrix = Array.tabulate(rows, columns)(f)
  def Diagonal(n: Int): Matrix = Array.tabulate(n, n)((i, j) => if (i == j) 1 else 0)
  def Zero(rows: Int, columns: Int): Matrix = Array.ofDim[Double](rows, columns)
  @inline def rowCount(m: Matrix) = m.length
  @inline def columnCount(m: Matrix) = m(0).length
  
  def mmult(a: Matrix, b: Matrix): Matrix =
    Array.tabulate(rowCount(a), columnCount(b))((i, j) => (0 until columnCount(a)).map(k => a(i)(k) * b(k)(j)).sum)
  
  def madd(a: Matrix, b: Matrix): Matrix =
    Array.tabulate(rowCount(a), columnCount(a))((i, j) => a(i)(j) + b(i)(j))
    
  def main(args: Array[String]): Unit = {
    val start = System.nanoTime
    val n = if (args.length == 0)
        1000
    else
        args(0).toInt
    
        /*
    val a = Array.tabulate[Double](n, n)(_ + _)
    val b = Array.tabulate[Double](n, n)(_ + _)
  
    val o = mmult(a, b)
    */
      
    val a = Array.tabulate(n, n)((i, j) => (i + j) * 1.0)
    val b = Array.tabulate(n, n)((i, j) => (i + j) * 1.0)
    val o = Array.ofDim[Double](n, n)
    for (i <- 0 until n; j <- 0 until n) {
        var tot = 0.0
        for (k <- 0 until n)
            tot += a(i)(k) * b(k)(j)
        
        o(i)(j) = tot
    }
      
      val timeNano = System.nanoTime - start
      val timeMillisStr = ((timeNano / 10000) / 100.0).toString
      val random = new java.util.Random(start)
      System.err.println("Total time (milliseconds) :")
      System.err.println(timeMillisStr)
      def rand = (random.nextDouble * o.length).toInt
      System.err.println("Random value = " + o(rand)(rand))
      System.err.println("Last value = " + o(o.length - 1)(o.length - 1))
      System.out.println(timeMillisStr)
  }
}
