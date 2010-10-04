/** Run with :
 *
 *  ./test
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
        val name = if (args.isEmpty) "Normal" else args(0)
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
            // Run cold code
            val cold = time("Cold", count, 1000) { b }
            // Finish warmup : (1000 + 3000 > hotspot threshold both in server and client modes)
            time(null, count, 3000) { b }
            val warm = time("Test", count, 10000) { b }
            (cold, warm)
        }
        
        val (cold1, warm1) = tst(mno) { test1_mno } 
        val (cold2, warm2) = tst(mno) { test2_mn_o } 
        val (cold3, warm3) = tst(mno) { test3_mno }
        val (cold4, warm4) = tst(mno * m) { test4_mnom }
        
        println(Array(name, "Cold", 1, cold1).mkString("\t"));
        println(Array(name, "Cold", 2, cold2).mkString("\t"));
        println(Array(name, "Cold", 3, cold3).mkString("\t"));
        println(Array(name, "Cold", 4, cold4).mkString("\t"));
        println(Array(name, "Warm", 1, warm1).mkString("\t"));
        println(Array(name, "Warm", 2, warm2).mkString("\t"));
        println(Array(name, "Warm", 3, warm3).mkString("\t"));
        println(Array(name, "Warm", 4, warm4).mkString("\t"));
        //println((c + "\t" + t).replace('.', ','))
    }
}
