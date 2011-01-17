package scalacl

import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import scala.collection.mutable.HashMap
import scala.concurrent.ops
import scala.io.Source

import java.net.URI
import java.net.URLClassLoader
import javax.tools.DiagnosticCollector
import javax.tools.FileObject
import javax.tools.ForwardingJavaFileManager
import javax.tools.JavaCompiler
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.ToolProvider
import org.junit.Assert._
import scala.tools.nsc.Settings
import scala.actors.Futures._
import Function.{tupled, untupled}

object SharedCompilerWithPlugins extends SharedCompiler(true)
object SharedCompilerWithoutPlugins extends SharedCompiler(false)
//object SharedCompilerWithoutPlugins1 extends SharedCompiler(false)
//object SharedCompilerWithoutPlugins2 extends SharedCompiler(false)

object Results {
  import java.io._
  import java.util.Properties
  def getPropertiesFileName(n: String) = n + ".perf.properties"
  val logs = new scala.collection.mutable.HashMap[String, (String, PrintStream, Properties)]
  def getLog(key: String) = {
    logs.getOrElseUpdate(key, {
      val logName = getPropertiesFileName(key)
      //println("Opening performance log file : " + logName)
      
      val logRes = getClass.getClassLoader.getResourceAsStream(logName)
      val properties = new java.util.Properties
      if (logRes != null) {
        println("Reading " + logName)
        properties.load(logRes)
      }
      (logName, new PrintStream(logName), properties)
    })
  } 
  Runtime.getRuntime.addShutdownHook(new Thread { override def run {
    for ((_, (logName, out, _)) <- logs) {
      println("Wrote " + logName)
      out.close
    }
  }})
}
trait TestUtils {

  implicit val baseOutDir = new File("target/testSnippetsClasses")
  baseOutDir.mkdirs

  /*def compile(src: String, outDir: String) = {
    outDir.mkdirs

    val srcFile = File.createTempFile("temp", ".scala")
    val out = new PrintWriter(srcFile)
    out.println(src)
    out.close
    //srcFile.delete

  }*/
  def getSnippetBytecode(className: String, source: String, subDir: String, compiler: SharedCompiler) = {
    val src = "class " + className + " { def invoke(): Unit = {\n" + source + "\n}}"
    val outDir = new File(baseOutDir, subDir)
    outDir.mkdirs
    val srcFile = new File(outDir, className + ".scala")
    val out = new PrintWriter(srcFile)
    out.println(src)
    out.close
    new File(outDir, className + ".class").delete

    compiler.compile(Array("-d", outDir.getAbsolutePath, srcFile.getAbsolutePath))
    
    val byteCodeSource = getClassByteCode(className, outDir.getAbsolutePath)
    val byteCode = byteCodeSource.mkString//("\n")
    /*
     println("COMPILED :")
     println("\t" + source.replaceAll("\n", "\n\t"))
     println("BYTECODE :")
     println("\t" + byteCode.replaceAll("\n", "\n\t"))
     */
    
    byteCode.
      replaceAll("scala/reflect/ClassManifest", "scala/reflect/Manifest").
      replaceAll("#\\d+", "")
  }

  def ensurePluginCompilesSnippet(source: String) = {
    val (_, testMethodName) = testClassInfo
    assertNotNull(getSnippetBytecode(testMethodName, source, "temp", SharedCompilerWithPlugins))
  }
  def ensurePluginCompilesSnippetsToSameByteCode(sourcesAndReferences: Traversable[(String, String)]): Unit = {
    def flatten(s: Traversable[String]) = s.map("{\n" + _ + "\n};").mkString("\n")
    ensurePluginCompilesSnippetsToSameByteCode(flatten(sourcesAndReferences.map(_._1)), flatten(sourcesAndReferences.map(_._2)))
  }
  def ensurePluginCompilesSnippetsToSameByteCode(source: String, reference: String, allowSameResult: Boolean = false) = {
    val (_, testMethodName) = testClassInfo
    
    import scala.concurrent.ops._
    implicit val runner = new scala.concurrent.ThreadRunner
  
    /*
    val expectedFut = future { getSnippetBytecode(className, reference, "expected", SharedCompilerWithoutPlugins1) }
    val withoutPluginFut = future { getSnippetBytecode(className, source, "withoutPlugin", SharedCompilerWithoutPlugins2) }
    val withPluginFut = future { getSnippetBytecode(className, source, "withPlugin", SharedCompilerWithPlugins) }//TestUtils.compilerWithPlugin) }
    val (expected, withoutPlugin, withPlugin) = (expectedFut(), withoutPluginFut(), withPluginFut())
    */
    val enableFuture = true

    def futEx[V](b: => V): () => V = if (!enableFuture) () => b else {
      val f = future { try { Right(b) } catch { case ex => Left(ex) } }
      () => f() match {
        case Left(ex) =>
          ex.printStackTrace
          assertTrue(ex.toString, false)
          error("")
        case Right(v) =>
          v
      }
    }

    val withPluginFut = futEx { getSnippetBytecode(testMethodName, source, "withPlugin", SharedCompilerWithPlugins) }
    val expected = getSnippetBytecode(testMethodName, reference, "expected", SharedCompilerWithoutPlugins)
    val withoutPlugin = if (allowSameResult) null else getSnippetBytecode(testMethodName, source, "withoutPlugin", SharedCompilerWithoutPlugins)
    val withPlugin = withPluginFut()

    if (!allowSameResult)
      assertTrue("Expected result already found without any plugin !!! (was the Scala compiler improved ?)", expected != withoutPlugin)
      
    if (expected != withPlugin) {
      def trans(tit: String, s: String) =
        println(tit + " :\n\t" + s.replaceAll("\n", "\n\t"))

      trans("EXPECTED", expected)
      trans("FOUND", withPlugin)

      assertEquals(expected, withPlugin)
    }
  }
  def getClassByteCode(className: String, classpath: String) = {
    val args = Array("-c", "-classpath", classpath, className)
    val p = Runtime.getRuntime.exec("javap " + args.mkString(" "))//"javap", args)

    var err = new StringBuffer
    ops.spawn {
      import scala.util.control.Exception._
      val inputStream = new BufferedReader(new InputStreamReader(p.getErrorStream))
      var str: String = null
      //ignoring(classOf[IOException]) {
        while ({ str = inputStream.readLine; str != null }) {
          //err.synchronized {
            println(str)
            err.append(str).append("\n")
          //}
        //}
      }
    }

    val out = Source.fromInputStream(p.getInputStream).toList
    if (p.waitFor != 0) {
      Thread.sleep(100)
      error("javap failed with :\n" + err.synchronized { err.toString } + "\nAnd :\n" + out)
    }
    out
  }

