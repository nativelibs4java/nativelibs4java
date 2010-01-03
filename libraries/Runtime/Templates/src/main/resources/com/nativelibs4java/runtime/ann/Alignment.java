#if ($useJNA.equals("true"))
#set ($package = "com.nativelibs4java.runtime.ann.jna")
#else
#set ($package = "com.nativelibs4java.runtime.ann")
#end

package $package;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author ochafik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Alignment {
    int value();
}
