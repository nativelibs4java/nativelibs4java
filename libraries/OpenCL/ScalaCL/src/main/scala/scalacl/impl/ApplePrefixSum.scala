package scalacl
package impl

// Ported from http://developer.apple.com/library/mac/#samplecode/OpenCL_Parallel_Prefix_Sum_Example/
//
// File:       scan.c
//
// Abstract:   This example shows how to perform an efficient parallel prefix sum (aka Scan)
//             using OpenCL.  Scan is a common data parallel primitive which can be used for
//             variety of different operations -- this example uses local memory for storing
//             partial sums and avoids memory bank conflicts on architectures which serialize
//             memory operations that are serviced on the same memory bank by offsetting the
//             loads and stores based on the size of the local group and the number of
//             memory banks (see appropriate macro definition).  As a result, this example
//             requires that the local group size > 1.
//
// Version:    <1.0>
//
// Disclaimer: IMPORTANT:  This Apple software is supplied to you by Apple Inc. ("Apple")
//             in consideration of your agreement to the following terms, and your use,
//             installation, modification or redistribution of this Apple software
//             constitutes acceptance of these terms.  If you do not agree with these
//             terms, please do not use, install, modify or redistribute this Apple
//             software.
//
//             In consideration of your agreement to abide by the following terms, and
//             subject to these terms, Apple grants you a personal, non - exclusive
//             license, under Apple's copyrights in this original Apple software ( the
//             "Apple Software" ), to use, reproduce, modify and redistribute the Apple
//             Software, with or without modifications, in source and / or binary forms;
//             provided that if you redistribute the Apple Software in its entirety and
//             without modifications, you must retain this notice and the following text
//             and disclaimers in all such redistributions of the Apple Software. Neither
//             the name, trademarks, service marks or logos of Apple Inc. may be used to
//             endorse or promote products derived from the Apple Software without specific
//             prior written permission from Apple.  Except as expressly stated in this
//             notice, no other rights or licenses, express or implied, are granted by
//             Apple herein, including but not limited to any patent rights that may be
//             infringed by your derivative works or by other works in which the Apple
//             Software may be incorporated.
//
//             The Apple Software is provided by Apple on an "AS IS" basis.  APPLE MAKES NO
//             WARRANTIES, EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION THE IMPLIED
//             WARRANTIES OF NON - INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A
//             PARTICULAR PURPOSE, REGARDING THE APPLE SOFTWARE OR ITS USE AND OPERATION
//             ALONE OR IN COMBINATION WITH YOUR PRODUCTS.
//
//             IN NO EVENT SHALL APPLE BE LIABLE FOR ANY SPECIAL, INDIRECT, INCIDENTAL OR
//             CONSEQUENTIAL DAMAGES ( INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
//             SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
//             INTERRUPTION ) ARISING IN ANY WAY OUT OF THE USE, REPRODUCTION, MODIFICATION
//             AND / OR DISTRIBUTION OF THE APPLE SOFTWARE, HOWEVER CAUSED AND WHETHER
//             UNDER THEORY OF CONTRACT, TORT ( INCLUDING NEGLIGENCE ), STRICT LIABILITY OR
//             OTHERWISE, EVEN IF APPLE HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
// Copyright ( C ) 2008 Apple Inc. All Rights Reserved.
//
////////////////////////////////////////////////////////////////////////////////////////////////////
import com.nativelibs4java.opencl._
import scala.math._
import org.bridj._
import org.bridj.Pointer._

object ApplePrefixSum {

  def printf(fmt: String, args: Any*) = System.out.printf(fmt, args.map(_.asInstanceOf[Object]): _*)

  ////////////////////////////////////////////////////////////////////////////////////////////////////

  val dataSize = 4

  val DEBUG_INFO = (0)
  var GROUP_SIZE = 256
  val NUM_BANKS = (16)
  val MAX_ERROR = (1e-7)
  val SEPARATOR = ("----------------------------------------------------------------------\n")

  val iterations = 1000
  val count      = 1024 * 1024

  ////////////////////////////////////////////////////////////////////////////////////////////////////

  var ComputeDeviceId: CLDevice = _
  var ComputeCommands: CLQueue = _
  var ComputeContext: CLContext = _
  var ComputeProgram: CLProgram = _
  var ComputeKernels: Array[CLKernel] = _
  var ScanPartialSums: Array[CLBuffer[java.lang.Float]] = _
  var ElementsAllocated = 0
  var LevelsAllocated = 0

  ////////////////////////////////////////////////////////////////////////////////////////////////////

  object KernelMethods {
    val PRESCAN = 0
    val PRESCAN_STORE_SUM = 1
    val PRESCAN_STORE_SUM_NON_POWER_OF_TWO = 2
    val PRESCAN_NON_POWER_OF_TWO = 3
    val UNIFORM_ADD = 4
  }

