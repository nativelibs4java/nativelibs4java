#if ($useJNA.equals("true"))
#set ($package = "com.nativelibs4java.runtime.ann.jna")
#else
#set ($package = "com.jdyncall.ann")
#end

package $package;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Size in bits of a bit field
 * @author ochafik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Bits {
    int value();
}
