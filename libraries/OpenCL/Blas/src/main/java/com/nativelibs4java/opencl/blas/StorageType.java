/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl.blas;

import com.nativelibs4java.util.*;

/**
 *
 * @author ochafik
 */
public enum StorageType implements ValuedEnum {
	RowPacked(Inner.ROW_PACKED),
	ColumnPacked(Inner.COLUMN_PACKED);

    private static class Inner {
        public static final int ROW_PACKED = 1, COLUMN_PACKED = 2;
    }

    StorageType(long value) { this.value = value; }
	long value; 
    public long value() { return value; }
}
