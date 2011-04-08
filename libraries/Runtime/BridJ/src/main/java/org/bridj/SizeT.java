/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bridj;

/**
 * Wraps a value which size is the same as the 'size_t' C type (32 bits on a 32 bits platform, 64 bits on a 64 bits platform)
 * @author Olivier
 */
public class SizeT extends Number {
    
	public static final int SIZE = Platform.SIZE_T_SIZE;
	
	private static final long serialVersionUID = 1547942367767922396L;
	private final long value;
    public SizeT(long value) {
        this.value = value;
    }
    
    public static SizeT valueOf(long value) {
    		return new SizeT(value);
    }


	static final long HIGH_NEG = 0xffffffff00000000L;
	public static int safeIntCast(long value) {
		long high = value & HIGH_NEG;
		if (high != 0 && high != HIGH_NEG) 
		//if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE)
            throw new RuntimeException("Value " + value + " = 0x" + Long.toHexString(value) + " is not within the int range");
		
        return (int)(value & 0xffffffff);
	}

    @Override
    public int intValue() {
        return safeIntCast(value);
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
    		if (o == null || !(o instanceof SizeT))
    			return false;
        return value == ((SizeT)o).value;
    }
    
    @Override
    public int hashCode() {
    		return ((Long)value).hashCode();
    }
    
    @Override
    public String toString() {
    		return "SizeT(" + value + ")";
    }
}
