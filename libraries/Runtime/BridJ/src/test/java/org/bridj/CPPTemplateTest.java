package org.bridj;

import org.bridj.Dyncall.CallingConvention;
import java.io.FileNotFoundException;

import java.util.Collection;

import org.junit.Test;
import static org.junit.Assert.*;

import org.bridj.ann.Constructor;
import org.bridj.ann.Convention;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
import org.bridj.ann.Symbol;
import org.bridj.ann.Template;
import org.bridj.ann.Ptr;
import org.bridj.ann.Virtual;
import org.bridj.cpp.CPPObject;


import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.ann.Field;
import org.bridj.ann.Array;
import org.bridj.ann.Library;
import org.bridj.ann.Struct;
import org.bridj.ann.Name;
import org.bridj.ann.Runtime;
import org.bridj.ann.Virtual;
import org.bridj.cpp.CPPRuntime;

import java.lang.reflect.Type;

import org.junit.After;
import org.junit.Before;
import static org.bridj.Pointer.*;

import org.bridj.CPPTemplateTest.std.vector;
import org.bridj.cpp.CPPType;

///http://www.codesourcery.com/public/cxx-abi/cxx-vtable-ex.html
@Library("test")
@Runtime(CPPRuntime.class)
public class CPPTemplateTest {
	static {
		BridJ.register();
	}

	@Template({ Integer.class, Class.class })
	public static class InvisibleSourcesTemplate<T> extends CPPObject {
		public final int n;
		@Constructor(0)
		public InvisibleSourcesTemplate(int n, Type t, int arg) {
			super(null, 0, n, t, arg);
			this.n = n;
		}
		public native Pointer<T> createSome();
		public native void deleteSome(Pointer<T> pValue);
	}
	@Template({ Class.class })
	public static class Temp1<T> extends CPPObject { }
	
	@Template({ Class.class, Class.class })
	public static class Temp2<T1, T2> extends CPPObject { }
	
	@Template({ Class.class, Integer.class })
	public static class TempV<T> extends CPPObject { }
	
	@Test
	public void invisibleSourcesTemplateIntegerTest() {
		invisibleSourcesTemplateTest(Integer.class);
	}
	<T> void invisibleSourcesTemplateTest(Class<T> t) {
		InvisibleSourcesTemplate<T> ii = new InvisibleSourcesTemplate(10, t, 4);
		Pointer<T> p = ii.createSome();
        T v = p.as(t).get();
		System.out.println("Template created value : " + v);
		ii.deleteSome(p);
	}


	public static final class std implements StructIO.Customizer {
		public StructIO process(StructIO io) {
			if (Platform.isWindows()) {
				Class c = io.getStructClass();
				if (c == vector.class) {
					// On Windows, vector begins by 3 pointers, before the start+finish+end pointers :
					io.prependBytes(3 * Pointer.SIZE);
				}
			}
			return io;
		}
    
		@Template({ Type.class })
		@Struct(customizer = std.class)
		public static class vector<T> extends CPPObject {
			@Field(0)
			public Pointer<T> _M_start() {
				return io.getPointerField(this, 0);
			}
			@Field(1)
			public Pointer<T> _M_finish() {
				return io.getPointerField(this, 1);
			}
			@Field(2)
			public Pointer<T> _M_end_of_storage() {
				return io.getPointerField(this, 2);
			}
			//@Constructor(-1)
			public vector(Type t) {
				super((Void)null, -2, t);
			}
			public vector(Pointer<? extends vector<T>> peer) {
				super(peer);
			}

			public T get(long index) {
                // TODO make this unnecessary
				Pointer<T> p = _M_start().as(T());
                return p.get(index);
			}
			public T get(int index) {
				return get((long)index);
			}
			public void push_back(T value) {
				throw new UnsupportedOperationException();
			}
			protected Type T() {
				return (Type)CPPRuntime.getInstance().getTemplateParameters(this, vector.class)[0];
			}
			protected long byteSize() {
				return _M_finish().getPeer() - _M_start().getPeer();
			}

			public long size() {
				long byteSize = byteSize();
				long elementSize = BridJ.sizeOf(T());

				return byteSize / elementSize;
			}
		}
	}
	//public static native
    ///*
	@Test
	public void testSTLVector() throws Exception {
		NativeLibrary lib = BridJ.getNativeLibrary("test");
		Pointer<?> ptr = lib.getSymbolPointer("newIntVector").getPointer();
		Pointer<?> sptr = lib.getSymbolPointer("sizeofIntVector").getPointer();
		int sizeofIntVector = (Integer)sptr.asDynamicFunction(null, int.class).apply();

		assertEquals("bad vector<int> size !", sizeofIntVector, BridJ.sizeOf(CPPType.getCPPType(vector.class, int.class)));

		vector<Integer> intVector = new vector<Integer>(Integer.class);

		//Pointer<Byte> intVector = allocateBytes(sizeofIntVector);
		Pointer intVectorPtr = pointerTo(intVector);
		DynamicFunction f = ptr.asDynamicFunction(null, void.class, Pointer.class, int.class);

		int size = 10;
		f.apply(intVectorPtr, size);

		//long start = intVectorPtr.getSizeTAtOffset(0);
		//long end = intVector.getSizeTAtOffset(SizeT.SIZE);
		//long endOfStorage = intVector.getSizeTAtOffset(SizeT.SIZE * 2);
		assertEquals("Bad size", size, intVector.size());

        for (int i = 0; i < size; i++) {
            int v = intVector.get(i);

            assertEquals(i, v);
        }
		//System.out.println("size = " + (end - start));
		//System.out.println("capacity = " + (endOfStorage - start));
    }
}

