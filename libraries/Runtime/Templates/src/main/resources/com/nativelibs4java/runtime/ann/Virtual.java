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
 * Position of a method in the virtual table of a C++ class.
 * @author ochafik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Virtual {
    int value();
}
