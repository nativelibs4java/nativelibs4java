package scalacl ; package test
import plugin._
import com.nativelibs4java.scalaxy._
import test._
import pluginBase._
import components._

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

trait ScalaCLTestUtils extends BaseTestUtils {
  def pluginDef: PluginDef =
    ScalaCLPluginDef
    
  override def getAdditionalClassPath = {
    def getPath(c: Class[_]): String = {
      c.getProtectionDomain.getCodeSource.getLocation.getFile
    }
    Some(Set(
      getPath(classOf[CLArray[_]]),
      getPath(classOf[com.nativelibs4java.opencl.CLBuffer[_]]),
      getPath(classOf[com.nativelibs4java.opencl.library.OpenCLLibrary]),
      getPath(classOf[org.bridj.BridJ]),
      getPath(classOf[org.bridj.relocated.org.objectweb.asm.ClassVisitor])
    ).toSeq)
  }
}
