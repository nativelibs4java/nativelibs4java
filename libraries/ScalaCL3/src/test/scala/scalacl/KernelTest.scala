package scalacl
import impl._

import org.junit._
import Assert._

class KernelTest {
  @Test
  def testEquality {
    val sources = "aa"
    same(new Kernel(1, sources), new Kernel(1, sources))
    diff(new Kernel(1, sources), new Kernel(2, sources), false)
    diff(new Kernel(1, sources), new Kernel(1, "a" + ('b' - 1)), true)
  }
  
  def same(a: AnyRef, b: AnyRef) = {
    assertEquals(a.hashCode, b.hashCode)
    assertEquals(a, b)
  }
  
  def diff(a: AnyRef, b: AnyRef, sameHC: Boolean) = {
    assertTrue(sameHC ^ (a.hashCode != b.hashCode))
    assertFalse(a.equals(b))
  }
}
