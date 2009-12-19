package com.nativelibs4java.runtime.structs;
import com.nativelibs4java.runtime.ann.Field;
import com.nativelibs4java.runtime.ann.ByValue;
import com.nativelibs4java.runtime.structs.StructIO.FieldIO.Refreshable;
import com.sun.jna.Pointer;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
public class StructIO<S extends Struct<S>> {

    public static class FieldIO {
        public interface Refreshable {
            public void setPointer(Pointer p, long offset);
            public Pointer getPointer();
        }
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
	
	public StructIO(Class<S> structClass) {
		this.structClass = structClass;
	}
	
	/// Call whenever an instanceof a struct that depends on that StructIO is created
	public void build() {
		if (fields == null) {
			synchronized (fields) {
				if (fields == null)
					fields = createFields();
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
	
	protected void orderFields(List<FieldIO> fields) {
		
	}
	protected List<FieldIO> listFields() {
		List<FieldIO> list = new ArrayList<FieldIO>();
		for (Method method : structClass.getDeclaredMethods()) {
			if (method.getParameterTypes().length != 0)
				continue;
			
			FieldIO field = new FieldIO();
			field.valueType = method.getGenericReturnType();
            field.valueClass = method.getReturnType();

            Field ann = method.getAnnotation(Field.class);
            if (ann != null) {
                field.bitLength = ann.bits();
                field.index = ann.index();
                field.arraySize = ann.arraySize();
            }
            field.isByValue = method.getAnnotation(ByValue.class) != null;
		}
		return list;
	}
	
	protected FieldIO[] createFields() {
		List<FieldIO> list = listFields();
		orderFields(list);

        int refreshableFieldCount = 0;
        for (FieldIO field : list) {
            if (!field.valueClass.isPrimitive())
                field.refreshableFieldIndex = refreshableFieldCount++;
            
        }
		return list.toArray(new FieldIO[list.size()]);
	}
	
	public Type getFieldType(int fieldIndex) {
		return fields[fieldIndex].valueType;
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