  import java.io.File
  /*val outputDirectory = {
    val f = new File(".")//target/classes")
    if (!f.exists)
      f.mkdirs
    f
  }*/

  import java.io._

  val packageName = "tests"

  case class Res(withPlugin: Boolean, output: AnyRef, time: Double)
  type TesterGen = Int => (Boolean => Res)
  
  def fail(msg: String) = {
    println(msg)
    println()
    assertTrue(msg, false)
  }
  
  private def getTesterGen(withPlugin: Boolean, decls: String, code: String) = {
    val (testClassName, testMethodName) = testClassInfo
    
    val suffixPlugin = (if (withPlugin) "Optimized" else "Normal")
    val className = "Test_" + testMethodName + "_" + suffixPlugin
    val src = "package " + packageName + "\nclass " + className + """(n: Int) {
      """ + (if (decls == null) "" else decls) + """
      def """ + testMethodName + """ = {
      """ + code + """
      }
    }"""

    val outputDirectory = new File("tmpTestClasses" + suffixPlugin)
    def del(dir: File): Unit = {
      val fs = dir.listFiles
      if (fs != null)
        fs foreach del
      
      dir.delete
    }

    del(outputDirectory)
    outputDirectory.mkdirs
    val loader = new URLClassLoader(Array(outputDirectory.toURI.toURL, new File(Compile.bootClassPath).toURI.toURL))

    var tmpFile = new File(outputDirectory, testMethodName + ".scala")
    val pout = new PrintStream(tmpFile)
    pout.println(src)
    pout.close
    //println(src)
    (
      if (withPlugin) 
        SharedCompilerWithPlugins 
      else 
        SharedCompilerWithoutPlugins
    ).compile(Array("-d", outputDirectory.getAbsolutePath, tmpFile.getAbsolutePath))
    
    //compileFile(tmpFile, withPlugin, outputDirectory)
    
    val testClass = loader.loadClass(packageName + "." + className)
    val testMethod = testClass.getMethod(testMethodName)//, classOf[Int])
    val testConstructor = testClass.getConstructors.first
    def instance(n: Int): AnyRef = testConstructor.newInstance(n.asInstanceOf[AnyRef]).asInstanceOf[AnyRef]
    def invoke(i: AnyRef) = testMethod.invoke(i)
    
    (n: Int) => {
      val i = instance(n)
      (isWarmup: Boolean) => {
        if (isWarmup) {
          invoke(i)
          null
        } else {
          System.gc
          Thread.sleep(50)
          val start = System.nanoTime
          val o = invoke(i)
          val time: Double = System.nanoTime - start
          Res(withPlugin, o, time)
        }
      }
    }
  }
  def testClassInfo = {
    val testTrace = new RuntimeException().getStackTrace.filter(se => se.getClassName.endsWith("Test")).last
    val testClassName = testTrace.getClassName
    val methodName = testTrace.getMethodName
    (testClassName, methodName)
  }
  
  val defaultExpectedFasterFactor = Option(System.getenv("SCALACL_MIN_PERF")).map(_.toDouble).getOrElse(0.95)
  val perfRuns = Option(System.getenv("SCALACL_PERF_RUNS")).map(_.toInt).getOrElse(4)
  