  import KernelMethods._

  val KernelNames = Array(
    "PreScanKernel",
    "PreScanStoreSumKernel",
    "PreScanStoreSumNonPowerOfTwoKernel",
    "PreScanNonPowerOfTwoKernel",
    "UniformAddKernel"
  )

  val KernelCount = KernelNames.size

  ////////////////////////////////////////////////////////////////////////////////////////////////////

  def IsPowerOfTwo(n: Int) =
    ((n & (n - 1)) == 0)

  def floorPow2(n: Int) = {
    var v = n
    var ret = 0
    while (v != 0) {
      v = v >> 1
      ret += 1
    }
    ret
  }


  ////////////////////////////////////////////////////////////////////////////////////////////////////

  def
  CreatePartialSumBuffers(count: Int) {
    ElementsAllocated = count

    var group_size = GROUP_SIZE
    var element_count = count

    var level = 0

    do {
      val group_count = max(1, ceil(element_count.toFloat / (2.0f * group_size)).toInt)
      if (group_count > 1) {
        level += 1
      }
      element_count = group_count

    } while (element_count > 1)

    ScanPartialSums = new Array[CLBuffer[java.lang.Float]](level)
    LevelsAllocated = level

    element_count = count
    level = 0

    do {
      var group_count = max(1, ceil(element_count / (2.0f * group_size)).toInt)
      if (group_count > 1) {
        ScanPartialSums(level) = ComputeContext.createBuffer(CLMem.Usage.InputOutput, classOf[java.lang.Float], group_count)
        level += 1
      }

      element_count = group_count

    } while (element_count > 1)
  }

