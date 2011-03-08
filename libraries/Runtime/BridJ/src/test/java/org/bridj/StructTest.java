package org.bridj;

import com.sun.jna.Memory;
import java.nio.ByteBuffer;
import org.junit.Test;
import static org.junit.Assert.*;

import org.bridj.ann.Field;
import static org.bridj.Pointer.*;
import static org.bridj.BridJ.*;

import javolution.io.*;

///http://www.codesourcery.com/public/cxx-abi/cxx-vtable-ex.html
public class StructTest {
	public class CAPDRIVERCAPS extends StructObject {
		public CAPDRIVERCAPS() {
			super();
		}
		public CAPDRIVERCAPS(Pointer pointer) {
			super(pointer);
		}
		/// C type : UINT
		@Field(0) 
		public int wDeviceIndex() {
			return this.io.getIntField(this, 0);
		}
		/// C type : UINT
		@Field(0) 
		public CAPDRIVERCAPS wDeviceIndex(int wDeviceIndex) {
			this.io.setIntField(this, 0, wDeviceIndex);
			return this;
		}
		/// C type : UINT
		public final int wDeviceIndex_$eq(int wDeviceIndex) {
			wDeviceIndex(wDeviceIndex);
			return wDeviceIndex;
		}
		@Field(1) 
		public boolean fHasOverlay() {
			return this.io.getBooleanField(this, 1);
		}
		@Field(1) 
		public CAPDRIVERCAPS fHasOverlay(boolean fHasOverlay) {
			this.io.setBooleanField(this, 1, fHasOverlay);
			return this;
		}
		public final boolean fHasOverlay_$eq(boolean fHasOverlay) {
			fHasOverlay(fHasOverlay);
			return fHasOverlay;
		}
		@Field(2) 
		public boolean fHasDlgVideoSource() {
			return this.io.getBooleanField(this, 2);
		}
		@Field(2) 
		public CAPDRIVERCAPS fHasDlgVideoSource(boolean fHasDlgVideoSource) {
			this.io.setBooleanField(this, 2, fHasDlgVideoSource);
			return this;
		}
		public final boolean fHasDlgVideoSource_$eq(boolean fHasDlgVideoSource) {
			fHasDlgVideoSource(fHasDlgVideoSource);
			return fHasDlgVideoSource;
		}
		@Field(3) 
		public boolean fHasDlgVideoFormat() {
			return this.io.getBooleanField(this, 3);
		}
		@Field(3) 
		public CAPDRIVERCAPS fHasDlgVideoFormat(boolean fHasDlgVideoFormat) {
			this.io.setBooleanField(this, 3, fHasDlgVideoFormat);
			return this;
		}
		public final boolean fHasDlgVideoFormat_$eq(boolean fHasDlgVideoFormat) {
			fHasDlgVideoFormat(fHasDlgVideoFormat);
			return fHasDlgVideoFormat;
		}
		@Field(4) 
		public boolean fHasDlgVideoDisplay() {
			return this.io.getBooleanField(this, 4);
		}
		@Field(4) 
		public CAPDRIVERCAPS fHasDlgVideoDisplay(boolean fHasDlgVideoDisplay) {
			this.io.setBooleanField(this, 4, fHasDlgVideoDisplay);
			return this;
		}
		public final boolean fHasDlgVideoDisplay_$eq(boolean fHasDlgVideoDisplay) {
			fHasDlgVideoDisplay(fHasDlgVideoDisplay);
			return fHasDlgVideoDisplay;
		}
		@Field(5) 
		public boolean fCaptureInitialized() {
			return this.io.getBooleanField(this, 5);
		}
		@Field(5) 
		public CAPDRIVERCAPS fCaptureInitialized(boolean fCaptureInitialized) {
			this.io.setBooleanField(this, 5, fCaptureInitialized);
			return this;
		}
		public final boolean fCaptureInitialized_$eq(boolean fCaptureInitialized) {
			fCaptureInitialized(fCaptureInitialized);
			return fCaptureInitialized;
		}
		@Field(6) 
		public boolean fDriverSuppliesPalettes() {
			return this.io.getBooleanField(this, 6);
		}
		@Field(6) 
		public CAPDRIVERCAPS fDriverSuppliesPalettes(boolean fDriverSuppliesPalettes) {
			this.io.setBooleanField(this, 6, fDriverSuppliesPalettes);
			return this;
		}
		public final boolean fDriverSuppliesPalettes_$eq(boolean fDriverSuppliesPalettes) {
			fDriverSuppliesPalettes(fDriverSuppliesPalettes);
			return fDriverSuppliesPalettes;
		}
		/// C type : HANDLE
		@Field(7) 
		public Pointer<? > hVideoIn() {
			return this.io.getPointerField(this, 7);
		}
		/// C type : HANDLE
		@Field(7) 
		public CAPDRIVERCAPS hVideoIn(Pointer<? > hVideoIn) {
			this.io.setPointerField(this, 7, hVideoIn);
			return this;
		}
		/// C type : HANDLE
		public final Pointer<? > hVideoIn_$eq(Pointer<? > hVideoIn) {
			hVideoIn(hVideoIn);
			return hVideoIn;
		}
		/// C type : HANDLE
		@Field(8) 
		public Pointer<? > hVideoOut() {
			return this.io.getPointerField(this, 8);
		}
		/// C type : HANDLE
		@Field(8) 
		public CAPDRIVERCAPS hVideoOut(Pointer<? > hVideoOut) {
			this.io.setPointerField(this, 8, hVideoOut);
			return this;
		}
		/// C type : HANDLE
		public final Pointer<? > hVideoOut_$eq(Pointer<? > hVideoOut) {
			hVideoOut(hVideoOut);
			return hVideoOut;
		}
		/// C type : HANDLE
		@Field(9) 
		public Pointer<? > hVideoExtIn() {
			return this.io.getPointerField(this, 9);
		}
		/// C type : HANDLE
		@Field(9) 
		public CAPDRIVERCAPS hVideoExtIn(Pointer<? > hVideoExtIn) {
			this.io.setPointerField(this, 9, hVideoExtIn);
			return this;
		}
		/// C type : HANDLE
		public final Pointer<? > hVideoExtIn_$eq(Pointer<? > hVideoExtIn) {
			hVideoExtIn(hVideoExtIn);
			return hVideoExtIn;
		}
		/// C type : HANDLE
		@Field(10) 
		public Pointer<? > hVideoExtOut() {
			return this.io.getPointerField(this, 10);
		}
		/// C type : HANDLE
		@Field(10) 
		public CAPDRIVERCAPS hVideoExtOut(Pointer<? > hVideoExtOut) {
			this.io.setPointerField(this, 10, hVideoExtOut);
			return this;
		}
		/// C type : HANDLE
		public final Pointer<? > hVideoExtOut_$eq(Pointer<? > hVideoExtOut) {
			hVideoExtOut(hVideoExtOut);
			return hVideoExtOut;
		}
	}
	@Test
	public void testSizeOfCAPDRIVERCAPS() {
		if (Platform.isMacOSX()) {
			if (Platform.is64Bits())
				assertEquals(48, BridJ.sizeOf(CAPDRIVERCAPS.class));
			else
				assertEquals(28, BridJ.sizeOf(CAPDRIVERCAPS.class));
		}
	}
	public static class MyStruct extends StructObject {
        public MyStruct(Pointer<MyStruct> p) { super(p); }
        public MyStruct() { super(); }
        @Field(0)
		public int a() {
			return io.getIntField(this, 0);
        }
        public MyStruct a(int a) {
            io.setIntField(this, 0, a);
            return this;
        }
        