  def ensureCodeWithSameResult(code: String): Unit = {
    val (testClassName, testMethodName) = testClassInfo
    
    val gens @ Array(genWith, genWithout) = Array(getTesterGen(true, "", code), getTesterGen(false, "", code))
      
    val testers @ Array(testerWith, testerWithout) = gens.map(_(-1))
      
    val firstRun = testers.map(_(false))
    val Array(optimizedOutput, normalOutput) = firstRun.map(_.output)
    
    val pref = "[" + testMethodName + "] "
    if (normalOutput != optimizedOutput) {
      fail(pref + "ERROR: Output is not the same !\n" + pref + "\t   Normal output = " + normalOutput + "\n" + pref + "\tOptimized output = " + optimizedOutput)
    }
  }
  def ensureFasterCodeWithSameResult(decls: String, code: String, params: Seq[Int] = Array(2, 10, 1000, 100000)/*10000, 100, 20, 2)*/, nRuns: Int = perfRuns, minFaster: Double = 1.0): Unit = {
    
    val (testClassName, methodName) = testClassInfo
    
    val gens @ Array(genWith, genWithout) = Array(getTesterGen(true, decls, code), getTesterGen(false, decls, code))
      
    def run = params.toList.sorted.map(param => {
      //println("Running with param " + param)
      val testers @ Array(testerWith, testerWithout) = gens.map(_(param))
      
      val firstRun = testers.map(_(false))
      val Array(optimizedOutput, normalOutput) = firstRun.map(_.output)
      
      val pref = "[" + methodName + ", n = " + param + "] "
      if (normalOutput != optimizedOutput) {
        fail(pref + "ERROR: Output is not the same !\n" + pref + "\t   Normal output = " + normalOutput + "\n" + pref + "\tOptimized output = " + optimizedOutput)
      }
      
      val runs: List[Res] = firstRun.toList ++ (1 until nRuns).toList.flatMap(_ => testers.map(_(false)))
      def calcTime(list: List[Res]) = {
        val times = list.map(_.time)
        times.sum / times.size.toDouble
      }
      val (runsWithPlugin, runsWithoutPlugin) = runs.partition(_.withPlugin)
      val (timeWithPlugin, timeWithoutPlugin) = (calcTime(runsWithPlugin), calcTime(runsWithoutPlugin))
      
      (param, timeWithoutPlugin / timeWithPlugin)
    }).toMap
    
    val (logName, log, properties) = Results.getLog(testClassName)
    
    //println("Cold run...")
    val coldRun = run
    
    //println("Warming up...");
    // Warm up the code being benchmarked :
    {
      val testers = gens.map(_(5))
      (0 until 2500).foreach(_ => testers.foreach(_(true)))
    };
    
    //println("Warm run...")
    val warmRun = run
    
    
    val errors = coldRun.flatMap { case (param, coldFactor) =>
      val warmFactor = warmRun(param)
      //println("coldFactor = " + coldFactor + ", warmFactor = " + warmFactor)
      
      def f2s(f: Double) = ((f * 10).toInt / 10.0) + ""
      def printFacts(warmFactor: Double, coldFactor: Double) = {
        val txt = methodName + "\\:" + param + "=" + Array(warmFactor, coldFactor).map(f2s).mkString(";")
        //println(txt)
        log.println(txt)
      }
      //def printFact(factor: Double) = log.println(methodName + "\\:" + param + "=" + f2s(factor))
      val (expectedWarmFactor, expectedColdFactor) = {
      //val expectedColdFactor = {
        val p = Option(properties.getProperty(methodName + ":" + param)).map(_.split(";")).orNull
        if (p != null && p.length == 2) {
          //val Array(c) = p.map(_.toDouble)
          //val c = p.toDouble; printFact(c); c
          //log.print("# Test result (" + (if (actualFasterFactor >= f) "succeeded" else "failed") + "): ")
          val Array(w, c) = p.map(_.toDouble)
          printFacts(w, c)
          (w, c)
        } else {
          //printFact(coldFactor - 0.1); 1.0
          printFacts(warmFactor - 0.1, coldFactor - 0.1)
          (defaultExpectedFasterFactor, defaultExpectedFasterFactor)
        }
      }
      
      def check(warm: Boolean, factor: Double, expectedFactor: Double) = {
        val pref = "[" + methodName + ", n = " + param + ", " + (if (warm) "warm" else "cold") + "] "
        
        if (factor >= expectedFactor) {
          println(pref + "  OK (" + factor + "x faster, expected > " + expectedFactor + "x)")
          Nil
        } else {
          val msg = "ERROR: only " + factor + "x faster (expected >= " + expectedFactor + "x)"
          println(pref + msg)
          List(msg)
        }
      }
      
      check(false, coldFactor, expectedColdFactor) ++
      check(true, warmFactor, expectedWarmFactor)
    }
    try {
      if (!errors.isEmpty)
        assertTrue(errors.mkString("\n"), false)
    } finally {
      println()
    }
  }

}
