package scalacl

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import scala.concurrent.ops
import scala.io.Source

import org.junit.Assert._

trait TestUtils {

  implicit val outDir = new File("target/testSnippetsClasses")
  outDir.mkdirs

  def getSnippetBytecode(className: String, source: String, enablePlugin: Boolean) = {
    val src = "class " + className + " { def invoke(): Unit = {\n" + source + "\n}}"
    val srcFile = new File(outDir, className + ".scala")
    val out = new PrintWriter(srcFile)
    out.println(src)
    out.close
    new File(outDir, className + ".class").delete
    Compile.compilerMain(Array("-d", outDir.getAbsolutePath, srcFile.getAbsolutePath), enablePlugin)
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
    val expected = getSnippetBytecode(className, reference, false)
    val withoutPlugin = getSnippetBytecode(className, source, false)
    val withPlugin = getSnippetBytecode(className, source, true)

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
      ignoring(classOf[IOException]) {
        while ({ str = inputStream.readLine; str != null }) {
          err.synchronized {
            println(str)
            err.append(str).append("\n")
          }
        }
      }
    }

    val out = Source.fromInputStream(p.getInputStream)
    if (p.waitFor != 0) {
      Thread.sleep(100)
      error("javap failed with :\n" + err.synchronized { err.toString } + "\nAnd :\n" + out)
    }
    out
  }   
}
