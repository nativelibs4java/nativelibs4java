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
        for (n <- Array(10000, 1000, 100, 10, 5, 4, 3, 2, 1).toList) {
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
                while (!l.isEmpty) {
                    val item = l.head
                    tot += item
                    l = l.tail
                }
                tot
            }
            
            def tst(loops: Int, n: Int) = {
                val wh = time(loops, n) { testWhile }
                val fe = time(loops, n) { testForeach }
                (wh, fe)
            }
            val coldLoops = 1000
            assert(testForeach == testWhile)
            val (coldWhile, coldForeach) = tst(coldLoops, n)
            
            val warmupLoops = 2000
            tst(warmupLoops, n)
            
            val warmLoops = 10000
            val (warmWhile, warmForeach) = tst(warmLoops, n)
            
            println("Foreach (n = " + n + ")\t: \tCold = " + coldForeach + " ns \t\t\tWarm = " +warmForeach + " ns")
            println("  While (n = " + n + ")\t: \tCold = " + coldWhile + " ns \t\t\tWarm = " +warmWhile + " ns")
            def fmt(d: Double) = ((d * 100).toLong / 100.0).toString
            println("   Gain (n = " + n + ")\t: \t     x " + fmt(coldForeach / coldWhile) + "\t\t\t     x " + fmt(warmForeach / warmWhile))
            
            println()
        }
    }
}
