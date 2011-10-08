import scala.sys.process._
import System.{ getenv, getProperty }
import java.io.{ File, PrintWriter }

object Bisect extends App {
  val minRev = 1183
  val maxRev = 1241
 
  def file(path: String) = new File(path)
  implicit def fileImplicits(f: File) = new {
    def /(subPath: String) = new File(f, subPath)
  }
  val homeDir = file(getProperty("user.home"))
  val dyncallDir = homeDir / "src/dyncall"
  val bridjDir = homeDir / "nativelibs4java/Runtime/BridJ"
  
  def svnUpdate(dir: File, rev: Int): Unit = {
    Process(Seq("svn", "update", "-r" + rev), Some(dir)) !!
    
    //println("Svn tree '" + dir + "' updated to revision " + rev);
    //if (ret != 0)
    //  throw new RuntimeException("svn update returned " + ret)
  }
  def svnLog(dir: File, rev: Int): String = {
    Process(Seq("svn", "log", "-r" + rev), Some(dir)) !!
  }
  def svnDiff(dir: File, from: Int, to: Int): String = {
    Process(Seq("svn", "diff", "-r" + from + ":" + to), Some(dir)) !!
  }
    
  def concatLogger = {
    val builder = new StringBuilder
    (ProcessLogger(s => builder.append(s).append('\n')), () => builder.toString)
  }
    
  sealed trait TestResult { def describe: String }
  case class BuildFailure(out: String) extends TestResult { override def describe = "Broken" }
  case class TestFailure(out: String) extends TestResult { override def describe = "Failure" }
  object TestSuccess extends TestResult { override def describe = "Success" }
  
  def test(rev: Int): TestResult = {
    print("Updating to rev " + rev + "... ")
    svnUpdate(dyncallDir, rev)
    print("Building... ")
        
    val ret = { 
      val (buildLog, buildOut) = concatLogger
      val buildRet: Int = 
        (
          Process(Seq("sh", "CleanNative"), Some(bridjDir)) #&&
          Process(Seq("sh", "BuildNative"), Some(bridjDir))
        ) ! (buildLog)
        
      if (buildRet != 0) {
        BuildFailure(buildOut())
      } else {
        print("Testing... ")
        val (testLog, testOut) = concatLogger
        val testRet: Int =
          Process(Seq("mvn", "test", "-o", "-Dtest=CallTest"), Some(bridjDir)) ! (testLog)
      
        if (testRet == 0)
          TestSuccess
        else
          TestFailure(testOut())
      }
    }
    
    println(ret.describe + ".")
    
    ret
    //println("Res = " + res)
  }
  
  import scala.annotation.tailrec
  
  @tailrec
  case class BisectRes(firstFailedTest: Int, firstBrokenBuild: Option[Int] = None)
  
  def bisect(fromOkRev: Int, toBadRev: Int): BisectRes = {
    if (toBadRev <= fromOkRev + 1)
      BisectRes(toBadRev)
    else {
      var pick = (fromOkRev + toBadRev) / 2
      if (pick == fromOkRev)
        pick += 1

      test(pick) match {
        case TestSuccess =>
          bisect(pick, toBadRev)
        case _: TestFailure =>
          bisect(fromOkRev, pick)
        case _: BuildFailure =>
          val BisectRes(failed, broken) =
            bisect(pick, toBadRev)
            
          BisectRes(failed, Some(pick))
      }
    }
  }
  
  
  // sbt "run 1183 1241"
  object SomeInt {
    def unapply(s: String) =
      try Some(s.toInt) catch { case _ => None }
  }
  args.toList match {
    case SomeInt(fromRev) :: SomeInt(toRev) :: xargs =>
      val (logFile, diffFile) = xargs match {
        case l :: d :: Nil => (l, d)
        case Nil => ("bisect.log", "bisect.diff")
      }
      println("Bisect from rev " + fromRev + " to rev " + toRev)
      assert(test(fromRev) == TestSuccess, "Does not build at rev " + fromRev)
      assert(test(toRev) != TestSuccess, "Already builds at rev " + toRev)
      val bisection = bisect(fromRev, toRev)
      //val bisection = BisectRes(1236,Some(1226))
      val (revisions, (diffFrom, diffTo)) = bisection match {
        case BisectRes(failed, Some(broken)) => 
          println("Tests fail at revision " + failed + " but build broken since revision " + broken)
          (broken to failed, (broken - 1, failed))
        case BisectRes(failed, None) =>
          println("Faulty revision is " + failed)
          (Seq(failed), (failed - 1, failed))
      }
      
      val logOut = new PrintWriter(file(logFile))
      for (rev <- revisions) {
        val log = svnLog(dyncallDir, rev)
        println(log)
        logOut.println(log)
      }
      logOut.close
      
      val diffOut = new PrintWriter(file(diffFile))
      val diff = svnDiff(dyncallDir, diffFrom, diffTo)
      println(diff)
      diffOut.println(diff)
      diffOut.close
  }
}
