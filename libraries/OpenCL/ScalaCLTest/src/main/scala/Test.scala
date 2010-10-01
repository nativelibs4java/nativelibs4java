/** Run with :
 *
 *  test Normal
 *  test Optimized
 *
 *  mvn clean scala:compile && scala -cp target\classes Test
 *  javap -c -classpath target\classes Test$
 */
object Test {
    def time[V](title: String, n: Long, loops: Int)(b: => V): Double = {
        val startTime = System.nanoTime
        var i = 0
        while (i < loops) {
            b
            i += 1
        }
        val time = System.nanoTime - startTime
        val timePerLoop = time / loops
        val timePerItem = timePerLoop / n.toDouble
        //System.err.println("Time[" + title + "] = " + (time / 1000000L) + " milliseconds (" + (timePerLoop / 1000L) + " microseconds per iteration, " + timePerItem + " nanoseconds per item)")
        if (title != null)
            System.err.println("Time[" + title + "] = " + timePerItem + " nanoseconds per item)")
        
        timePerItem
    }
    def main(args: Array[String]): Unit = {
        val name = args(0)
        val m = 10
        val n = 10
        val o = 30
        val mn = m * n 
        val mno = m * n * o
        def test1_mno = {
            var t = 0.0
            for (i <- 0 until mno)
                    t += i / 10
            t
        }
        def test2_mn_o = {
            var t = 0.0
            for (i <- 0 until mn)
                for (j <- 0 until o)
                    t += (i + j) / 10
            t
        }
        def test3_mno = {
            var t = 0.0
            for (i <- 0 until n)
                for (j <- 0 until m)
                    for (k <- 0 until o)
                        t += (i + j + k) / 10
            t
        }
        def test4_mnom = {
            var t = 0.0
            for (i <- 0 until n)
                for (j <- 0 until m)
                    for (k <- 0 until o)
                        for (l <- 0 until m)
                            t += (i + j + k + l) / 10
            t
        }
        
        def tst(count: Long)(b: => Unit) = {
            val c = time("Cold", count, 1000) { b }
            // Finish warmup :
            time(null, count, 2000) { b }
            val t = time("Test", count, 10000) { b }
            
            (c, t)
        }
        
        val (c1, t1) = tst(mno) { test1_mno } 
        val (c2, t2) = tst(mno) { test2_mn_o } 
        val (c3, t3) = tst(mno) { test3_mno }
        val (c4, t4) = tst(mno * m) { test4_mnom }
        
        println(Array(name, "Cold", 1, c1).mkString("\t"));
        println(Array(name, "Cold", 2, c2).mkString("\t"));
        println(Array(name, "Cold", 3, c3).mkString("\t"));
        println(Array(name, "Cold", 4, c4).mkString("\t"));
        println(Array(name, "Warm", 1, t1).mkString("\t"));
        println(Array(name, "Warm", 2, t2).mkString("\t"));
        println(Array(name, "Warm", 3, t3).mkString("\t"));
        println(Array(name, "Warm", 4, t4).mkString("\t"));
        //println((c + "\t" + t).replace('.', ','))
    }
}
