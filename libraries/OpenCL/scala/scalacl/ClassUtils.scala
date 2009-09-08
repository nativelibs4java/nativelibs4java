package scalacl
import java.nio._


case class ClassUtils(var target: Class[_]) {
	def isBuffer() = classOf[Buffer] isAssignableFrom target
	def isAnyOf(matches: Class[_]*) : Boolean = {
		for (m <- matches.elements)
			if (m isAssignableFrom target)
				return true
		return false
	}
}
//implicit def class2ClassUtils(target: Class[_]) = ClassUtils(target)
