/// USE -optimise when compiling the code : the difference between List.foreach and while is then much bigger !
/// scalac -optimise TestListForeach.scala && scala TestListForeach
object TestListForeach {
    /*
        import TestUtils._
        val arrLen = 100
        val a = Array.tabulate(arrLen) { i => i.toString }
        val list = a.toList
        def testListForeach = {
            var tot = 0
            for (v <- list)
                tot += v.length
            tot
        }
        def testListWhile = {
            var tot = 0
            var l = list
            while (!l.isEmpty) {
                val v = l.head
                tot += v.length
                l = l.tail
            }
            tot
        }
        val (coldForeach, warmForeach) = tst(arrLen) { testListForeach }
        val (coldWhile, warmWhile) = tst(arrLen) { testListWhile }
        println(Array("Foreach", "Cold", coldForeach).mkString("\t"));
        println(Array("Foreach", "Warm", warmForeach).mkString("\t"));
        println(Array("While", "Cold", coldWhile).mkString("\t"));
        println(Array("While", "Warm", warmWhile).mkString("\t"));
        
    */
    def time(loops: Int, n: Int)(b: => Unit): Double = {
        val start = System.nanoTime
        for (i <- 0 until loops)
            b
        val time = System.nanoTime - start
        val nsPerItem = time / (loops * (n: Double))
        nsPerItem
    }
    def main(args: Array[String]): Unit = {
        for (n <- Array(100000, 10000, 1000, 100, 10, 5, 4, 3, 2, 1).toList) {
            val list = (0 until n).toList
            
            def testForeach = {
                var tot = 0L
                for (v <- list)
                    tot += v
                tot
            }
            
            def testWhile = {
                var tot = 0L
                var l = list
                
                //while (l.isInstanceOf[::[_]]) {
                while (!l.isEmpty) {
                    val item = l.head
                    tot += item
                    l = l.tail
                }
                tot
            }
            def tst(loops: Int, n: Int, runs: Int) = {
              val res = for (i <- 0 until runs) yield {
                val fe = time(loops, n) { testForeach }
                val wh = time(loops, n) { testWhile }
                (wh, fe)
              }
              val whs = res.map(_._1)
              val fes = res.map(_._2)
              (whs.sum / whs.size, fes.sum / fes.size)
            }
            
            val nRuns = 20
            val coldLoops = 500
            assert(testForeach == testWhile)
            val (coldWhile, coldForeach) = tst(coldLoops, n, nRuns)
            
            val warmupLoops = 2500
            tst(warmupLoops, n, 1)
            
            val warmLoops = 500
            val (warmWhile, warmForeach) = tst(warmLoops, n, nRuns)
            
            println("Foreach (n = " + n + ")\t: \tCold = " + coldForeach + " ns \t\t\tWarm = " +warmForeach + " ns")
            println("  While (n = " + n + ")\t: \tCold = " + coldWhile + " ns \t\t\tWarm = " +warmWhile + " ns")
            def fmt(d: Double) = ((d * 100).toLong / 100.0).toString
            println("   Gain (n = " + n + ")\t: \t     x " + fmt(coldForeach / coldWhile) + "\t\t\t     x " + fmt(warmForeach / warmWhile))
            
            println()
        }
    }
}
