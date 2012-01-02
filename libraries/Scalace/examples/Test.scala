class Test {
  //val a = Array(1, 2) ; 
  //val r: Array[Array[Int]] = a.map(xx => a.map(x => { def f = x ; f }))
  //List(Array(1,2), Array(3,4)).toArray.map(_.toSeq).toSeq
  val a = Array(1, 2) ; val r: Array[(Int, Int)] = for (v <- a) yield (v, v)
}
