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
    byteCode//.replaceAll("#\\d+", "")
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
    def futEx[V](b: => V): () => V = {
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
}
