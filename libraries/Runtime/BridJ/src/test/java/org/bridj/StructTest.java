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
		long len = sizeOf(x);
		int a = 10;
		double b = 12.0;
		pointerTo(x).clearBytes(0, len, (byte)0xff);
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
        private static final int aOffset, bOffset;
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
			return pointer.getInt(aOffset);
        }
        public void a(int a) {
            pointer.putInt(aOffset, aOffset);
        }
        
        @Field(1)
		public double b() {
			return pointer.getDouble(bOffset);
        }
        public void b(double b) {
            pointer.putDouble(bOffset, bOffset);
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