        @Field(1)
		public double b() {
			return io.getDoubleField(this, 1);
        }
        public MyStruct b(double b) {
            io.setDoubleField(this, 1, b);
            return this;
        }
	}
	
	@Test
	public void testEquality() {
		MyStruct x = new MyStruct(), y = new MyStruct(), z = new MyStruct();
		long len = sizeOf(MyStruct.class);
		int a = 10;
		double b = 12.0;
		pointerTo(x).clearBytesAtOffset(0, len, (byte)0xff);
		for (MyStruct s : new MyStruct[] { x, y })
			s.a(a).b(b);
		
		assertFalse(pointerTo(x).compareBytes(pointerTo(y), len) == 0);
		assertEquals(x, y);
		assertFalse(x.equals(z));
	}
	
	/*
	public static class MyStruct extends StructObject {
		public MyStruct(org.bridj.Pointer<MyStruct> p) {
			super(p);
		}
		public MyStruct() {
			super();
		}
		@Field(0)
		public native int a();
		public native void a(int a);

        @Field(1)
		public native double b();
		public native void b(double a);
	}*/
	
	public static class MyJNAStruct extends com.sun.jna.Structure {
		public MyJNAStruct(com.sun.jna.Pointer p) {
			super(p);
		}
		public MyJNAStruct() {
			super();
		}
		public int a;
		public double b;
	}
    public static class MyNIOStruct {
        final ByteBuffer p;
		public MyNIOStruct(ByteBuffer p) {
            this.p = p;
        }
        public MyNIOStruct() {
            this(ByteBuffer.allocateDirect(12));
        }
        public int a() {
			return p.getInt(0);
        }
        public void a(int a) {
            p.putInt(0, a);
        }
        
