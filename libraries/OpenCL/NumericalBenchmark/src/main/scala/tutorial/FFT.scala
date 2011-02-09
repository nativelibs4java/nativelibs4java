package tutorial

import com.nativelibs4java.opencl._

case class Interval(offset: Int, stride: Int, length: Int) {
  
  def /(parts: Int): Array[Interval] = {
    val newLength = length / parts
    val newStride = stride * parts
    (0 until parts).map(i => Interval(offset + stride * i, newStride, newLength))(collection.breakOut)
  }
}
trait Op
case class Op1(i: Interval) extends Op
case class Op2(o1: Op, o2: Op) extends Op
case class Op4(o1: Op, o2: Op, o3: Op, o4: Op) extends Op

object FFTScala {
  
  def computeOp(i: Interval): Op = {
    /*if ((i.length & 3) == 0) { // i.length is a multiple of 4
      val Array(o1, o2, o3, o4) = (i / 4) map computeOp
      Op4(o1, o2, o3, o4)
    } else*/ if ((i.length & 1) == 0) { // i.length is odd
      val Array(o1, o2) = (i / 2) map computeOp
      Op2(o1, o2)
    } else {
      Op1(i)
    }
  }
  
  // mvn package && java -classpath target/javacl-tutorial-1.0-beta-6.jar tutorial.FFTScala
  // mvn scala:run -DmainClass=tutorial.FFTScala -DaddArgs=8
  def main(args: Array[String]): Unit = {
    implicit val context = JavaCL.createBestContext(CLPlatform.DeviceFeature.DoubleSupport)
    implicit val queue = context.createDefaultOutOfOrderQueueIfPossible
    println("Context = " + context)
    
    val Array(n) = args.map(_.toInt)
    val in = (0 until n).flatMap(i => Seq(i.toDouble, 0)).toArray//Array(1, 0, 2, 0, 3, 0, 4, 0, 5.0, 0, 6, 0)
    println("Data = " + in.toSeq)
    
    
    val outDitFFT2 = ditfft2(in.grouped(2).map({ case Array(x, y) => (x, y) }).toArray, true)
    println("DITFFT2 = " + outDitFFT2.toSeq)
    //println("BackDITFFT2 = " + ditfft2(outDitFFT2, false).toSeq)
    
    val outDitFFT4 = ditfft4InPlace(in.grouped(2).map({ case Array(x, y) => (x, y) }).toArray, true)
    println("DITFFT4 = " + outDitFFT4.toSeq)
    //println("BackDITFFT4 = " + ditfft4InPlace(outDitFFT4, false).toSeq)
    
    //val outDitFFTInPlace = fft.ditfft2InPlace(in.grouped(2).map({ case Array(x, y) => (x, y) }).toArray, true)
    //println("DITFFT2InPlace = " + outDitFFTInPlace.toSeq)
    //println("BackDITFFT2InPlace = " + fft.ditfft2InPlace(outDitFFTInPlace, false).toSeq)
    
    /*val outDitFFTInPlaceCL = fft.ditfft2InPlaceCL(in, true)
    println("DITFFT2InPlace = " + outDitFFTInPlaceCL.toSeq)
    println("BackDITFFT2InPlaceCL = " + fft.ditfft2InPlaceCL(outDitFFTInPlaceCL, false).toSeq)
    */
   
    //val outFFT = fft.fft(in, true)
    //println("FFT  = " + outFFT.toSeq)
    //println("BackFFT = " + fft.fft(outFFT, false).toSeq)
  }
  
  // http://en.wikipedia.org/wiki/Cooley%E2%80%93Tukey_FFT_algorithm
  def ditfft2(X: Array[(Double, Double)], forward: Boolean): Array[(Double, Double)] = {
    val Y = ditfft2(X, X.length, 1, !forward)
    if (forward)
      Y
    else {
      val l = X.length.toDouble
      Y.map { case (x, y) => (x / l, y / l) }
    }
  }
  def ditfft2InPlace(X: Array[(Double, Double)], forward: Boolean): Array[(Double, Double)] = {
    val Y = new Array[(Double, Double)](X.length)
    ditfft2InPlace(X, X.length, 1, !forward, 0, Y, 0)
    if (forward)
      Y
    else {
      val l = X.length.toDouble
      Y.map { case (x, y) => (x / l, y / l) }
    }
  }
    
  private def ditfft2(X: Array[(Double, Double)], N: Int, s: Int, inverse: Boolean, offset: Int = 0): Array[(Double, Double)] = {
    assert(N >= 0)
    if (N == 1) {
      println("Terminal : " + offset)
      Array(X(offset))
    }
    else {
      import scala.math._
      val N2 = N / 2
      val s2 = s * 2
      val Y = ditfft2(X, N2, s2, inverse, offset) ++ ditfft2(X, N2, s2, inverse, offset + s)
      println("Y(N = " + N + ") = " + Y.mkString(", "))
      for (k <- 0 until N / 2) {
        val param: Double = - 2 * Pi * k / N.toDouble * (if (inverse) -1 else 1)
        val c: Double = cos(param)
        val s: Double = sin(param)
        
        val (y1Real, y1Imag) = Y(k)
        val (y2Real, y2Imag) = Y(k + N / 2)
       
        // (c + i * s) * (y2Real + i * y2Imag)
        
        val vReal = c * y2Real - s * y2Imag
        val vImag = c * y2Imag + s * y2Real
        Y(k) = (
          y1Real + vReal,
          y1Imag + vImag
        )
        Y(k + N / 2) = (
          y1Real - vReal,
          y1Imag - vImag 
        )
      }
      Y
    }
  }
  
