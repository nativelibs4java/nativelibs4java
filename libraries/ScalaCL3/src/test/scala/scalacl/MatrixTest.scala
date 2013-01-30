package scalacl
import org.junit._
import Assert._

class MatrixTest {
  
  case class Matrix(data: CLArray[Float], rows: Int, columns: Int)(implicit context: Context) {
    def this(rows: Int, columns: Int)(implicit context: Context) =
      this(new CLArray[Float](rows * columns), rows, columns)
    def this(n: Int)(implicit context: Context) =
      this(n, n)
  }
  
  def kernel_soon(v: => Unit) = sys.error("TODO")
  
  def mult(a: Matrix, b: Matrix, out: Matrix)(implicit context: Context) = 
  {
    assert(a.columns == b.rows)
    assert(a.rows == out.rows)
    assert(b.columns == out.columns)
    
    kernel_soon {
      // This block will either be converted to an OpenCL kernel or cause compilation error
      // It captures out.data, a.data and b.data
      for (i <- 0 until a.rows; j <- 0 until b.columns) {
        // TODO chain map and sum (to avoid creating a builder here !)
        //out.data(i * out.columns + j) = (0 until a.columns).map(k => {
        //  a.data(i * a.columns + k) * b.data(k * b.columns + j)
        //}).sum
        var sum = 0f
        for (k <- 0 until a.columns) {
          sum += a.data(i * a.columns + k) * b.data(k * b.columns + j)
        }
        out.data(i * out.columns + j) = sum
      }
    }
  }
  
  @Test
  def testMatrix {
    implicit val context = Context.best
    
    val n = 10
    val a = new Matrix(n)
    val b = new Matrix(n)
    val out = new Matrix(n)
    
    mult(a, b, out)
    
    println(out.data)
  }
}