        public double b() {
			return p.getDouble(4);
        }
        public void b(double b) {
            p.putDouble(4, b);
        }
	}
    public static class MyOptimalStruct {
        //protected final com.sun.jna.Pointer pointer;
        ByteBuffer pointer;
        private static final long aOffset, bOffset;
        static {
            StructIO io = StructIO.getInstance(MyOptimalStruct.class, MyOptimalStruct.class);
            io.build();
            aOffset = io.fields[0].byteOffset;
            bOffset = io.fields[1].byteOffset;
        }
		public MyOptimalStruct(ByteBuffer p) {//com.sun.jna.Pointer p) {
            this.pointer = p;
        }
        public MyOptimalStruct() {
            //this(new Memory(16));
            //this(allocateBytes(16));
            this(ByteBuffer.allocateDirect(16));
        }
        @Field(0)
		public int a() {
			return pointer.getInt((int)aOffset);
        }
        public void a(int a) {
            pointer.putInt((int)aOffset, a);
        }
        
        @Field(1)
		public double b() {
			return pointer.getDouble((int)bOffset);
        }
        public void b(double b) {
            pointer.putDouble((int)bOffset, b);
        }
	}
	
	public static class MyJavolutionStruct extends javolution.io.Struct {
		public final Signed32 a = new Signed32();
		public final Float64 b = new Float64();
	}
	
	@Test
	public void trivial() {
		MyStruct s = new MyStruct();
		s.a(10);
        s.b(100.0);
		int a = s.a();
		double b = s.b();
		assertEquals(10, a);
		assertEquals(100.0, b, 0);
	}

	public static class CastStruct extends StructObject {
        public CastStruct(Pointer p) {
            super(p);
        }
		@Field(0)
		public int a() {
			return this.io.getIntField(this, 0);
		}
        public void a(int a) {
			this.io.setIntField(this, 0, a);
		}
		//public native CastStruct a(int a);

	}

    @Test
    public void testCast() {
        Pointer<?> m = Pointer.allocateBytes(100);
        CastStruct s = new CastStruct(m);
        s.a(10);
        //assertTrue(s == s.a(10));
        int a = s.a();
        assertEquals(10, a);
    }

    public static class ArrStruct extends StructObject {

		@Field(0)
		public int a() {
			return this.io.getIntField(this, 0);
		}
		public ArrStruct a(int a) {
			this.io.setIntField(this, 0, a);
			return this;
		}

	}
    @Test
    public void arrayCast() {
        Pointer<ArrStruct> formats = allocateArray(ArrStruct.class, 10);
        assertEquals(10, formats.getValidElements());
        for (ArrStruct s : formats) {
            assertNotNull(s);
            assertEquals(0, s.a());
            assertTrue(s == s.a(10));
        }
    }


    public static class ThisStruct extends StructObject {

		@Field(0)
		public int a() {
            return io.getIntField(this, 0);
        }
		public ThisStruct a(int a) {
            io.setIntField(this, 0, a);
            return this;
        }

	}
    @Test
    public void testThisStruct() {
        ThisStruct s = new ThisStruct();
        ThisStruct o = s.a(10);
        assertTrue(s == o);
        int a = s.a();
        assertEquals(10, a);
    }
}