  def ditfft4InPlace(X: Array[(Double, Double)], forward: Boolean): Array[(Double, Double)] = {
    val Y = new Array[(Double, Double)](X.length)
    ditfft4InPlace(X, X.length, 1, !forward, 0, Y, 0)
    if (forward)
      Y
    else {
      val l = X.length.toDouble
      Y.map { case (x, y) => (x / l, y / l) }
    }
  }
    
  private def ditfft2InPlace(X: Array[(Double, Double)], N: Int, s: Int, inverse: Boolean, offsetX: Int, Y: Array[(Double, Double)], offsetY: Int): Unit = {
    //println("Inplace exec N = " + N + ", s = " + s + ", offsetX = " + offsetX + ", offsetY = " + offsetY)
    assert(N >= 0)
    if (N == 1) {
      //println("Terminal : " + offsetX + " going to " + offsetY)
      Y(offsetY) = X(offsetX)
      //Array(X(offsetX))
    }
    else {
      import scala.math._
      val halfN = N / 2
      val twiceS = s * 2
      
      ditfft2InPlace(X, halfN, twiceS, inverse, offsetX, Y, offsetY)
      ditfft2InPlace(X, halfN, twiceS, inverse, offsetX + s, Y, offsetY + halfN)
      //println("Y(N = " + N + ") = " + Y.mkString(", "))
      for (k <- 0 until halfN) {
        val param: Double = - 2 * Pi * k / N.toDouble * (if (inverse) -1 else 1)
        val c: Double = cos(param)
        val s: Double = sin(param)
        
        val (y1Real, y1Imag) = Y(offsetY + k)
        val (y2Real, y2Imag) = Y(offsetY + k + N / 2)
       
        // (c + i * s) * (y2Real + i * y2Imag)
        
        val vReal = c * y2Real - s * y2Imag
        val vImag = s * y2Real + c * y2Imag 
        Y(offsetY + k) = (
          y1Real + vReal,
          y1Imag + vImag
        )
        Y(offsetY + k + N / 2) = (
          y1Real - vReal,
          y1Imag - vImag 
        )
      }
    }
  }
  
  private def ditfft4InPlace(X: Array[(Double, Double)], N: Int, s: Int, inverse: Boolean, offsetX: Int, Y: Array[(Double, Double)], offsetY: Int): Unit = {
    //println("Inplace exec N = " + N + ", s = " + s + ", offsetX = " + offsetX + ", offsetY = " + offsetY)
    assert(N > 0)
    if (N <= 1) {
      //println("Terminal : " + offsetX + " going to " + offsetY)
      Y(offsetY) = X(offsetX)
      //Array(X(offsetX))
    }
    else {
      import scala.math._
      val subN = N / 4
      val subS = s * 4
      
      ditfft4InPlace(X, subN, subS, inverse, offsetX, Y, offsetY)
      ditfft4InPlace(X, subN, subS, inverse, offsetX + s, Y, offsetY + subN)
      ditfft4InPlace(X, subN, subS, inverse, offsetX + s * 2, Y, offsetY + subN * 2)
      ditfft4InPlace(X, subN, subS, inverse, offsetX + s * 3, Y, offsetY + subN * 3)

      /// TODO !!!!
      //println("Y(N = " + N + ") = " + Y.mkString(", "))
      /*
      for (k <- 0 until subN) {
        val param: Double = - 2 * Pi * k / N.toDouble * (if (inverse) -1 else 1)
        val c: Double = cos(param)
        val s: Double = sin(param)
        
        val o = offsetY + k
        val (y1Real, y1Imag) = Y(o)
        val (y2Real, y2Imag) = Y(o + subN)
        val (y3Real, y3Imag) = Y(o + subN * 2)
        val (y4Real, y4Imag) = Y(o + subN * 3)
       
        // (c + i * s) * (y2Real + i * y2Imag)
        
        val vReal1 = c * y3Real - s * y3Imag
        val vImag1 = s * y3Real + c * y3Imag 
        
        val vReal2 = c * y2Real - s * y2Imag
        val vImag2 = s * y2Real + c * y2Imag
        
        Y(o) = (
          y1Real + vReal1 - vReal2,
          y1Imag + vImag2 - vImag2
        )
        Y(o + subN) = ( // (1, 0.5) => (-0.5, 1)
          y1Real + vReal1 + vReal2,
          y1Imag + vImag2 + vImag2 
        )
        Y(o + subN * 2) = (
          y1Real - vReal1 - vReal2,
          y1Imag - vImag2 - vImag2 
        )
        Y(o + subN * 3) = ( // (1, 0.5) => 
          y1Real - vReal1 + vReal2,
          y1Imag - vImag2 + vImag2
        )
      }*/
    }
  }
}
/*

x x x x x x x x Interval(0, 1, 8)

x   x   x   x   Interval(0, 2, 4)
  x   x   x   x Interval(1, 2, 4)
  
x       x       Interval(0, 4, 2)
    x       x   Interval(2, 4, 2)
  x       x     Interval(1, 4, 2)
      x       x Interval(3, 4, 2)

*/
