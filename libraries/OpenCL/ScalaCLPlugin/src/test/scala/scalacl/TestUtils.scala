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
object TestUtils {
  private var _nextId = 1
  def nextId = TestUtils synchronized {
    val id = _nextId
    _nextId += 1
    id
  }
}
trait TestUtils {
  import TestUtils._
  
  implicit val baseOutDir = new File("target/testSnippetsClasses")
  baseOutDir.mkdirs

  val options = new ScalaCLPlugin.PluginOptions(null)
  
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

    compiler.compile(Array("-d", outDir.getAbsolutePath, srcFile.getAbsolutePath) ++ getScalaCLCollectionsPath)
    
    val f = new File(outDir, className + ".class")
    if (!f.exists())
      throw new RuntimeException("Class file " + f + " not found !")
    
    
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
      error("javap (args = " + args.mkString(" ") + ") failed with :\n" + err.synchronized { err.toString } + "\nAnd :\n" + out)
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
  
  trait RunnableMethod {
    def apply(args: Any*): Any
  }
  trait RunnableCode {
    def newInstance(constructorArgs: Any*): RunnableMethod
  }
  
  protected def compileCodeWithPlugin(decls: String, code: String) =
    compileCode(withPlugin = true, "", decls, "", code)
  
  lazy val getScalaCLCollectionsPath = {
    val r = """jar:file:(.*?\.jar)!.*""".r
    def getPath(c: Class[_]) = {
      val r(file) = c.getClassLoader.getResource(c.getName.replace('.', '/') + ".class").toString
      file
    }
    
    Array(
      "-cp", 
      Array(
        getPath(classOf[CLArray[_]])/*,
        getPath(classOf[com.nativelibs4java.opencl.CLBuffer[_]]),
        getPath(classOf[com.nativelibs4java.opencl.library.OpenCLLibrary]),
        getPath(classOf[org.bridj.BridJ]),
        getPath(classOf[org.objectweb.asm.ClassVisitor])*/
      ).mkString(File.pathSeparator)
    )
    //Array[String]()
  }
  protected def compileCode(withPlugin: Boolean, constructorArgsDecls: String, decls: String, methodArgsDecls: String, code: String) = {
    val (testClassName, testMethodName) = testClassInfo
    
    val suffixPlugin = (if (withPlugin) "Optimized" else "Normal")
    val className = "Test_" + testMethodName + "_" + suffixPlugin + "_" + nextId
    val src = "package " + packageName + "\nclass " + className + "(" + constructorArgsDecls + """) {
      """ + (if (decls == null) "" else decls) + """
      def """ + testMethodName + "(" + methodArgsDecls + ")" + """ = {
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
    
    var tmpFile = new File(outputDirectory, testMethodName + ".scala")
    val pout = new PrintStream(tmpFile)
    pout.println(src)
    pout.close
    //println(src)
    val compileArgs = Array("-d", outputDirectory.getAbsolutePath, tmpFile.getAbsolutePath) ++ getScalaCLCollectionsPath
    //println("Compiling '" + tmpFile.getAbsolutePath + "' with args '" + compileArgs.mkString(" ") +"'")
    (
      if (withPlugin) 
        SharedCompilerWithPlugins 
      else 
        SharedCompilerWithoutPlugins
    ).compile(compileArgs)
    
    //println("CLASS LOADER WITH PATH = '" + outputDirectory + "'")
    val loader = new URLClassLoader(Array(outputDirectory.toURI.toURL, new File(Compile.bootClassPath).toURI.toURL))
    
    val parent = 
      if (packageName == "") 
        outputDirectory
      else
        new File(outputDirectory, packageName.replace('.', File.separatorChar))
        
    val f = new File(parent, className + ".class")
    if (!f.exists())
      throw new RuntimeException("Class file " + f + " not found !")
    
    //compileFile(tmpFile, withPlugin, outputDirectory)
    
    val testClass = loader.loadClass(packageName + "." + className)
    val testMethod = testClass.getMethod(testMethodName)//, classOf[Int])
    val testConstructor = testClass.getConstructors.first
    
    new RunnableCode {
      override def newInstance(constructorArgs: Any*) = new RunnableMethod {
        val inst = 
          testConstructor.newInstance(constructorArgs.map(_.asInstanceOf[AnyRef]):_*).asInstanceOf[AnyRef]
        
        override def apply(args: Any*): Any = 
          testMethod.invoke(inst, args.map(_.asInstanceOf[AnyRef]):_*)
      }
    }
  }
  
  
  private def getTesterGen(withPlugin: Boolean, decls: String, code: String) = {
    val runnableCode = compileCode(withPlugin, "n: Int", decls, "", code)
    
    (n: Int) => {
      val i = runnableCode.newInstance(n)
      (isWarmup: Boolean) => {
        if (isWarmup) {
          i()
          null
        } else {
          System.gc
          Thread.sleep(50)
          val start = System.nanoTime
          val o = i().asInstanceOf[AnyRef]
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