  def
  ReleasePartialSums {
    ScanPartialSums.map(_.release)
    ScanPartialSums = null
    ElementsAllocated = 0
    LevelsAllocated = 0
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////

  def
  PreScan(
           global: Array[Int],
           local: Array[Int],
           shared: Long,
           output_data: CLBuffer[java.lang.Float],
           input_data: CLBuffer[java.lang.Float],
           n: Int,
           group_index: Int,
           base_index: Int) {
    /*
    printf("PreScan: Global[%4d] Local[%4d] Shared[%4d] BlockIndex[%4d] BaseIndex[%4d] Entries[%d]\n",
      global(0).toInt, local(0).toInt, shared.toInt, group_index, base_index, n)
    */
    var k = ComputeKernels(PRESCAN)

    k.setArgs(output_data, input_data, new LocalSize(shared), group_index, base_index, n)
    k.enqueueNDRange(ComputeCommands, global, local)
  }

  def
  PreScanStoreSum(
                   global: Array[Int],
                   local: Array[Int],
                   shared: Long,
                   output_data: CLBuffer[java.lang.Float],
                   input_data: CLBuffer[java.lang.Float],
                   partial_sums: CLBuffer[java.lang.Float],
                   n: Int,
                   group_index: Int,
                   base_index: Int) {
    /*
    printf("PreScan: Global[%4d] Local[%4d] Shared[%4d] BlockIndex[%4d] BaseIndex[%4d] Entries[%d]\n",
      global(0).toInt, local(0).toInt, shared.toInt, group_index, base_index, n)
    */
    var k = ComputeKernels(PRESCAN_STORE_SUM)

    k.setArgs(output_data, input_data, partial_sums, new LocalSize(shared), group_index, base_index, n)
    k.enqueueNDRange(ComputeCommands, global, local)
  }

  def
  PreScanStoreSumNonPowerOfTwo(
                                global: Array[Int],
                                local: Array[Int],
                                shared: Long,
                                output_data: CLBuffer[java.lang.Float],
                                input_data: CLBuffer[java.lang.Float],
                                partial_sums: CLBuffer[java.lang.Float],
                                n: Int,
                                group_index: Int,
                                base_index: Int) {
    /*
    printf("PreScanStoreSumNonPowerOfTwo: Global[%4d] Local[%4d] BlockIndex[%4d] BaseIndex[%4d] Entries[%d]\n",
      global(0).toInt, local(0).toInt, shared.toInt, group_index, base_index, n)
    */
    var k = ComputeKernels(PRESCAN_STORE_SUM_NON_POWER_OF_TWO)
    k.setArgs(output_data, input_data, partial_sums, new LocalSize(shared), group_index, base_index, n)
    k.enqueueNDRange(ComputeCommands, global, local)
  }

  def
  PreScanNonPowerOfTwo(
                        global: Array[Int],
                        local: Array[Int],
                        shared: Long,
                        output_data: CLBuffer[java.lang.Float],
                        input_data: CLBuffer[java.lang.Float],
                        n: Int,
                        group_index: Int,
                        base_index: Int) {
    /*
    printf("PreScanNonPowerOfTwo: Global[%4d] Local[%4d] BlockIndex[%4d] BaseIndex[%4d] Entries[%d]\n",
      global(0).toInt, local(0).toInt, shared.toInt, group_index, base_index, n)
    */
    var k = ComputeKernels(PRESCAN_NON_POWER_OF_TWO)
    k.setArgs(output_data, input_data, new LocalSize(shared), group_index, base_index, n)
    k.enqueueNDRange(ComputeCommands, global, local)
  }

  def
  UniformAdd(
              global: Array[Int],
              local: Array[Int],
              output_data: CLBuffer[java.lang.Float],
              partial_sums: CLBuffer[java.lang.Float],
              n: Int,
              group_offset: Int,
              base_index: Int) {
    /*
    printf("UniformAdd: Global[%4d] Local[%4d] BlockOffset[%4d] BaseIndex[%4d] Entries[%d]\n",
      global(0).toInt, local(0).toInt, group_offset, base_index, n)
    */
    var k = ComputeKernels(UNIFORM_ADD)

    k.setArgs(output_data, partial_sums, new LocalSize(dataSize), group_offset, base_index, n)
    k.enqueueNDRange(ComputeCommands, global, local)
  }


  def
  PreScanBufferRecursive(
                          output_data: CLBuffer[java.lang.Float],
                          input_data: CLBuffer[java.lang.Float],
                          max_group_size: Int,
                          max_work_item_count: Int,
                          element_count: Int,
                          level: Int) {
    var group_size = max_group_size
    var group_count = max(1, ceil(element_count / (2.0f * group_size)).toInt)
    var work_item_count = 0

    if (group_count > 1)
      work_item_count = group_size
    else if (IsPowerOfTwo(element_count))
      work_item_count = element_count / 2
    else
      work_item_count = floorPow2(element_count)

    work_item_count = if (work_item_count > max_work_item_count) max_work_item_count else work_item_count

    var element_count_per_group = work_item_count * 2
    var last_group_element_count = element_count - (group_count - 1) * element_count_per_group
    var remaining_work_item_count = max(1, last_group_element_count / 2)
    remaining_work_item_count = if (remaining_work_item_count > max_work_item_count) max_work_item_count else remaining_work_item_count
    var remainder = 0
    var last_shared = 0L


    if (last_group_element_count != element_count_per_group) {
      remainder = 1

      if (!IsPowerOfTwo(last_group_element_count))
        remaining_work_item_count = floorPow2(last_group_element_count)

      remaining_work_item_count = if (remaining_work_item_count > max_work_item_count) max_work_item_count else remaining_work_item_count
      var padding = (2 * remaining_work_item_count) / NUM_BANKS
      last_shared = 4 /*sizeof(float)*/ * (2 * remaining_work_item_count + padding)
    }

    remaining_work_item_count = if (remaining_work_item_count > max_work_item_count) max_work_item_count else remaining_work_item_count
    val global = Array(max(1, group_count - remainder) * work_item_count, 1)
    val local = Array(work_item_count, 1)

    var padding = element_count_per_group / NUM_BANKS
    var shared = 4L /*sizeof(float)*/ * (element_count_per_group + padding)

    var partial_sums = if (level >= LevelsAllocated) null else ScanPartialSums(level)

    if (group_count > 1) {
      PreScanStoreSum(global, local, shared, output_data, input_data, partial_sums, work_item_count * 2, 0, 0)

      if (remainder != 0) {
        var last_global = Array(1 * remaining_work_item_count, 1)
        var last_local = Array(remaining_work_item_count, 1)

        PreScanStoreSumNonPowerOfTwo(
          last_global, last_local, last_shared,
          output_data, input_data, partial_sums,
          last_group_element_count,
          group_count - 1,
          element_count - last_group_element_count)
      }

      PreScanBufferRecursive(partial_sums, partial_sums, max_group_size, max_work_item_count, group_count, level + 1)
      UniformAdd(global, local, output_data, partial_sums, element_count - last_group_element_count, 0, 0)

      if (remainder != 0) {
        var last_global = Array(1 * remaining_work_item_count, 1)
        var last_local = Array(remaining_work_item_count, 1)

        UniformAdd(
          last_global, last_local,
          output_data, partial_sums,
          last_group_element_count,
          group_count - 1,
          element_count - last_group_element_count)
      }
    }
    else if (IsPowerOfTwo(element_count)) {
      PreScan(global, local, shared, output_data, input_data, work_item_count * 2, 0, 0)
    }
    else {
      PreScanNonPowerOfTwo(global, local, shared, output_data, input_data, element_count, 0, 0)
    }
  }

  def
  PreScanBuffer(
                 output_data: CLBuffer[java.lang.Float],
                 input_data: CLBuffer[java.lang.Float],
                 max_group_size: Int,
                 max_work_item_count: Int,
                 element_count: Int) {
    PreScanBufferRecursive(output_data, input_data, max_group_size, max_work_item_count, element_count, 0)
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////

  def ScanReference(reference: Pointer[java.lang.Float], input: Pointer[java.lang.Float], count: Int) {
    reference(0) = 0
    var total_sum = 0.0

    for (i <- 1 until count) {
      total_sum += input(i - 1).floatValue
      reference(i) = input(i - 1).floatValue + reference(i - 1).floatValue
    }
    if (total_sum != reference(count - 1))
      printf("Warning: Exceeding single-precision accuracy.  Scan will be inaccurate.\n")
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////

  def main2(args: Array[String]): Unit = {
    var t0 = 0L
    var t1 = 0L
    var t2 = 0L
    var output_buffer: CLBuffer[java.lang.Float] = null
    var input_buffer: CLBuffer[java.lang.Float] = null

    // Create some random input data on the host
    //
    val rand = new java.util.Random(System.nanoTime)
    var float_data = allocateFloats(count)
    for (i <- 0 until count) {
      float_data(i) = (10 * rand.nextFloat).toInt
    }

    // Connect to a GPU compute device
    //

    val source: String = com.nativelibs4java.util.IOUtils.readText(Platform.getClassLoader(classOf[GroupedPrefixSum[_]]).getResourceAsStream("scalacl/impl/scan_kernel.cl"))

    // Create a compute ComputeContext
    //
    ComputeContext = JavaCL.createBestContext(CLPlatform.DeviceFeature.GPU)
    ComputeDeviceId = ComputeContext.getDevices()(0)
    var max_workgroup_size = ComputeDeviceId.getMaxWorkGroupSize

    GROUP_SIZE = min(GROUP_SIZE, max_workgroup_size.toInt)

    ComputeCommands = ComputeContext.createDefaultQueue()

    // Create the compute program from the source buffer
    //
    ComputeProgram = ComputeContext.createProgram(source).build

    ComputeKernels = KernelNames.map(n => ComputeProgram.createKernel(n))
    var wgSize = ComputeKernels.map(_.getWorkGroupSize.get(ComputeDeviceId).longValue.toInt).min
    GROUP_SIZE = min(GROUP_SIZE, wgSize.toInt)

    // Create the input buffer on the device
    //
    //val buffer_size = sizeof(float) * count
    input_buffer = ComputeContext.createBuffer(CLMem.Usage.InputOutput, classOf[java.lang.Float], count)

    // Fill the input buffer with the host allocated random data
    //
    input_buffer.write(ComputeCommands, float_data, true)

    // Create the output buffer on the device
    //
    output_buffer = ComputeContext.createBuffer(CLMem.Usage.InputOutput, classOf[java.lang.Float], count)

    val result = allocateFloats(count)
    output_buffer.write(ComputeCommands, result, true)

    CreatePartialSumBuffers(count)
    PreScanBuffer(output_buffer, input_buffer, GROUP_SIZE, GROUP_SIZE, count)

    printf("Starting timing run of '%d' iterations...\n", iterations)

    t1 = System.nanoTime
    t0 = t1
    for (i <- 0 until iterations) {
      PreScanBuffer(output_buffer, input_buffer, GROUP_SIZE, GROUP_SIZE, count)
    }
    ComputeCommands.finish
    t2 = System.nanoTime


    // Calculate the statistics for execution time and throughput
    //
    val t = (t2 - t1) / 1000000000.0
    printf("Exec Time:  %.2f ms\n", 1000.0 * t / iterations)
    printf("Throughput: %.2f GB/sec\n", 1e-9 * (count * dataSize) * iterations / t)
    printf(SEPARATOR)

    // Read back the results that were computed on the device
    //
    output_buffer.read(ComputeCommands, result, true)

    // Verify the results are correct
    //
    var reference = allocateFloats(count)
    ScanReference(reference, float_data, count)

    var errorValue = 0.0f
    var diff = 0.0f
    for (i <- 0 until count) {
      diff = abs(reference(i).floatValue - result(i).floatValue)
      errorValue = if (diff > errorValue) diff else errorValue
    }

    if (errorValue > MAX_ERROR) {
      error("Error:   Incorrect results obtained! Max error = " + errorValue)
    }
    else {
      printf("Results Validated!\n")
      printf(SEPARATOR)
    }

    // Shutdown and cleanup
    //
    ReleasePartialSums
    ComputeKernels.map(_.release)
    ComputeProgram.release
    input_buffer.release
    output_buffer.release
    ComputeCommands.release
    ComputeContext.release

    ComputeKernels = null
    float_data.release
    reference.release
    result.release
  }


}