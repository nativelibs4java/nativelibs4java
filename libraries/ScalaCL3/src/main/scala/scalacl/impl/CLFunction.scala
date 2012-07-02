package scalacl.impl

import java.util.concurrent.atomic.AtomicLong
import scala.collection.mutable.ArrayBuffer
import com.nativelibs4java.opencl.CLQueue
import scalacl.CLArray

case class Captures(
  inputs: Array[CLArray[_]] = Array(),
  outputs: Array[CLArray[_]] = Array(),
  constants: Array[AnyRef] = Array())

case class CLFunction[U, V](f: U => V, kernel: Kernel, captures: Captures = Captures())
  extends Function1[U, V] {

  def apply(u: U) = f(u)

  def apply(queue: CLQueue, params: KernelExecutionParameters, input: CLArray[U], output: CLArray[V]) = {
    ScheduledData.schedule(
      if (input == null) captures.inputs else captures.inputs :+ input,
      if (output == null) captures.outputs else captures.outputs :+ output,
      eventsToWaitFor => {
        val args = new ArrayBuffer[AnyRef]
        
        input.foreachBuffer(args += _.buffer)
        captures.inputs.foreach(_.foreachBuffer(args += _.buffer))
        output.foreachBuffer(args += _.buffer)
        captures.outputs.foreach(_.foreachBuffer(args += _.buffer))
        args ++= captures.constants
        kernel.enqueue(queue, params, args.toArray, eventsToWaitFor)
      })

  }
}