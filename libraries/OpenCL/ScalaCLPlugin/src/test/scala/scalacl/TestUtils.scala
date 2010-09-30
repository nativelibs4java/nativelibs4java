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
    
  //def testSameByteCode(originalSource: String)
  //var nextSnippetId = 0
  def getSnippetBytecode(source: String, enablePlugin: Boolean)(implicit outDir: File) = {
    //val normalizedClassName = "Snippet"
    //val className = normalizedClassName + "$" + nextSnippetId + "$"
    val className = "Snippet"
    //nextSnippetId += 1
    val src = "class " + className + " { def invoke(): Unit = {\n" + source + "\n}}"
    val srcFile = new File(outDir, className + ".scala")
    val out = new PrintWriter(srcFile)
    out.println(src)
    out.close
    new File(outDir, className + ".class").delete
    Compile.compilerMain(Array("-d", outDir.getAbsolutePath, srcFile.getAbsolutePath), enablePlugin)
    val byteCodeSource = getClassByteCode(className, outDir.getAbsolutePath)
    println("COMPILED :")
    println("\t" + source.replaceAll("\n", "\n\t"))
    println("BYTECODE :")
    val byteCode = byteCodeSource.mkString//("\n")
    println("\t" + byteCode.replaceAll("\n", "\n\t"))
    byteCode
  }
  def ensureSourceIsTransformedToExpectedByteCode(source: String, reference: String)(implicit outDir: File) = {
    val expected = getSnippetBytecode(reference, false)
    val withoutPlugin = getSnippetBytecode(source, false)
    val withPlugin = getSnippetBytecode(source, true)

    assertTrue("Expected result already found without any plugin !!! (was the Scala compiler improved ?)", expected != withoutPlugin)
    assertEquals(expected, withPlugin)
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
