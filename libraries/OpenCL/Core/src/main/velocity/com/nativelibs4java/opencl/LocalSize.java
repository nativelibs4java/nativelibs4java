#parse("main/Header.vm")
package com.nativelibs4java.opencl;
import org.bridj.*;
import org.bridj.util.*;
import java.lang.reflect.Type;
/**
 * Size in bytes of a __local argument.
 */
public class LocalSize {
	long size;
	public LocalSize(long size) {
		this.size = size;
	}
	
	#foreach ($prim in $primitivesNoBool)

	/**
	 * Returns the size in bytes of an array of ${prim.Name} values of the specified length.<br>
	 * @return <code>arrayLength * sizeof(${prim.Name}) = arrayLength * ${prim.Size}<br>
	 */
	public static LocalSize of${prim.CapName}Array(long arrayLength) {
		return new LocalSize(arrayLength * ${prim.Size});
	}
	
	#end
	
	/**
	 * Returns the size in bytes of an array of T values of the specified length.<br>
	 */
	public static LocalSize ofArray(long arrayLength, Type componentType) {
		PointerIO io = PointerIO.getInstance(componentType);
		if (io == null)
			throw new RuntimeException("Unsupported type : " + Utils.toString(componentType));
		return new LocalSize(arrayLength * io.getTargetSize());
	}
}
