/* Benchmark modified from: 

   The Computer Language Benchmarks Game 
   http://shootout.alioth.debian.org/ 
   contributed by Isaac Gouy 
   modified by Meiko Rachimow
   modified by Olivier Chafik
   updated for 2.8 by Rex Kerr 
*/ 
/*
sbt clean compile && JAVA_OPTS="-Xmx3G" scala -cp target/scala_2.8.0/classes/ nbody
DISABLE_SCALACL_PLUGIN=1 sbt clean compile && JAVA_OPTS="-Xmx3G" scala -cp target/scala_2.8.0/classes/ nbody

*/
import math._ 

object nbody { 
  def runWhile(n: Int) { 
    val josys = new JovianSystem 
    val e0 = josys.energy 
    var i = 0 
    while (i < n) { josys.advanceWhile(0.01); i += 1 } 
    printf("While Energy: %.9f; change: %.9f.  ",e0,(josys.energy - e0)) 
  } 

  def runLimit(n: Int) { 
    val josys = new JovianSystem 
    val e0 = josys.energy 
    for (i <- 1 to n) { josys.advanceLimit(0.01) } 
    printf("Limit Energy: %.9f; change: %.9f.  ",e0,(josys.energy - e0)) 
  } 

  def runRange(n: Int) { 
    val josys = new JovianSystem 
    val e0 = josys.energy 
    for (i <- 0 until n) { josys.advanceRange(0.01) } 
    printf("Range Energy: %.9f; change: %.9f.  ",e0,(josys.energy - e0)) 
  } 

  def ptime[F](f: => F): F = { 
    val t0 = System.nanoTime 
    val ans = f 
    printf("Elapsed: %.3f s\n",(System.nanoTime-t0)*1e-9) 
    ans 
  } 

  def main(args: Array[String]) = { 
    val n = try { args(0).toInt } catch { case _ => 2000000 } 
    for (i <- 1 to 3) { 
      ptime( runWhile(n) ) 
      ptime( runLimit(n) ) 
      ptime( runRange(n) ) 
    } 
  } 
} 


abstract class NBodySystem { 

  def energy() = { 
    var e = 0.0 
    for (i <- 0 until bodies.length) { 
      e += 0.5 * bodies(i).mass * bodies(i).speedSq 

      for (j <- i+1 until bodies.length) { 
        val dx = bodies(i).x - bodies(j).x 
        val dy = bodies(i).y - bodies(j).y 
        val dz = bodies(i).z - bodies(j).z 
        val distance = sqrt(dx*dx + dy*dy + dz*dz) 
        e -= (bodies(i).mass * bodies(j).mass) / distance 
      } 
    } 
    e 
  } 

  def advanceWhile(dt: Double) = { 
    var i = 0 
    while (i < bodies.length){ 
      var j = i+1 
      while (j < bodies.length){ 
        val dx = bodies(i).x - bodies(j).x 
        val dy = bodies(i).y - bodies(j).y 
        val dz = bodies(i).z - bodies(j).z 

        val distance = sqrt(dx*dx + dy*dy + dz*dz) 
        val mag = dt / (distance * distance * distance) 

        bodies(i).advance(dx,dy,dz,-bodies(j).mass*mag) 
        bodies(j).advance(dx,dy,dz,bodies(i).mass*mag) 

        j += 1 
      } 
      i += 1 
    } 

    i = 0 
    while (i < bodies.length){ 
      bodies(i).move(dt) 
      i += 1 
    } 
  } 

  def advanceLimit(dt: Double) = { 
    for (i <- 0 until bodies.length ; j <- i+1 until bodies.length) { 
      val bi = bodies(i)
      val bj = bodies(j)
      val dx = bi.x - bj.x 
      val dy = bi.y - bj.y 
      val dz = bi.z - bj.z 

      val distance = sqrt(dx*dx + dy*dy + dz*dz) 
      val mag = dt / (distance * distance * distance) 

      bi.advance(dx,dy,dz,-bj.mass*mag) 
      bj.advance(dx,dy,dz,bi.mass*mag) 
    } 
    bodies.foreach(_.move(dt)) 
  } 

