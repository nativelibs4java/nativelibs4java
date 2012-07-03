ScalaCL... v3 (yeah, yet another rewrite from scratch FTW!)

NOT FUNCTIONAL YET, WORK IN PROGRESS (see [ScalaCL](https://code.google.com/p/scalacl/) if you want something that _works_).

Features of the new design:
- Much better asynchronicity support (now requires OpenCL 1.1), and much better performance in general
- Support for captures of constants *and* OpenCL arrays
- Support for lazy clones for fast zipping
- Kernels are now fully specialized on static types and generated at compile-time (allows much faster startup and caching at runtime)
- ScalaCL Collections no longer fit in regular Scala Collections, to avoid silent data transfers / conversions when using unaccelerated methods (syntax stays the same, though)
- No more CLRange: expecting compiler to do its job

TODO:
- Implement missing DataIO[T], including ASM-optimized case classes
- Catch up with compiler plugin:
  - Auto-vectorize for loops
  - Provide static types to OpenCL conversion
  - Create top-level objects for kernels code
- Plug some v2 code back (filtered array compaction, reduceSymmetric, parallel sums...)
- Benchmarks!

Example that will eventually work:

    import scalacl._
    
    implicit val context = Context.best
    
    case class Matrix(data: CLArray[Float], rows: Int, columns: Int)(implicit context: Context) {
      def this(rows: Int, columns: Int) =
        this(new CLArray[Float](rows * columns), rows, columns)
      def this(n: Int) =
        this(n, n)
        
      def putProduct(a: Matrix, b: Matrix): Unit = {
        assert(a.columns == b.rows)
        assert(a.rows == rows)
        assert(b.columns == columns)
        
        kernel {
          // This block will either be converted to an OpenCL kernel or cause compilation error
		  for (i <- 0 until rows; j <- 0 until columns) {
		    data(i * columns + j) = (0 until a.columns).map(k => {
		      a.data(i * a.columns + k) * b.data(k * b.columns + j)
		    }).sum
		  }
	    }
      }
      
      def putSum(a: Matrix, b: Matrix): Unit = {
        assert(a.columns == b.columns && a.columns == columns)
        assert(a.rows == b.rows && a.rows == rows)
        
        kernel {
          for (i <- 0 until rows; j <- 0 until columns) {
          	val offset = i * columns + j
		    data(offset) = a.data(offset) + b.data(offset)
		  }
	    }
      }
    }
    
    val n = 10
    val a = new Matrix(n)
    val b = new Matrix(n)
    val out = new Matrix(n)
    
    out.putProduct(a, b)
    
    println(out.data)
