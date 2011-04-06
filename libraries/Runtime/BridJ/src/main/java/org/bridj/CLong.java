/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bridj;

/**
 * Wraps a value which size is the same as the 'long' C type (32 bits on a 32 bits platform, 64 bits on a 64 bits platform with GCC and still 32 bits with MSVC++ on 64 bits platforms)
 * @author Olivier
 */
public class CLong extends Number {
    
	public static final int SIZE = Platform.CLONG_SIZE;
	
	private static final long serialVersionUID = 1542942327767932396L;
	private final long value;
    public CLong(long value) {
        this.value = value;
    }
    
    public static CLong valueOf(long value) {
    		return new CLong(value);
    }

    @Override
    public int intValue() {
        return SizeT.safeIntCast(value);
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
    		if (o == null || !(o instanceof CLong))
    			return false;
        return value == ((CLong)o).value;
    }
    
    @Override
    public int hashCode() {
    		return ((Long)value).hashCode();
    }
    
    @Override
    public String toString() {
    		return "CLong(" + value + ")";
    }
    
}
