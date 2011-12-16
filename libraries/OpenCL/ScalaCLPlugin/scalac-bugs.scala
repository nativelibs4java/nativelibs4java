class Test {
  val out = collection.mutable.ArrayBuffer[Int]()
  val item: Int = 0
  if (item != 0)
    out.+=(item) // triggers a "silent" reference to BoxedUnit.UNIT
}
//:javap -c Test
