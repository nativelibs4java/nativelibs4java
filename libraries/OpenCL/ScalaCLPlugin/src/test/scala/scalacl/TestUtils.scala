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

object SharedCompilerWithPlugins extends SharedCompiler(true)
object SharedCompilerWithoutPlugins extends SharedCompiler(false)
//object SharedCompilerWithoutPlugins1 extends SharedCompiler(false)
//object SharedCompilerWithoutPlugins2 extends SharedCompiler(false)

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
    byteCode.replaceAll("#\\d+", "")
  }

  def ensurePluginCompilesSnippet(className: String, source: String) = {
    assertNotNull(getSnippetBytecode(className, source, "temp", SharedCompilerWithPlugins))
  }
  def ensurePluginCompilesSnippetsToSameByteCode(className: String, sourcesAndReferences: Traversable[(String, String)]): Unit = {
    def flatten(s: Traversable[String]) = s.map("{\n" + _ + "\n};").mkString("\n")
    ensurePluginCompilesSnippetsToSameByteCode(className, flatten(sourcesAndReferences.map(_._1)), flatten(sourcesAndReferences.map(_._2)))
  }
  def ensurePluginCompilesSnippetsToSameByteCode(className: String, source: String, reference: String) = {

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

    val withPluginFut = futEx { getSnippetBytecode(className, source, "withPlugin", SharedCompilerWithPlugins) }
    val expected = getSnippetBytecode(className, reference, "expected", SharedCompilerWithoutPlugins)
    val withoutPlugin = getSnippetBytecode(className, source, "withoutPlugin", SharedCompilerWithoutPlugins)
    val withPlugin = withPluginFut()

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

  def fail(msg: String) = {
    println(msg)
    error(msg)
  }
  def ensureFasterCodeWithSameResult(code: String, fasterFactor: Float, params: Seq[Int] = Array(10, 100000), nRuns: Int = 10) = {
    val packageName = "tests"

    val methodName = new RuntimeException().getStackTrace.filter(se => se.getClassName.endsWith("Test")).last.getMethodName
    //val methodName = name.replace(' ', '_')

    def test(withPlugin: Boolean) = {
      val className = "Test_" + methodName + "_" + (if (withPlugin) "_Optimized" else "_Normal")

      val src = "package " + packageName + "\nclass " + className + """ {
        def """ + methodName + """(n: Int) = {
        """ + code + """
        }
      }"""

      val outputDirectory = new File("tmpTestClasses")
      def del(dir: File): Unit = {
        val fs = dir.listFiles
        if (fs != null)
          fs foreach del
        
        dir.delete
      }

      del(outputDirectory)
      outputDirectory.mkdirs
      val loader = new URLClassLoader(Array(outputDirectory.toURI.toURL))

      compileSource(src, withPlugin, outputDirectory)
      val c = loader.loadClass(packageName + "." + className)
      val m = c.getMethod(methodName, classOf[Int])
      val i = c.newInstance
      val ret = for (param <- params) yield {
        def run = {
          System.gc
          Thread.sleep(50)
          val start = System.nanoTime
          val o = m.invoke(i, param.asInstanceOf[AnyRef])
          val time = System.nanoTime - start
          (o, time)
        }

        val o = run._1 // take first output
        var times = for (i <- 0 until nRuns) yield run._2 // skip first run, compute average on other runs
        (param, o, times.sum / times.size.toFloat)
      }
      del(outputDirectory)
      ret
    }
    def eq(a: AnyRef, b: AnyRef) = {
      if ((a == null) != (b == null))
        false
      else
        (a == null) || a.equals(b)
    }
    test(false).zip(test(true)).map {
      case ((param, normalOutput, normalTime), (_, optimizedOutput, optimizedTime)) =>
        val pref = "[" + methodName + ", n = " + param + "] "
        if (!eq(normalOutput, optimizedOutput)) {
          fail(pref + "Output is not the same !\n" + pref + "\t   Normal output = " + normalOutput + "\n" + pref + "\tOptimized output = " + optimizedOutput)
        }
        val actualFasterFactor = normalTime / optimizedTime.toFloat
        if (actualFasterFactor < fasterFactor)
          fail(pref + "Expected optimized code to be at least " + fasterFactor + "x faster, but it is only " + actualFasterFactor + "x faster !")

        println(pref + "Optimized code is " + actualFasterFactor + "x faster ! (expected at least " + fasterFactor + "x factor)")
    }
    println()
  }
  def compileSource(src: String, withPlugin: Boolean, outputDirectory: File) {
    var tmpFile = File.createTempFile("test", ".scala")
    val pout = new PrintStream(tmpFile)
    pout.println(src)
    pout.close

    (if (withPlugin) SharedCompilerWithPlugins else SharedCompilerWithoutPlugins).compile(Array("-d", outputDirectory.getAbsolutePath, tmpFile.getAbsolutePath))
    //tmpFile.deleteOnExit
    /*val p = Runtime.getRuntime.exec(
      Array(
        """d:\Program Files\scala-2.8.0.final\bin\scalac.bat""",
        //"scalac",
        "-d", outputDirectory.toString,
        tmpFile.toString
      ) ++ (
        if (withPlugin)
          Array("-Xplugin:scalacl-compiler-plugin-1.0-SNAPSHOT.jar")
        else
          Array[String]()
      )
    )
    def recopy(in: InputStream) = scala.concurrent.ops.spawn {
      var line = ""
      var rin = new BufferedReader(new InputStreamReader(in))
      while ({ line = rin.readLine; line != null })
        println(line)
    }
    recopy(p.getInputStream)
    recopy(p.getErrorStream)
    assert(p.waitFor == 0)
    */
  }

}
