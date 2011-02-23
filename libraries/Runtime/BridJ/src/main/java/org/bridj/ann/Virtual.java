package org.bridj.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a C++ method as virtual and specify its position in the virtual table.<br>
 * The virtual table offset is optional but strongly recommended (will fail in many cases without it).<br>
 * This position is absolute : it must take into account the virtual table offset due to parent classes (unlike {@link Field}, which index is relative to the declared class).
 * @author ochafik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Virtual {
	/**
	 * Optional virtual table offset for the C++ method
	 */
    int value() default -1;
}
