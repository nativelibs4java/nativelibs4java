package com.nativelibs4java.runtime.structs;
import com.nativelibs4java.runtime.ann.Alignment;
import com.nativelibs4java.runtime.ann.Array;
import com.nativelibs4java.runtime.ann.Bits;
import com.nativelibs4java.runtime.ann.Field;
import com.nativelibs4java.runtime.ann.ByValue;
import com.nativelibs4java.runtime.structs.StructIO.FieldIO.Refreshable;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class StructIO<S extends Struct<S>> {

    static Map<Class<?>, StructIO<?>> structIOs = new HashMap<Class<?>, StructIO<?>>();

    public static synchronized <E extends Struct<E>> StructIO<E> getInstance(Class<E> structClass) {
        StructIO<?> io = structIOs.get(structClass);
        if (io == null)
            registerStructIO((Class)structClass, (StructIO)(io = new StructIO(structClass)));
        return (StructIO<E>)io;
    }

    public static synchronized <E extends Struct<E>> StructIO<E> registerStructIO(Class<E> structClass, StructIO<E> io) {
        structIOs.put(structClass, io);
        return io;
    }

    public static class FieldIO {
        public interface Refreshable<T> {
            public T setPointer(Pointer p);
            public Pointer getPointer();
        }
        String name;
		int index = -1;
		int byteOffset, byteLength;
		int bitOffset, bitLength = -1;
        int arraySize = 1;
        boolean isBitField, isByValue, isNativeSize, isCLong;
        int refreshableFieldIndex = -1;
		Type valueType;
        Class<?> valueClass;
	}
	protected final Class<S> structClass;
	protected volatile FieldIO[] fields;
	private int structSize = -1;
    private int structAlignment = -1;
	
	public StructIO(Class<S> structClass) {
		this.structClass = structClass;

	}

    protected int alignSize(int size, int alignment) {
        if (alignment != 1) {
            int r = size % alignment;
            if (r != 0)
                size += alignment - r;
        }
        return size;
    }


	/// Call whenever an instanceof a struct that depends on that StructIO is created
	public void build() {
		if (fields == null) {
			synchronized (this) {
				if (fields == null)
					fields = computeStructLayout();
			}
		}
	}
    
    /// TODO only create array for fields that need an object representation. Can even return null if none qualify.
	Object[] createRefreshableFieldsArray() {
        return new Object[fields.length];
    }
	
	public int getStructSize() {
		build();
		return structSize;
	}

    public int getStructAlignment() {
		build();
		return structAlignment;
	}
	
	/**
     * Orders the fields to match the actual structure layout
     */
	protected void orderFields(List<FieldIO> fields) {
		Collections.sort(fields, new Comparator<FieldIO>() {

            @Override
            public int compare(FieldIO o1, FieldIO o2) {
                return o1.index - o2.index;
            }

        });
	}

    protected boolean acceptFieldGetter(Method method) {
        if (method.getParameterTypes().length != 0)
            return false;

        int modifiers = method.getModifiers();
        return Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers);
    }

    protected FieldIO newFieldIO() {
        return new FieldIO();
    }
    protected FieldIO createFieldIO(Method getter) {
        FieldIO field = newFieldIO();
        field.valueType = getter.getGenericReturnType();
        field.valueClass = getter.getReturnType();

        String name = getter.getName();
        if (name.matches("get[A-Z].*"))
            name = Character.toLowerCase(name.charAt(3)) + name.substring(4);

        field.name = name;

        Field fil = getter.getAnnotation(Field.class);
        Bits bits = getter.getAnnotation(Bits.class);
        Array arr = getter.getAnnotation(Array.class);
        if (fil != null)
            field.index = fil.value();
        if (bits != null)
            field.bitLength = bits.value();
        if (arr != null)
            field.arraySize = arr.value();
        
        field.isByValue = getter.getAnnotation(ByValue.class) != null;
        return field;
    }

    /**
     * Creates a list of structure fields
     */
	protected List<FieldIO> listFields() {
		List<FieldIO> list = new ArrayList<FieldIO>();
		for (Method method : structClass.getDeclaredMethods()) {
            if (acceptFieldGetter(method)) {
                FieldIO io = createFieldIO(method);
                if (io != null)
                    list.add(io);
            }
		}
		return list;
	}
	
	protected FieldIO[] computeStructLayout() {
		List<FieldIO> list = listFields();
		orderFields(list);

        Alignment alignment = structClass.getAnnotation(Alignment.class);
        structAlignment = alignment != null ? alignment.value() : 1; //TODO get platform default alignment

        int refreshableFieldCount = 0;
        structSize = 0;
        int cumulativeBitOffset = 0;
        for (FieldIO field : list) {
            field.byteOffset = structSize;
            if (field.valueClass.isPrimitive()) {
                if (field.valueClass == Integer.TYPE)
                    field.byteLength = 4;
                else if (field.valueClass == Long.TYPE)
                    field.byteLength = 8;
                else if (field.valueClass == Short.TYPE)
                    field.byteLength = 2;
                else if (field.valueClass == Byte.TYPE)
                    field.byteLength = 1;
                else if (field.valueClass == Float.TYPE)
                    field.byteLength = 4;
                else if (field.valueClass == Double.TYPE)
                    field.byteLength = 8;
                else
                    throw new UnsupportedOperationException("Field type " + field.valueClass.getName() + " not supported yet");

            } else if (Struct.class.isAssignableFrom(field.valueClass)) {
                if (field.isByValue)
                    field.byteLength = Pointer.SIZE;
                else {
                    StructIO<?> io = StructIO.getInstance((Class<? extends Struct>)field.valueClass);
                    field.byteLength = io.getStructSize();
                }
                field.refreshableFieldIndex = refreshableFieldCount++;
            } else if (Pointer.class.equals(field.valueClass)) {
                field.byteLength = Pointer.SIZE;
            } else if (PointerType.class.isAssignableFrom(field.valueClass)) {
                field.byteLength = Pointer.SIZE;
                field.refreshableFieldIndex = refreshableFieldCount++;
            } else if (Buffer.class.isAssignableFrom(field.valueClass)) {
                if (field.valueClass == IntBuffer.class)
                    field.byteLength = 4;
                else if (field.valueClass == LongBuffer.class)
                    field.byteLength = 8;
                else if (field.valueClass == ShortBuffer.class)
                    field.byteLength = 2;
                else if (field.valueClass == ByteBuffer.class)
                    field.byteLength = 1;
                else if (field.valueClass == FloatBuffer.class)
                    field.byteLength = 4;
                else if (field.valueClass == DoubleBuffer.class)
                    field.byteLength = 8;
                else
                    throw new UnsupportedOperationException("Field array type " + field.valueClass.getName() + " not supported yet");
                
                field.refreshableFieldIndex = refreshableFieldCount++;
            } else
                throw new UnsupportedOperationException("Field type " + field.valueClass.getName() + " not supported yet");


            if (field.bitLength < 0) {
				// Align fields as appropriate
				if (cumulativeBitOffset != 0) {
					cumulativeBitOffset = 0;
					structSize++;
				}
                int fieldAlignment = field.byteLength;
				structAlignment = Math.max(structAlignment, fieldAlignment);
                structSize = alignSize(structSize, fieldAlignment);
			}
			field.byteOffset = structSize;
            field.bitOffset = cumulativeBitOffset;

			if (field.bitLength >= 0) {
				field.byteLength = (field.bitLength >>> 3) + ((field.bitLength & 7) != 0 ? 1 : 0);
                cumulativeBitOffset += field.bitLength;
				structSize += cumulativeBitOffset >>> 3;
				cumulativeBitOffset &= 7;
			} else {
                structSize += field.arraySize * field.byteLength;
			}
        }
        if (cumulativeBitOffset > 0)
			structSize = alignSize(structSize + 1, structAlignment);
        else if (structSize > 0)
            structSize = alignSize(structSize, structAlignment);

		return list.toArray(new FieldIO[list.size()]);
	}
	
	public Type getFieldType(int fieldIndex) {
		return fields[fieldIndex].valueType;
	}

    public void read(S struct) {

    }
    public void write(S struct) {
        
    }
    
    public Pointer getPointerField(int fieldIndex, S struct) {
        FieldIO field = fields[fieldIndex];
        assert !field.isBitField;
        assert Pointer.class.isAssignableFrom(field.valueClass);

        return struct.getPointer().getPointer(field.byteOffset);
    }
    public void setPointerField(int fieldIndex, S struct, Pointer p) {
        FieldIO field = fields[fieldIndex];
        assert !field.isBitField;
        assert Pointer.class.isAssignableFrom(field.valueClass);

        struct.getPointer().setPointer(field.byteOffset, p);
    }
    public <P extends PointerType> P getPointerTypeField(int fieldIndex, S struct) {
        FieldIO field = fields[fieldIndex];
        assert !field.isBitField;
        assert PointerType.class.isAssignableFrom(field.valueClass);

        P p = (P)struct.refreshableFields[field.refreshableFieldIndex];
        Pointer ptr = struct.getPointer().getPointer(field.byteOffset);
        try {
            if (p == null)
                struct.refreshableFields[field.refreshableFieldIndex] = p = (P)field.valueClass.getConstructor(Pointer.class).newInstance(ptr);
            else
                p.setPointer(ptr);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to instantiate pointer of type " + field.valueClass.getName(), ex);
        }
        return p;
    }
    public <P extends PointerType> void setPointerTypeField(int fieldIndex, S struct, P p) {
        FieldIO field = fields[fieldIndex];
        assert !field.isBitField;
        assert PointerType.class.isAssignableFrom(field.valueClass);

        struct.refreshableFields[field.refreshableFieldIndex] = p;
        struct.getPointer().setPointer(field.byteOffset, p.getPointer());
    }

    public void setObjectField(int fieldIndex, S struct, Object value) {
        FieldIO field = fields[fieldIndex];
        assert !field.isBitField;
        if (field.valueClass == Pointer.class) {
            struct.getPointer().setPointer(field.byteOffset, (Pointer)value);
            return;
        }
        Refreshable ref = (Refreshable)value;
        struct.refreshableFields[field.refreshableFieldIndex] = ref;
        struct.getPointer().setPointer(field.byteOffset, ref.getPointer());
    }

    public <F extends Refreshable> F getRefreshableField(int fieldIndex, S struct, Class<F> fieldClass) {
        FieldIO field = fields[fieldIndex];
        assert fieldClass.equals(field.valueClass);
        try {
            F sf = (F)struct.refreshableFields[field.refreshableFieldIndex];
            if (sf == null) 
                struct.refreshableFields[field.refreshableFieldIndex] = sf = fieldClass.newInstance();
            
            sf.setPointer(field.isByValue ? struct.getPointer().share(field.byteOffset) : struct.getPointer().getPointer(field.byteOffset));
            return sf;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to instantiate struct of type " + fieldClass.getName(), ex);
        }
	}
    public <F extends Struct<F>> void setStructField(int fieldIndex, S struct, Class<F> fieldClass, F fieldValue) {
        FieldIO field = fields[fieldIndex];
        assert fieldClass.equals(field.valueClass);
        if (field.isByValue) {
            if (fieldValue == null)
                throw new IllegalArgumentException("By-value struct fields cannot be set to null");
            // Nothing to do : by-value struct already wrote its feeds as appropriate
        } else {
            struct.refreshableFields[field.refreshableFieldIndex] = fieldValue;
            struct.getPointer().setPointer(field.byteOffset, fieldValue.getPointer());
        }
	}

