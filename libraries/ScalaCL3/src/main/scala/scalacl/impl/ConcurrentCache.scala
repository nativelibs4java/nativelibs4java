package scalacl.impl

/**
 * Simple concurrent cache with support for costly extra values discarding
 */
class ConcurrentCache[K, V <: AnyRef] {
  private val map = new java.util.concurrent.ConcurrentHashMap[K, V]
  
  def apply(key: K, discardValue: V => Unit)(initialValue: => V) = {
    val value = map.get(key)
    if (value != null) {
      value
    } else {
      val newValue = initialValue
      val oldValue = map.putIfAbsent(key, newValue)
      if (oldValue eq null) {
        newValue
      } else {
        discardValue(newValue)
        oldValue
      }
    }
  }
  def clear(discardValue: V => Unit = null) = {
    if (discardValue == null) {
      map.clear()
    } else {
      while (!map.isEmpty) {
        val it = map.values().iterator
        while (it.hasNext) {
          discardValue(it.next)
          it.remove()
        }
      }
    }
  }
}