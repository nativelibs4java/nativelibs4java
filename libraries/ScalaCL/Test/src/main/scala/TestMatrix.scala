/** Run with :
 *
 *  ./test
 *  mvn scala:compile && scala -cp target/classes Test
 *  mvn scala:compile && scala -cp target\classes Test
 *  javap -c -classpath target\classes Test$
 */
object TestMatrix {
    import TestUtils._
    
    def main(args: Array[String]): Unit = {
        val name = if (args.isEmpty) "Normal" else args(0)
        val m = 10
        val n = 10
        val o = 30
        val mn = m * n 
        val mno = m * n * o
        
        
        def multMatrix(
            a: Array[Array[Double]], aRows: Int, aColumns: Int, 
            b: Array[Array[Double]], bRows: Int, bColumns: Int,
            out: Array[Array[Double]]) = {
                
            for (i <- 0 until aRows) {
                val outi = out(i)
                for (j <- 0 until bColumns) {
                    var tot = 0.0
                    val ai = a(i)
                    for (k <- 0 until aColumns)
                        tot += ai(k) * b(k)(j)
                    outi(j) = tot
                }
            }
        }
        
        val a = new Array[Array[Double]](m).map(_ => new Array[Double](n))
        val b = new Array[Array[Double]](n).map(_ => new Array[Double](o))
        val out = new Array[Array[Double]](m).map(_ => new Array[Double](o))
        def testMat = {
            multMatrix(a, m, n, b, n, o, out)   
        }
        val (coldMat, warmMat) = tst(mno) { testMat }
        println(Array(name, "Cold", "mat", coldMat).mkString("\t"));
        println(Array(name, "Warm", "mat", warmMat).mkString("\t"));
        
           
    }
}
