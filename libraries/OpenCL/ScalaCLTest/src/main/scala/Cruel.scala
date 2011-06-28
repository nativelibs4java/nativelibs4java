object Cruel { 
  def cfor[@specialized T](z: T)(ok: T=>Boolean)(succ: T=>T)(f: T=>Unit) { 
    var i = z 
    while (ok(i)) { 
      f(i) 
      i = succ(i) 
    } 
  } 

  def ptime[F](f: => F) = { 
    var t0 = System.nanoTime 
    val ans = f 
    printf("Elapsed: %.3f s\n",(System.nanoTime - t0)*1e-9) 
    ans 
  } 

  def sumWhile1(n: Int) = { 
    var i,s = 0 
    while (i < n) { 
      s += i 
      i += 1 
    } 
    s 
  } 

  def sumWhile3(n: Int, m: Int, l: Int) = { 
    var a,s = 0 
    while (a < n) { 
      var b = 0 
      while (b < m) { 
        var c = 0 
        while (c < l) { 
          s += c+l*(b+m*a); 
          c += 1 
        } 
        b += 1 
      } 
      a += 1 
    } 
    s 
  } 

  def sumWhile5(n: Int, m: Int, l: Int, k: Int, j: Int) = { 
    var a,s = 0 
    while (a < n) { 
      var b = 0 
      while (b < m) { 
        var c = 0 
        while (c < l) { 
          var d = 0 
          while (d < k) { 
            var e = 0 
            while (e < j) { 
              s += e+j*(d+k*(c+l*(b+m*a))); 
              e += 1 
            } 
            d += 1 
          } 
          c += 1 
        } 
        b += 1 
      } 
      a += 1 
    } 
    s 
  } 

  def weirdWhile(n: Int, m: Int) = { 
    var i,s = 0 
    while (i < n) { 
      var j = 0 
      while (j < m) { 
        s += j 
        j += 1 
      } 
      j = 1 
      while (j <= m) { 
        s *= j 
        j += 1 
      } 
      j = 0 
      while (j < m) { 
        s = j*j - s 
        j += 1 
      } 
      j = m 
      while (j > 0) { 
        s = j*j/(1+s*s) 
        j -= 1 
      } 
      j = 0 
      while (j < m) { 
        if (j*s > j+s) s += j 
        else s -= j 
        j += 1 
      } 
      i += 1 
    } 
    s 
  } 

  def sumLimit1(n: Int) = { 
    var s = 0 
    for (i <- 0 until n) s += i 
    s 
  } 

  def sumLimit3(n: Int, m: Int, l: Int) = { 
    var s = 0 
    for (a <- 0 until n; b <- 0 until m; c <- 0 until l) { s += c+l*(b+m*a) } 
    s 
  } 

  def sumLimit5(n: Int, m: Int, k: Int, j: Int, i: Int) = { 
    var s = 0 
    for (a <- 0 until n; 
         b <- 0 until m; 
         c <- 0 until k; 
         d <- 0 until j; 
         e <- 0 until i) { 
      s += e + i*(d + j*(c + k*(b + m*a))) 
    } 
    s 
  } 

  def weirdLimit(n: Int, m: Int) = { 
    var s = 0 
    for (i <- 0 to n) { 
      for (j <- 0 until m) { s += j } 
      for (j <- 1 to m) { s *= j } 
      for (j <- 0 until m) { s = j*j - s } 
      for (j <- m to 1 by -1) { s = j*j/(1+s*s) } 
      for (j <- 0 until m) { if (j*s > j+s) s += j else s -= j } 
    } 
    s 
  } 

  def sumCfor1(n: Int) = { 
    var s = 0 
    cfor(0)(_ < n)(_ + 1) { s += _ } 
    s 
  } 

  def sumCfor3(n: Int, m: Int, k: Int) = { 
    var s = 0 
    cfor(0)(_ < n)(_ + 1)(a => { 
      cfor(0)(_ < m)(_ + 1)(b => { 
        cfor(0)(_ < k)(_ + 1) { s += _ + k*(b + a*m) } 
      }) 
    }) 
    s 
  } 
  
  def sumCfor5(n: Int, m: Int, k: Int, j: Int, i: Int) = { 
    var s = 0 
    cfor(0)(_ < n)(_ + 1)(a => { 
      cfor(0)(_ < m)(_ + 1)(b => { 
        cfor(0)(_ < k)(_ + 1)(c => { 
          cfor(0)(_ < j)(_ + 1)(d => { 
            cfor(0)(_ < i)(_ + 1) { s += _ + i*(d + j*(c + k*(b + a*m))) } 
          }) 
        }) 
      }) 
    }) 
    s 
  } 

  def weirdCfor(n: Int, m: Int) = { 
    var s = 0 
    cfor(0)(_ < n)(_ + 1)(_ => { 
      cfor(0)(_ < m)(_ + 1) { s += _ } 
      cfor(1)(_ <= m)(_ + 1) { s *= _ } 
      cfor(0)(_ < m)(_ + 1) { j => s = j*j - s } 
      cfor(m)(_ > 0)(_ - 1) { j => s = j*j/(1+s*s) } 
      cfor(0)(_ < m)(_ + 1) { j => (if (j*s > j+s) s += j else s -= j) } 
    }) 
    s 
  } 

  def main(args: Array[String]) { 
    for (i <- 1 to 4) { 
      ptime( print(sumWhile1(1000000000) + " While1 ") ) 
      ptime( print(sumWhile3(1000,500,2000) + " While3 ") ) 
      ptime( print(sumWhile5(100,25,40,200,50) + " While5 ") ) 
      ptime( print(weirdWhile(100,1000000) + " While? ") ) 
      ptime( print(sumLimit1(1000000000) + " Limit1 ") ) 
      ptime( print(sumLimit3(1000,500,2000) + " Limit3 ") ) 
      ptime( print(sumLimit5(100,25,40,200,50) + " Limit5 ") ) 
      ptime( print(weirdLimit(100,1000000) + " Limit? ") ) 
      ptime( print(sumCfor1(1000000000) + "  Cfor1 ") ) 
      ptime( print(sumCfor3(1000,500,2000) + "  Cfor3 ") ) 
      ptime( print(sumCfor5(100,25,40,200,50) + "  Cfor5 ") ) 
      ptime( print(weirdCfor(100,1000000) + " Cfor?? ") ) 
      println 
    } 
  } 
} 
