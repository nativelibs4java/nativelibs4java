#if ($useJNA.equals("true"))
#set ($package = "com.nativelibs4java.runtime.structs.jna")
#else
#set ($package = "com.nativelibs4java.runtime.structs")
#end

package $package;

/**
 *
 * @author ochafik
 */
public interface Addressable {
    
}