  def advanceRange(dt: Double) = { 
    for (i <- 0 until bodies.length) { 
      for (j <- i+1 until bodies.length) { 
        val dx = bodies(i).x - bodies(j).x 
        val dy = bodies(i).y - bodies(j).y 
        val dz = bodies(i).z - bodies(j).z 

        val distance = sqrt(dx*dx + dy*dy + dz*dz) 
        val mag = dt / (distance * distance * distance) 

        bodies(i).advance(dx,dy,dz,-bodies(j).mass*mag) 
        bodies(j).advance(dx,dy,dz,bodies(i).mass*mag) 
      } 
    } 

    for (i <- 0 until bodies.length) { bodies(i).move(dt) } 
  } 

  protected val bodies: Array[Body] 

  class Body(){ 
    var x,y,z = 0.0 
    var vx,vy,vz = 0.0 
    var mass = 0.0 
    def speedSq = vx*vx + vy*vy + vz*vz 
    @inline def move(dt: Double) { 
      x += dt*vx 
      y += dt*vy 
      z += dt*vz 
    } 
    @inline def advance(dx: Double, dy: Double, dz: Double, delta: Double) { 
      vx += dx*delta 
      vy += dy*delta 
      vz += dz*delta 
    } 
  } 
} 

class JovianSystem extends NBodySystem { 
   protected val bodies = initialValues 

   private def initialValues() = { 
      val SOLAR_MASS = 4 * Pi * Pi 
      val DAYS_PER_YEAR = 365.24 

      val sun = new Body 
      sun.mass = SOLAR_MASS 

      val jupiter = new Body 
      jupiter.x = 4.84143144246472090e+00 
      jupiter.y = -1.16032004402742839e+00 
      jupiter.z = -1.03622044471123109e-01 
      jupiter.vx = 1.66007664274403694e-03 * DAYS_PER_YEAR 
      jupiter.vy = 7.69901118419740425e-03 * DAYS_PER_YEAR 
      jupiter.vz = -6.90460016972063023e-05 * DAYS_PER_YEAR 
      jupiter.mass = 9.54791938424326609e-04 * SOLAR_MASS 

      val saturn = new Body 
      saturn.x = 8.34336671824457987e+00 
      saturn.y = 4.12479856412430479e+00 
      saturn.z = -4.03523417114321381e-01 
      saturn.vx = -2.76742510726862411e-03 * DAYS_PER_YEAR 
      saturn.vy = 4.99852801234917238e-03 * DAYS_PER_YEAR 
      saturn.vz = 2.30417297573763929e-05 * DAYS_PER_YEAR 
      saturn.mass = 2.85885980666130812e-04 * SOLAR_MASS 

      val uranus = new Body 
      uranus.x = 1.28943695621391310e+01 
      uranus.y = -1.51111514016986312e+01 
      uranus.z = -2.23307578892655734e-01 
      uranus.vx = 2.96460137564761618e-03 * DAYS_PER_YEAR 
      uranus.vy = 2.37847173959480950e-03 * DAYS_PER_YEAR 
      uranus.vz = -2.96589568540237556e-05 * DAYS_PER_YEAR 
      uranus.mass = 4.36624404335156298e-05 * SOLAR_MASS 

      val neptune = new Body 
      neptune.x = 1.53796971148509165e+01 
      neptune.y = -2.59193146099879641e+01 
      neptune.z = 1.79258772950371181e-01 
      neptune.vx = 2.68067772490389322e-03 * DAYS_PER_YEAR 
      neptune.vy = 1.62824170038242295e-03 * DAYS_PER_YEAR 
      neptune.vz = -9.51592254519715870e-05 * DAYS_PER_YEAR 
      neptune.mass = 5.15138902046611451e-05  * SOLAR_MASS 


      val initialValues = Array ( sun, jupiter, saturn, uranus, neptune ) 

      var px = 0.0; var py = 0.0; var pz = 0.0; 
      for (b <- initialValues){ 
         px += (b.vx * b.mass) 
         py += (b.vy * b.mass) 
         pz += (b.vz * b.mass) 
      } 
      sun.vx = -px / SOLAR_MASS 
      sun.vy = -py / SOLAR_MASS 
      sun.vz = -pz / SOLAR_MASS 

      initialValues 
   } 
} 