#set ($prims = [ "int", "long", "short", "byte", "float", "double" ])
#set ($primCaps = [ "Int", "Long", "Short", "Byte", "Float", "Double" ])
#set ($primBufs = [ "IntBuffer", "LongBuffer", "ShortBuffer", "ByteBuffer", "FloatBuffer", "DoubleBuffer" ])
#set ($primWraps = [ "Integer", "Long", "Short", "Byte", "Float", "Double" ])
#foreach ($prim in $prims)
        
    #set ($i = $velocityCount - 1)
    #set ($primCap = $primCaps.get($i))
    #set ($primWrap = $primWraps.get($i))
    #set ($primBuf = $primBufs.get($i))

    /** $prim field getter */
    public ${prim} get${primCap}Field(int fieldIndex, S struct) {
        FieldIO field = fields[fieldIndex];
        assert field.byteLength == (${primWrap}.SIZE / 8);
        assert ${primWrap}.TYPE.equals(field.valueClass) || ${primWrap}.class.equals(field.valueClass);

        if (field.isBitField)
            return BitFields.getPrimitiveValue(struct.getPointer(), field.byteOffset, field.bitOffset, field.bitLength, ${primWrap}.TYPE);

        return struct.getPointer().get$primCap(field.byteOffset);
	}

    public void setIntField(int fieldIndex, S struct, ${prim} value) {
        FieldIO field = fields[fieldIndex];
        assert field.byteLength == (${primWrap}.SIZE / 8);
        assert ${primWrap}.TYPE.equals(field.valueClass) || ${primWrap}.class.equals(field.valueClass);

        if (field.isBitField)
            BitFields.setPrimitiveValue(struct.getPointer(), field.byteOffset, field.bitOffset, field.bitLength, value, ${primWrap}.TYPE);
        else
            struct.getPointer().set$primCap(field.byteOffset, value);
    }

	public ${primBuf} get${primCap}ArrayField(int fieldIndex, S struct) {
        FieldIO field = fields[fieldIndex];
        ${primBuf} b = (${primBuf})struct.refreshableFields[field.refreshableFieldIndex];
        if (b == null || !b.isDirect() || !struct.getPointer().share(field.byteOffset).equals(Native.getDirectBufferPointer(b))) {
            int len = field.arraySize * field.byteLength;
            struct.refreshableFields[field.refreshableFieldIndex] = b = 
                //(field.isByValue ?
                    struct.getPointer().getByteBuffer(field.byteOffset, len)
                    #if (!$prim.equals("byte"))
                        .as${primBuf}()
                    #end
                //    struct.getPointer().getPointer(field.byteOffset).getByteBuffer(0, len).asIntBuffer()
                //)
            ;
        }
        return b;
    }
    public void set${primCap}ArrayField(int fieldIndex, S struct, ${primBuf} fieldValue) {
        FieldIO field = fields[fieldIndex];
        if (fieldValue == null)
            throw new IllegalArgumentException("By-value struct fields cannot be set to null");

        assert fieldValue.capacity() >= field.arraySize;
        struct.refreshableFields[field.refreshableFieldIndex] = fieldValue;
        int len = field.arraySize * field.byteLength;
        //if (field.isByValue)
            struct.getPointer().getByteBuffer(field.byteOffset, len)
            #if (!$prim.equals("byte"))
                .as${primBuf}()
            #end
                .put(fieldValue.duplicate());
        //else
        //    struct.getPointer().setPointer(field.byteOffset, Native.getDirectBufferPointer(fieldValue));
    }

#end

}
