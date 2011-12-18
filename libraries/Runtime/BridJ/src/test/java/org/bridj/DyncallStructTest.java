package org.bridj;

import org.junit.Test;
import static org.junit.Assert.*;

import org.bridj.ann.Field;
import org.bridj.ann.Array;


import static org.bridj.dyncall.DyncallLibrary.*;

public class DyncallStructTest {

    public static class SimpleStruct extends StructObject {
		@Field(0)
		public int a;
		
		@Field(1)
        @Array(2)
		public Pointer<Long> b;
        
		@Field(2)
		public short c;
        
		@Field(3)
        @Array(3)
		public Pointer<Byte> d;
        
		@Field(4)
		public float e;
	}
	
    @Test
    public void testSimpleStruct() {
        Pointer<DCstruct> s = dcNewStruct(5, DEFAULT_ALIGNMENT);
        try {
            dcStructField(s, DC_SIGCHAR_INT, DEFAULT_ALIGNMENT, 1);
            dcStructField(s, DC_SIGCHAR_LONGLONG, DEFAULT_ALIGNMENT, 2);
            dcStructField(s, DC_SIGCHAR_SHORT, DEFAULT_ALIGNMENT, 1);
            dcStructField(s, DC_SIGCHAR_CHAR, DEFAULT_ALIGNMENT, 3);
            dcStructField(s, DC_SIGCHAR_FLOAT, DEFAULT_ALIGNMENT, 1);
            dcCloseStruct(s);

            long size = dcStructSize(s);
            assertEquals(BridJ.sizeOf(SimpleStruct.class), size);
        } finally {
            dcFreeStruct(s);
        }
    }
            
}