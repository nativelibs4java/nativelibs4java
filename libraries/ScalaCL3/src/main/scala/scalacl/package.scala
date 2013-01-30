import com.nativelibs4java.opencl.CLMem

import language.experimental.macros

package object scalacl {
  import impl._

  implicit val intDataIO = IntDataIO
  implicit val floatDataIO = FloatDataIO
  implicit val booleanDataIO = BooleanDataIO
  implicit def tuple2DataIO[T1 : Manifest : DataIO, T2 : Manifest : DataIO] = {
    new Tuple2DataIO[T1, T2]
  }
  
  implicit class ArrayConversions[A : Manifest : DataIO](array: Array[A])(implicit context: Context) {
    def toCLArray = CLArray[A](array: _*)
    def cl = toCLArray
  }
  
  def kernel(block: Unit)(implicit context: Context): Unit = macro KernelMacros.kernelImpl
  
  def task(block: Unit)(implicit context: Context): Unit = macro KernelMacros.taskImpl
}