import com.nativelibs4java.opencl.CLMem
package object scalacl {
  import impl._

  //  val Input = CLMem.Usage.Input
  //  val Output = CLMem.Usage.Output
  //  val InputOutput = CLMem.Usage.InputOutput

  implicit val intDataIO = IntDataIO
  implicit val booleanDataIO = BooleanDataIO
  implicit def tuple2DataIO[T1 : Manifest : DataIO, T2 : Manifest : DataIO] = {
    new Tuple2DataIO[T1, T2]
  }
}