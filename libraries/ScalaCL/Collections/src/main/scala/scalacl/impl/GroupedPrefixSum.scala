package scalacl
package impl

// Ported from http://developer.apple.com/library/mac/#samplecode/OpenCL_Parallel_Prefix_Sum_Example/
import scala.collection._
import com.nativelibs4java.opencl._
import scala.math._
import org.bridj._
import org.bridj.Pointer._
import com.nativelibs4java.util.IOUtils.readText
import scala.collection.JavaConversions._

class GroupedPrefixSum[A](
  implicit val context: Context,
  val dataIO: CLDataIO[A]
) {
  val source: String = readText(Platform.getClassLoader(classOf[GroupedPrefixSum[_]]).getResourceAsStream("scalacl/impl/scan_kernel.cl"))
  val devices = context.getDevices
  val program = context.createProgram(source)
  program.defineMacro("DATA_TYPE", dataIO.clType)
  
  val kernels @ Array(
    preScanKernel,
    preScanStoreSumKernel,
    preScanStoreSumNonPowerOfTwoKernel,
    preScanNonPowerOfTwoKernel,
    uniformAddKernel
  ) = Array(
    "PreScanKernel",
    "PreScanStoreSumKernel",
    "PreScanStoreSumNonPowerOfTwoKernel",
    "PreScanNonPowerOfTwoKernel",
    "UniformAddKernel"
  ).map(n => program.createKernel(n))
  
  val GROUP_SIZE = (Array(256) ++ kernels.flatMap(_.getWorkGroupSize.values.map(_.longValue.toInt)) ++ devices.map(_.getMaxWorkGroupSize.toInt)).min
  //println("GROUP_SIZE = " + GROUP_SIZE)
  
  val dataSize = dataIO.elementSize
  val dataClass = dataIO.t.erasure.asInstanceOf[Class[A]]
  val NUM_BANKS = (16)

  private def IsPowerOfTwo(n: Int) =
    ((n & (n - 1)) == 0)

    /*
  private def floorPow2(n: Int) = {
    var v = n
    var ret = 0
    while (v != 0) {
      v = v >> 1
      ret += 1
    }
    ret
  } //*/
  //*
  // http://graphics.stanford.edu/~seander/bithacks.html#IntegerLogObvious
  private def floorLog2(n: Int) = {
    var x = n
    var ret = 0
    if ((x & 0xffff0000) != 0) { ret += 16; x >>= 16; }
    if ((x & 0x0000ff00) != 0) { ret +=  8; x >>=  8; }
    if ((x & 0x000000f0) != 0) { ret +=  4; x >>=  4; }
    if ((x & 0x0000000c) != 0) { ret +=  2; x >>=  2; }
    if ((x & 0x00000002) != 0) { ret +=  1; }
    ret
  }
  private def floorPow2(n: Int) = 
    1 << floorLog2(n)
  //*/

  @inline private def ceilFunc(elementCount: Int, groupSize: Int) =
    max(1, ceil(elementCount / (2.0f * groupSize)).toInt)
  
  private def CreatePartialSumBuffers(count: Int) = {
    var group_size = GROUP_SIZE
    var element_count = count

    var level = 0

    do {
      val group_count = ceilFunc(element_count, group_size)
      if (group_count > 1) {
        level += 1
      }
      element_count = group_count

    } while (element_count > 1)

    val ScanPartialSums = new Array[CLBuffer[A]](level)

    element_count = count
    level = 0

    do {
      val group_count = ceilFunc(element_count, group_size)
      if (group_count > 1) {
        ScanPartialSums(level) = context.createBuffer(CLMem.Usage.InputOutput, dataClass, group_count)
        level += 1
      }

      element_count = group_count

    } while (element_count > 1)

    ScanPartialSums
  }

  private def PreScanBufferRecursive(
    ScanPartialSums: Array[CLBuffer[A]],
    output_data: CLBuffer[A],
    input_data: CLBuffer[A],
    max_group_size: Int,
    max_work_item_count: Int,
    element_count: Int,
    level: Int): Unit =
  {
    val group_size = max_group_size
    val group_count = ceilFunc(element_count, group_size)
    var work_item_count = 0

    if (group_count > 1)
      work_item_count = group_size
    else if (IsPowerOfTwo(element_count))
      work_item_count = element_count / 2
    else
      work_item_count = floorPow2(element_count)

    work_item_count = if (work_item_count > max_work_item_count) max_work_item_count else work_item_count

    val element_count_per_group = work_item_count * 2
    val last_group_element_count = element_count - (group_count - 1) * element_count_per_group
    var remaining_work_item_count = max(1, last_group_element_count / 2)
    remaining_work_item_count = if (remaining_work_item_count > max_work_item_count) max_work_item_count else remaining_work_item_count
    var remainder = 0
    var last_shared = 0L


    if (last_group_element_count != element_count_per_group) {
      remainder = 1

      if (!IsPowerOfTwo(last_group_element_count))
        remaining_work_item_count = floorPow2(last_group_element_count)

      remaining_work_item_count = if (remaining_work_item_count > max_work_item_count) max_work_item_count else remaining_work_item_count
      val padding = (2 * remaining_work_item_count) / NUM_BANKS
      last_shared = dataSize * (2 * remaining_work_item_count + padding)
    }

    remaining_work_item_count = if (remaining_work_item_count > max_work_item_count) max_work_item_count else remaining_work_item_count
    val global = Array(max(1, group_count - remainder) * work_item_count, 1)
    val local = Array(work_item_count, 1)

    val padding = element_count_per_group / NUM_BANKS
    val shared = dataSize * (element_count_per_group + padding)

    val partial_sums = if (level >= ScanPartialSums.size) null else ScanPartialSums(level)

    if (group_count > 1) {
      preScanStoreSumKernel.enqueueNDRange(global, local)(
        output_data,
        input_data,
        partial_sums,
        new LocalSize(shared),
        0, //group_index
        0, //base_index
        work_item_count * 2
      )

      if (remainder != 0) {
        val last_global = Array(1 * remaining_work_item_count, 1)
        val last_local = Array(remaining_work_item_count, 1)

        preScanStoreSumNonPowerOfTwoKernel.enqueueNDRange(last_global, last_local)(
          output_data, 
          input_data, 
          partial_sums, 
          new LocalSize(last_shared), 
          group_count - 1, // group_index, 
          element_count - last_group_element_count, // base_index, 
          last_group_element_count
        )
      }

      PreScanBufferRecursive(ScanPartialSums, partial_sums, partial_sums, max_group_size, max_work_item_count, group_count, level + 1)

      uniformAddKernel.enqueueNDRange(global, local)(
        output_data,
        partial_sums,
        new LocalSize(dataSize),
        0, //group_offset
        0, //base_index
        element_count - last_group_element_count
      )

      if (remainder != 0) {
        val last_global = Array(1 * remaining_work_item_count, 1)
        val last_local = Array(remaining_work_item_count, 1)

        uniformAddKernel.enqueueNDRange(last_global, last_local)(
          output_data,
          partial_sums,
          new LocalSize(dataSize),
          group_count - 1, //group_offset
          element_count - last_group_element_count, //base_index
          last_group_element_count
        )
      }
    }
    else if (IsPowerOfTwo(element_count)) {
      preScanKernel.enqueueNDRange(global, local)(
        output_data,
        input_data,
        new LocalSize(shared),
        0, //group_index
        0, //base_index
        work_item_count * 2
      )
    }
    else {
      preScanNonPowerOfTwoKernel.enqueueNDRange(global, local)(
        output_data,
        input_data,
        new LocalSize(shared),
        0, //group_index
        0, //base_index
        element_count
      )
    }
  }

  def prefixSum(input_buffer: CLBuffer[A], evtsToWaitFor: CLEvent*): (CLBuffer[A], CLEvent) =
  {
    val count = input_buffer.getElementCount.toInt
    val output_buffer = context.createBuffer(CLMem.Usage.InputOutput, dataClass, count)
    (output_buffer, prefixSum(input_buffer, output_buffer, evtsToWaitFor:_*))
  }
  def prefixSum(input_buffer: CLBuffer[A], output_buffer: CLBuffer[A], evtsToWaitFor: CLEvent*): CLEvent =
  {
    assert(input_buffer != null, "null input buffer")
    assert(output_buffer != null, "null output buffer")
    
    val count = input_buffer.getElementCount.toInt
    assert(output_buffer.getElementCount == count, "invalid output buffer size (expected " + count + ", got " + output_buffer.getElementCount + ")")
    
    if (context.queue.isOutOfOrder)
      CLEvent.waitFor(evtsToWaitFor:_*) // TODO use these events
    
    // TODO remove this
    //output_buffer.write(context.queue, allocateArray(dataClass, count), true)

    var ScanPartialSums = CreatePartialSumBuffers(count)

    PreScanBufferRecursive(
      ScanPartialSums,
      output_buffer,
      input_buffer,
      GROUP_SIZE,
      GROUP_SIZE,
      count,
      0
    )

    ScanPartialSums.map(_.release)

    if (context.queue.isOutOfOrder)
      context.queue.finish // TODO return events !
    null
  }
}

object GroupedPrefixSum {

  private val cache = new mutable.HashMap[(Context, Class[_]), GroupedPrefixSum[_]]
  def apply[A](implicit context: Context, dataIO: CLDataIO[A]) = cache synchronized {
    cache.getOrElseUpdate((context, dataIO.t.erasure), new GroupedPrefixSum[A]).asInstanceOf[GroupedPrefixSum[A]]
  }
/*
  def main(args: Array[String]) {
    implicit val context = Context.best(CPU)
    val n = 10
    val inputValues = allocateInts(n).as(classOf[Int])
    for (i <- 0 until n)
      inputValues(i) = i

    val scanner = GroupedPrefixSum[Int]
    val inputBuffer = context.createBuffer(CLMem.Usage.InputOutput, inputValues, true)
    val (outputBuffer, evt) = scanner.prefixSum(inputBuffer)
    CLEvent.waitFor(evt)

    val expectedValues = (0 until n).scanLeft(0)(_ + _)
    val outputValues = outputBuffer.read(context.queue)
    for (i <- 0 until n)
      println("At " + i + ", expected " + expectedValues(i) + ", got " + outputValues(i))
  }
*/
}