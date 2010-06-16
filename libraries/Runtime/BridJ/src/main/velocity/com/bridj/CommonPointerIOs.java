package com.bridj;
import com.bridj.util.*;
import java.util.*;
import java.lang.reflect.Type;

class CommonPointerIOs {

	static class StructPointerIO<S extends StructObject> extends PointerIO<S> {
		final StructIO structIO;
		public StructPointerIO(StructIO structIO) {
			super(structIO.getStructType(), structIO.getStructSize(), null);
			this.structIO = structIO;
		}
		
		@Override
		public S get(Pointer<S> pointer, long index) {
			return (S)pointer.getNativeObject(index * getTargetSize(), structIO.getStructType());
		}
		@Override
		public void set(Pointer<S> pointer, long index, S value) {
			Pointer<S> ps = Pointer.getPointer(value);
			pointer.getByteBuffer(index * getTargetSize()).put(ps.getByteBuffer(0));
		}
		@Override
		public int getTargetAlignment() {
			return structIO.getStructAlignment();
		}
	}
	
	static class PointerPointerIO<T> extends PointerIO<Pointer<T>> {
		final PointerIO<T> underlyingIO;

		public PointerPointerIO(PointerIO<T> underlyingIO) {
			super(new DefaultParameterizedType(Pointer.class, new Type[] {underlyingIO.getTargetType()}), Pointer.SIZE, null);
			this.underlyingIO = underlyingIO;
		}
		
		@Override
		public Pointer<T> get(Pointer<Pointer<T>> pointer, long index) {
			return pointer.getPointer(index * Pointer.SIZE, underlyingIO);
		}

		@Override
		public void set(Pointer<Pointer<T>> pointer, long index, Pointer<T> value) {
			pointer.setPointer(index * Pointer.SIZE, value);
		}
	}
	
	
	static class TypedPointerPointerIO<P extends TypedPointer> extends PointerIO<P> {
		final java.lang.reflect.Constructor cons;
		final Class<P> pointerClass;
		public TypedPointerPointerIO(Class<P> pointerClass) {
			super(pointerClass, Pointer.SIZE, null);
			this.pointerClass = pointerClass;
			try {
				cons = pointerClass.getConstructor(long.class);
			} catch (Exception ex) {
				throw new RuntimeException("Cannot find constructor for " + pointerClass.getName(), ex);
			}
		}
		
		@Override
		public P get(Pointer<P> pointer, long index) {
			try {
				return (P)cons.newInstance(pointer.getSizeT(index * Pointer.SIZE));
			} catch (Exception ex) {
				throw new RuntimeException("Cannot create pointer of type " + pointerClass.getName(), ex);
			}
		}

		@Override
		public void set(Pointer<P> pointer, long index, P value) {
			pointer.setPointer(index * Pointer.SIZE, value);
		}
	}
	
#foreach ($prim in $primitivesNoBool)
		
	public static final PointerIO<${prim.WrapperName}> ${prim.Name}IO = new PointerIO<${prim.WrapperName}>(${prim.WrapperName}.class, ${prim.Size}, null) {
		@Override
		public ${prim.WrapperName} get(Pointer<${prim.WrapperName}> pointer, long index) {
			return pointer.get${prim.CapName}(index * ${prim.Size});
		}

		@Override
		public void set(Pointer<${prim.WrapperName}> pointer, long index, ${prim.WrapperName} value) {
			pointer.set${prim.CapName}(index * ${prim.Size}, value);
		}		
	};

#end

	public static final PointerIO<SizeT> sizeTIO = new PointerIO<SizeT>(SizeT.class, SizeT.SIZE, null) {
		@Override
		public SizeT get(Pointer<SizeT> pointer, long index) {
			return new SizeT(pointer.getSizeT(index * SizeT.SIZE));
		}
		@Override
		public void set(Pointer<SizeT> pointer, long index, SizeT value) {
			pointer.setSizeT(index * SizeT.SIZE, value == null ? 0 : value.longValue());
		}		
	};

	/*public static final PointerIO<Integer> intIO = new PointerIO<Integer>(Integer.class, 4, null) {
		@Override
		public Integer get(Pointer<Integer> pointer, long index) {
			return pointer.getInt(index * 4);
		}

		@Override
		public void set(Pointer<Integer> pointer, long index, Integer value) {
			pointer.setInt(index * 4, value);
		}		
	};*/

}

