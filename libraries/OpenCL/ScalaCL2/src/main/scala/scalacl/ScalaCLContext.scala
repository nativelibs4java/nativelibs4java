/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scalacl

import scala.collection.mutable.HashMap
import com.nativelibs4java.opencl._

class ScalaCLContext(val context: CLContext = JavaCL.createBestContext) {
  lazy val queue = context.createDefaultQueue()
  lazy val order = context.getKernelsDefaultByteOrder
  //var cache = new HashMap[CLFunction[_, _], CLKernel]
  //def getKernel(function: CLFunction[_, _]) = cache(function)
}
