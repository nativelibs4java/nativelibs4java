/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.scalaxy ; package test
import plugin._

object CompilerUtils {
  import javax.tools._
  import JavaFileManager._
  import scala.collection.mutable.HashMap
  import java.io._
  import java.net._
  
  class MemoryFileManager(compiler: JavaCompiler, diagnostics: DiagnosticCollector[JavaFileObject]) extends ForwardingJavaFileManager[JavaFileManager](compiler.getStandardFileManager(diagnostics, null, null)) {
    val inputs = new HashMap[String, MemoryJavaFile]()
    val outputs = new HashMap[String, MemoryFileObject]
    def addSourceInput(path: String, source: String) = {
      val pathURI = if (!path.startsWith("file:///"))
        "file:///" + path
      else
        path

      val f = new MemoryJavaFile(pathURI, source, JavaFileObject.Kind.SOURCE);
      inputs(pathURI) = f
      f
    }

    override def getJavaFileForInput(location: JavaFileManager.Location, className: String, kind: JavaFileObject.Kind) = {
      if (kind == JavaFileObject.Kind.SOURCE)
        inputs(className)
      else
        super.getJavaFileForInput(location, className, kind);
    }


    def getFullPathForClass(className: String, extension: String) =
      "file:///" + getSimplePathForClass(className, extension);

    def getSimplePathForClass(className: String, extension: String) =
      className.replace('.', '/') + "." + extension;

    override def getJavaFileForOutput(location: JavaFileManager.Location, className: String, kind: JavaFileObject.Kind, sibling: FileObject) = {
      var jo: MemoryJavaFile = null;
      if (kind == JavaFileObject.Kind.CLASS)
        outputs(getFullPathForClass(className, "class")) = { jo = new MemoryJavaFile(getFullPathForClass(className, "class"), null, kind); jo }
      else if (kind == JavaFileObject.Kind.SOURCE)
        inputs(getFullPathForClass(className, "java")) = { jo = new MemoryJavaFile(getFullPathForClass(className, "java"), null, kind); jo }

      if (jo == null)
        super.getJavaFileForInput(location, className, kind)
      else
        jo
    }
    override def getFileForOutput(location: JavaFileManager.Location, packageName: String, relativeName: String, sibling: FileObject) = {
      val actualRelativeName =
        if (relativeName.startsWith("file:///"))
          relativeName.substring("file:///".length())
      else
        relativeName

      val out = outputs.getOrElseUpdate(relativeName, new MemoryFileObject(relativeName, null.asInstanceOf[String]))
      out
    }
  }
  class MemoryFileObject(var path: String, var content: Array[Byte]) extends FileObject {
    def this(path: String, content: String) = this(path, content.getBytes)

    override def delete = {
      content = null
      true
    }
    override def getCharContent(ignoreEncodingErrors: Boolean) =
      new String(content)

    val lastModified = System.currentTimeMillis
    override def getLastModified =
      lastModified

    override def getName = path

    override def openInputStream = {
      if (content == null)
        null
      else
        new ByteArrayInputStream(content)
    }
    override def openOutputStream =
      new ByteArrayOutputStream() {
        override def close = {
          super.close
          content = toByteArray
        }
      }

    override def openReader(ignoreEncodingErrors: Boolean) = {
      val in = openInputStream
      if (in == null)
        null
      else
        new InputStreamReader(in)
    }
    override def openWriter = {
      val out = openOutputStream
      if (out == null)
        null
      else
        new OutputStreamWriter(out)
    }
    override def toUri =
      try {
        new URI(path);
      } catch {
        case ex =>
          ex.printStackTrace();
          null;
      }

    override def toString = path + ":\n" + getCharContent(true)
  }

  class MemoryJavaFile(path: String, content: String, kind: JavaFileObject.Kind) extends MemoryFileObject(path, content) with JavaFileObject {
    import javax.lang.model.element.Modifier
    override def getAccessLevel =
      Modifier.PUBLIC

    override def getKind =
      kind

    override def getNestingKind =
      null

    override def isNameCompatible(simpleName: String, kind: JavaFileObject.Kind) =
      true // TODO
  }
}
