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
 *
 * @author Olivier Chafik
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
//@NoInheritance
public @interface Wide {
    /// String encoding
    String value() default "";
}
