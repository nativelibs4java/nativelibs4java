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

trait TestUtils {

  implicit val outDir = new File("target/testSnippetsClasses")
  outDir.mkdirs

  def getSnippetBytecode(className: String, source: String, enablePlugin: Boolean, spawnUniqueCompilerInstance: Boolean) = {
    val src = "class " + className + " { def invoke(): Unit = {\n" + source + "\n}}"
    val srcFile = new File(outDir, className + ".scala")
    val out = new PrintWriter(srcFile)
    out.println(src)
    out.close
    new File(outDir, className + ".class").delete

    (
      if (enablePlugin)
        SharedCompilerWithPlugins
      else
        SharedCompilerWithoutPlugins
    ).compile(spawnUniqueCompilerInstance, Array("-d", outDir.getAbsolutePath, srcFile.getAbsolutePath))
    
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
    def run(spawnUniqueCompilerInstance: Boolean) = {
      val expected = getSnippetBytecode(className, reference, false, spawnUniqueCompilerInstance)
      val withoutPlugin = getSnippetBytecode(className, source, false, spawnUniqueCompilerInstance)
      val withPlugin = getSnippetBytecode(className, source, true, spawnUniqueCompilerInstance)

      assertTrue("Expected result already found without any plugin !!! (was the Scala compiler improved ?)", expected != withoutPlugin)
      if (expected != withPlugin) {
        def trans(tit: String, s: String) =
          println(tit + " :\n\t" + s.replaceAll("\n", "\n\t"))

        trans("EXPECTED", expected)
        trans("FOUND", withPlugin)

        assertEquals(expected, withPlugin)
      }
    }
    try {
      run(false)
    } catch {
      case ex =>
        println("Shared compiler threw an exception, spawning a new one.")
        //ex.printStackTrace
        run(true)
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
