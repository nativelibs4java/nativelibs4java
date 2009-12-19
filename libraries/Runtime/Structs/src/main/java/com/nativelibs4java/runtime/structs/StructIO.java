package com.nativelibs4java.runtime.structs;
import com.nativelibs4java.runtime.ann.Alignment;
import com.nativelibs4java.runtime.ann.Field;
import com.nativelibs4java.runtime.ann.ByValue;
import com.nativelibs4java.runtime.structs.StructIO.FieldIO.Refreshable;
import com.sun.jna.Pointer;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
public class StructIO<S extends Struct<S>> {

    static Map<Class<?>, StructIO<?>> structIOs = new HashMap<Class<?>, StructIO<?>>();

    public static synchronized <E extends Struct<E>> StructIO<E> getInstance(Class<E> structClass) {
        StructIO<?> io = structIOs.get(structClass);
        if (io == null)
            registerStructIO((Class)structClass, (StructIO)(io = new StructIO(structClass)));
        return (StructIO<E>)io;
    }

    public static synchronized <E extends Struct<E>> void registerStructIO(Class<E> structClass, StructIO<E> io) {
        structIOs.put(structClass, io);
    }

    public static class FieldIO {
        public interface Refreshable {
            public void setPointer(Pointer p, long offset);
            public Pointer getPointer();
        }
        String name;
		int index = -1;
		int byteOffset, byteLength;
		int bitOffset, bitLength = -1;
        int arraySize = -1;
        boolean isBitField, isByValue, isNativeSize, isCLong;
        int refreshableFieldIndex = -1;
		Type valueType;
        Class<?> valueClass;
	}
	protected final Class<S> structClass;
	protected volatile FieldIO[] fields;
	private int structSize = -1;
    private int structAlignment = 1; //TODO get platform default alignment
	
	public StructIO(Class<S> structClass) {
		this.structClass = structClass;

	}

    protected int alignSize(int size, int alignment) {
        if (alignment != 1) {
            int r = size % alignment;
            if (r != 0)
                size += structAlignment - r;
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
	Refreshable[] createRefreshableFieldsArray() {
        return new Refreshable[fields.length];
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
		
	}

    protected boolean acceptFieldGetter(Method method) {
        if (method.getParameterTypes().length != 0)
            return false;

        int modifiers = method.getModifiers();
        return Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers);
    }
    protected FieldIO createFieldIO(Method getter) {
        FieldIO field = new FieldIO();
        field.valueType = getter.getGenericReturnType();
        field.valueClass = getter.getReturnType();

        String name = getter.getName();
        if (name.matches("get[A-Z].*"))
            name = Character.toLowerCase(name.charAt(3)) + name.substring(4);

        field.name = name;

        Field ann = getter.getAnnotation(Field.class);
        if (ann != null) {
            field.bitLength = ann.bits();
            field.index = ann.index();
            field.arraySize = ann.arraySize();
        }
        field.isByValue = getter.getAnnotation(ByValue.class) != null;
        return field;
    }

    /**
     * Creates a list of structure fields
     */
	protected List<FieldIO> listFields() {
		List<FieldIO> list = new ArrayList<FieldIO>();
		for (Method method : structClass.getDeclaredMethods()) {
            if (acceptFieldGetter(method))
                list.add(createFieldIO(method));
		}
		return list;
	}
	
	protected FieldIO[] computeStructLayout() {
		List<FieldIO> list = listFields();
		orderFields(list);

        Alignment alignment = structClass.getAnnotation(Alignment.class);
        if (alignment != null)
            structAlignment = alignment.value();

        int refreshableFieldCount = 0;
        structSize = 0;
        int cumulativeBitOffset = 0;
        for (FieldIO field : list) {
            if (!field.valueClass.isPrimitive())
                field.refreshableFieldIndex = refreshableFieldCount++;

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
            } else
                throw new UnsupportedOperationException("Field type " + field.valueClass.getName() + " not supported yet");


            if (field.bitOffset >= 0) {
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
                structSize += field.arraySize < 0 ? field.byteLength : field.arraySize * field.byteLength;
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
    
    public Object getObjectField(int fieldIndex, S struct) {
        FieldIO field = fields[fieldIndex];
        assert !field.isBitField;
        if (field.valueClass == Pointer.class)
            return struct.getPointer().getPointer(field.byteOffset);

        Refreshable ref = struct.refreshableFields[field.refreshableFieldIndex];
        if (ref == null) {
            if (Struct.class.isAssignableFrom(field.valueClass)) {
                try {
                    Struct<?> s = (Struct<?>)field.valueClass.newInstance();
                    s.setPointer(struct.getPointer().share(field.byteOffset));
                    return s;
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to create struct " + field.valueClass.getName(), ex);
                }
            } else
                throw new UnsupportedOperationException("TODO");
        } else {
            ref.setPointer(struct.getPointer(), field.byteOffset);
            return ref;
        }
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

    public <F extends Struct<F>> F getStructField(int fieldIndex, S struct, Class<F> fieldClass) {
        FieldIO field = fields[fieldIndex];
        assert fieldClass.equals(field.valueClass);
        try {
            F sf = (F)struct.refreshableFields[field.refreshableFieldIndex];
            if (sf == null) 
                struct.refreshableFields[field.refreshableFieldIndex] = sf = fieldClass.newInstance();
            
            sf.setPointer(field.isByValue ? struct.getPointer().share(field.byteOffset) : struct.getPointer().getPointer(field.byteOffset));
            return sf;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to instantiate struct of type " + fieldClass.getName());
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

	public int getIntField(int fieldIndex, S struct) {
        FieldIO field = fields[fieldIndex];
        assert field.byteLength == 4;
        assert Integer.TYPE.equals(field.valueClass) || Integer.class.equals(field.valueClass);

        if (field.isBitField)
            return BitFields.getPrimitiveValue(struct.getPointer(), field.byteOffset, field.bitOffset, field.bitLength, Integer.TYPE);

        return struct.getPointer().getInt(field.byteOffset);
	}

    public void setIntField(int fieldIndex, S struct, int value) {
        FieldIO field = fields[fieldIndex];
        assert field.byteLength == 4;
        assert Integer.TYPE.equals(field.valueClass) || Integer.class.equals(field.valueClass);

        if (field.isBitField)
            BitFields.setPrimitiveValue(struct.getPointer(), field.byteOffset, field.bitOffset, field.bitLength, value, Integer.TYPE);
        else
            struct.getPointer().setInt(field.byteOffset, value);
    }
    
}
