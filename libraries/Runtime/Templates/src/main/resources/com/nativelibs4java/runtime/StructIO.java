#if ($useJNA == "true")
#set ($package = "com.nativelibs4java.runtime.jna")
#set ($annPackage = "com.nativelibs4java.runtime.ann.jna")
#set ($getPointer = "getPointer")
#else
#set ($package = "com.nativelibs4java.runtime")
#set ($annPackage = "com.nativelibs4java.runtime.ann")
#set ($getPointer = "getReference")
#end

package $package;

#if ($useJNA != "true")
import ${annPackage}.*;
#end

import ${annPackage}.*;

import ${memoryClass};
import ${pointerClass};

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.*;
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
        String name;
		int index = -1;
		int byteOffset, byteLength;
		int bitOffset, bitLength = -1;
        int arraySize = 1;
        boolean isBitField, isByValue, isNativeSize, isCLong, isWide;
        int refreshableFieldIndex = -1;
		Type valueType;
        Class<?> valueClass;
        Class<?> declaringClass;
	}
	
	#if ($useJNA != "true")
	protected PointerIO<S> pointerIO;
	#end
	protected final Class<S> structClass;
	protected volatile FieldIO[] fields;
	private int structSize = -1;
    private int structAlignment = -1;

    protected java.lang.reflect.Field[] javaFields;
    protected java.lang.reflect.Method[] javaIOGetters, javaIOSetters;
	
	public StructIO(Class<S> structClass) {
		this.structClass = structClass;

	}
	
	public Class<S> getStructClass() {
		return structClass;
	}
	
#if ($useJNA != "true")
	public synchronized PointerIO<S> getPointerIO() {
		if (pointerIO == null)
			pointerIO = new PointerIO(getStructClass(), getStructSize());
			
		return pointerIO;
	}
#end

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
                if (o1.declaringClass.isAssignableFrom(o2.declaringClass))
                    return -1;
                if (o2.declaringClass.isAssignableFrom(o1.declaringClass))
                    return -1;
                
                assert o1.declaringClass.equals(o2.declaringClass);
                return o1.index - o2.index;
            }

        });
	}

    protected boolean acceptFieldGetter(Method method) {
        if (method.getParameterTypes().length != 0)
            return false;
        //if (!Struct.class.isAssignableFrom(method.getDeclaringClass()))
        //    return false;

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
        field.declaringClass = getter.getDeclaringClass();

        String name = getter.getName();
        if (name.matches("get[A-Z].*"))
            name = Character.toLowerCase(name.charAt(3)) + name.substring(4);

        field.name = name;

        Field fil = getter.getAnnotation(Field.class);
        Bits bits = getter.getAnnotation(Bits.class);
        Length arr = getter.getAnnotation(Length.class);
        if (fil != null)
            field.index = fil.value();
        if (bits != null)
            field.bitLength = bits.value();
        if (arr != null)
            field.arraySize = arr.value();
        field.isWide = getter.getAnnotation(Wide.class) != null;
		
        field.isByValue = getter.getAnnotation(ByValue.class) != null;
        return field;
    }

    /**
     * Creates a list of structure fields
     */
	protected List<FieldIO> listFields() {
		List<FieldIO> list = new ArrayList<FieldIO>();

        for (Method method : structClass.getMethods()) {
            if (acceptFieldGetter(method)) {
                FieldIO io = createFieldIO(method);
                if (io != null)
                    list.add(io);
            }
        }


        List<Class<?>> classes = new ArrayList<Class<?>>();
        Class<?> c = structClass;
        do {
            classes.add(c = c.getSuperclass());
        } while (Struct.class.isAssignableFrom(c));
        Collections.reverse(classes);
        for (Class<?> cl : classes) {
            for (java.lang.reflect.Field field : structClass.getDeclaredFields()) {

            }
        }
		return list;
	}
	
	protected int primTypeLength(Class<?> primType) {
		if (primType == Integer.TYPE)
			return 4;
		else if (primType == Long.TYPE)
			return 8;
		else if (primType == Short.TYPE)
			return 2;
		else if (primType == Byte.TYPE)
			return 1;
		else if (primType == Float.TYPE)
			return 4;
		else if (primType == Double.TYPE)
			return 8;
		else
			throw new UnsupportedOperationException("Field type " + primType.getName() + " not supported yet");

	}
	protected FieldIO[] computeStructLayout() {
		List<FieldIO> list = listFields();
		orderFields(list);

        Alignment alignment = structClass.getAnnotation(Alignment.class);
        structAlignment = alignment != null ? alignment.value() : 1; //TODO get platform default alignment

        //int fieldCount = 0;
        int refreshableFieldCount = 0;
        structSize = 0;
        int cumulativeBitOffset = 0;
        for (FieldIO field : list) {
            field.byteOffset = structSize;
            if (field.valueClass.isPrimitive()) {
				field.byteLength = primTypeLength(field.valueClass);
            } else if (Struct.class.isAssignableFrom(field.valueClass)) {
                if (field.isByValue)
                    field.byteLength = ${pointerClass}.SIZE;
                else {
                    StructIO<?> io = StructIO.getInstance((Class<? extends Struct>)field.valueClass);
                    field.byteLength = io.getStructSize();
                }
                field.refreshableFieldIndex = refreshableFieldCount++;
            } else if (${pointerClass}.class.equals(field.valueClass)) {
                field.byteLength = ${pointerClass}.SIZE;
		#if ($useJNA == "true")
            } else if (${pointerTypeClass}.class.isAssignableFrom(field.valueClass)) {
                field.byteLength = ${pointerClass}.SIZE;
				field.refreshableFieldIndex = refreshableFieldCount++;
		#end
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
            } else if (field.valueClass.isArray() && field.valueClass.getComponentType().isPrimitive()) {
				field.byteLength = primTypeLength(field.valueClass.getComponentType());
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
            //field.index = fieldCount++;

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

        List<FieldIO> filtered = new ArrayList<FieldIO>();
        for (FieldIO fio : list)
            if (fio.declaringClass.equals(structClass))
                filtered.add(fio);
        
		return filtered.toArray(new FieldIO[filtered.size()]);
	}
	
	public Type getFieldType(int fieldIndex) {
		return fields[fieldIndex].valueType;
	}

    public void read(S struct) {

    }
    public void write(S struct) {
        
    }
    
    public ${pointerClass} getPointerField(int fieldIndex, S struct) {
        FieldIO field = fields[fieldIndex];
        assert !field.isBitField;
        assert ${pointerClass}.class.isAssignableFrom(field.valueClass);

        return struct.$getPointer().getPointer(field.byteOffset);
    }
    public void setPointerField(int fieldIndex, S struct, ${pointerClass} p) {
        FieldIO field = fields[fieldIndex];
        assert !field.isBitField;
        assert ${pointerClass}.class.isAssignableFrom(field.valueClass);

        struct.$getPointer().setPointer(field.byteOffset, p);
    }
	
#if ($useJNA == "true")

	public String getStringField(int fieldIndex, S struct) {
		FieldIO field = fields[fieldIndex];
        assert field.valueClass.equals(String.class);

		return struct.$getPointer().getString(field.byteOffset, field.isWide);
	}
	
	public void setStringField(int fieldIndex, S struct, String value) {
		FieldIO field = fields[fieldIndex];
        assert field.valueClass.equals(String.class);

		struct.$getPointer().setString(field.byteOffset, value, field.isWide);
	}
	
    public String[] getStringArrayField(int fieldIndex, S struct) {
		FieldIO field = fields[fieldIndex];
        assert field.valueClass.equals(String[].class);

		String[] strings = new String[field.arraySize];
		Pointer p = struct.$getPointer();
		for (int i = 0, len = field.arraySize; i < len; i++) {
			int offset = field.byteOffset + i * Pointer.SIZE;
			strings[i] = p.getString(offset, field.isWide);
		}
		return strings;
	}
	
	public void setStringArrayField(int fieldIndex, S struct, String[] value) {
		FieldIO field = fields[fieldIndex];
        assert field.valueClass.equals(String[].class);
		assert field.arraySize == value.length;

		Pointer p = struct.$getPointer();
		for (int i = 0, len = value.length; i < len; i++) {
			String s = value[i];
			int offset = field.byteOffset + i * Pointer.SIZE;
			if (s == null)
				p.setPointer(offset, null);
			else
				p.setString(offset, s);
		}
	}
	
    public <P extends ${pointerTypeClass}> P getPointerTypeField(int fieldIndex, S struct) {
        FieldIO field = fields[fieldIndex];
        assert !field.isBitField;
        assert ${pointerTypeClass}.class.isAssignableFrom(field.valueClass);

        P p = (P)struct.refreshableFields[field.refreshableFieldIndex];
        ${pointerClass} ptr = struct.$getPointer().getPointer(field.byteOffset);
        try {
            if (p == null)
                struct.refreshableFields[field.refreshableFieldIndex] = p = (P)field.valueClass.getConstructor(${pointerClass}.class).newInstance(ptr);
            else
                p.setPointer(ptr);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to instantiate pointer of type " + field.valueClass.getName(), ex);
        }
        return p;
    }
    public <P extends ${pointerTypeClass}> void setPointerTypeField(int fieldIndex, S struct, P p) {
        FieldIO field = fields[fieldIndex];
        assert !field.isBitField;
        assert ${pointerTypeClass}.class.isAssignableFrom(field.valueClass);

        struct.refreshableFields[field.refreshableFieldIndex] = p;
        struct.$getPointer().setPointer(field.byteOffset, p.getPointer());
    }
#end

#if ($useJNA != "true")

    public void setRefreshableField(int fieldIndex, S struct, PointerRefreshable value) {
        FieldIO field = fields[fieldIndex];
        assert !field.isBitField;
        if (field.valueClass == ${pointerClass}.class) {
            struct.$getPointer().setPointer(field.byteOffset, (${pointerClass})value);
            return;
        }
        PointerRefreshable ref = (PointerRefreshable)value;
        struct.refreshableFields[field.refreshableFieldIndex] = ref;
        struct.$getPointer().setPointer(field.byteOffset, ref.getPointer());
    }

    public <F extends PointerRefreshable> F getRefreshableField(int fieldIndex, S struct, Class<F> fieldClass) {
        FieldIO field = fields[fieldIndex];
        assert fieldClass.equals(field.valueClass);
        try {
            F sf = (F)struct.refreshableFields[field.refreshableFieldIndex];
            if (sf == null) 
                struct.refreshableFields[field.refreshableFieldIndex] = sf = fieldClass.newInstance();
            
            sf.setPointer(field.isByValue ? struct.$getPointer().share(field.byteOffset) : struct.$getPointer().getPointer(field.byteOffset));
            return sf;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to instantiate struct of type " + fieldClass.getName(), ex);
        }
	}

#end

    public <F extends Struct<F>> void setStructField(int fieldIndex, S struct, Class<F> fieldClass, F fieldValue) {
        FieldIO field = fields[fieldIndex];
        assert fieldClass.equals(field.valueClass);
        if (field.isByValue) {
            if (fieldValue == null)
                throw new IllegalArgumentException("By-value struct fields cannot be set to null");
            // Nothing to do : by-value struct already wrote its feeds as appropriate
        } else {
            struct.refreshableFields[field.refreshableFieldIndex] = fieldValue;
            struct.$getPointer().setPointer(field.byteOffset, fieldValue.$getPointer());
        }
	}

    public <F extends Struct<F>> F getStructField(int fieldIndex, S struct, Class<F> fieldClass) {
        FieldIO field = fields[fieldIndex];
        assert fieldClass.equals(field.valueClass);
        F fieldValue = (F)struct.refreshableFields[field.refreshableFieldIndex];
        if (fieldValue == null) {
            try {
                struct.refreshableFields[field.refreshableFieldIndex] = fieldValue = fieldClass.newInstance();
            } catch (Exception ex) {
                throw new RuntimeException("Failed to instantiate struct of type " + fieldClass.getName(), ex);
            }
        }
        
        fieldValue.setPointer(struct.$getPointer().share(field.byteOffset));
        return fieldValue;
	}

    public <F extends Struct<F>> Array<F> getStructArrayField(int fieldIndex, S struct, Class<F> fieldClass) {
        FieldIO field = fields[fieldIndex];
        assert fieldClass.equals(field.valueClass);

        ${pointerClass} ptr = struct.$getPointer().share(field.byteOffset);
        Array<F> fieldValue = (Array<F>)struct.refreshableFields[field.refreshableFieldIndex];
        if (fieldValue == null)
            struct.refreshableFields[field.refreshableFieldIndex] = fieldValue = new Array<F>(fieldClass, field.arraySize, ptr);
        else
            fieldValue.setPointer(ptr);
        
        return fieldValue;
	}

	public boolean getBoolField(int fieldIndex, S struct) {
		return getByteField(fieldIndex, struct) != 0;
	}
	
	
	public void setBoolField(int fieldIndex, S struct, boolean fieldValue) {
		setByteField(fieldIndex, struct, (byte)(fieldValue ? 1 : 0));
	}

#foreach ($prim in $primitivesNoBool)
        
    /** $prim field getter */
    public ${prim.Name} get${prim.CapName}Field(int fieldIndex, S struct) {
        FieldIO field = fields[fieldIndex];
        assert field.byteLength == (${prim.WrapperName}.SIZE / 8);
        assert ${prim.WrapperName}.TYPE.equals(field.valueClass) || ${prim.WrapperName}.class.equals(field.valueClass);

        if (field.isBitField)
            return BitFields.getPrimitiveValue(struct.$getPointer(), field.byteOffset, field.bitOffset, field.bitLength, ${prim.WrapperName}.TYPE);

        return struct.$getPointer().get${prim.CapName}(field.byteOffset);
	}

    public void set${prim.CapName}Field(int fieldIndex, S struct, ${prim.Name} value) {
        FieldIO field = fields[fieldIndex];
        assert field.byteLength == (${prim.WrapperName}.SIZE / 8);
        assert ${prim.WrapperName}.TYPE.equals(field.valueClass) || ${prim.WrapperName}.class.equals(field.valueClass);

        if (field.isBitField)
            BitFields.setPrimitiveValue(struct.$getPointer(), field.byteOffset, field.bitOffset, field.bitLength, value, ${prim.WrapperName}.TYPE);
        else
            struct.$getPointer().set${prim.CapName}(field.byteOffset, value);
    }

	public ${prim.BufferName} get${prim.CapName}BufferField(int fieldIndex, S struct) {
        FieldIO field = fields[fieldIndex];
        ${prim.BufferName} b = (${prim.BufferName})struct.refreshableFields[field.refreshableFieldIndex];
        if (b == null || !b.isDirect() || !struct.$getPointer().share(field.byteOffset).equals(${getDirectBufferPointer}(b))) {
            int len = field.arraySize * field.byteLength;
            struct.refreshableFields[field.refreshableFieldIndex] = b = 
                struct.$getPointer().getByteBuffer(field.byteOffset, len)
                #if (!$prim.Name.equals("byte"))
                    .as${prim.BufferName}()
                #end
            ;
        }
        return b;
    }
    public void set${prim.CapName}BufferField(int fieldIndex, S struct, ${prim.BufferName} fieldValue) {
        FieldIO field = fields[fieldIndex];
        if (fieldValue == null)
            throw new IllegalArgumentException("By-value struct fields cannot be set to null");

        assert fieldValue.capacity() >= field.arraySize;
        struct.refreshableFields[field.refreshableFieldIndex] = fieldValue;
        int len = field.arraySize * field.byteLength;
        struct.$getPointer().getByteBuffer(field.byteOffset, len)
        #if (!$prim.Name.equals("byte"))
            .as${prim.BufferName}()
        #end
            .put(fieldValue.duplicate());
    }

	public ${prim.Name}[] get${prim.CapName}ArrayField(int fieldIndex, S struct) {
        FieldIO field = fields[fieldIndex];
		return struct.$getPointer().get${prim.CapName}Array(field.byteOffset, field.arraySize);
    }
    public void set${prim.CapName}ArrayField(int fieldIndex, S struct, ${prim.Name}[] fieldValue) {
        FieldIO field = fields[fieldIndex];
        if (fieldValue == null)
            throw new IllegalArgumentException("By-value struct fields cannot be set to null");

		struct.$getPointer().write(field.byteOffset, fieldValue, 0, field.arraySize);
    }

#end

}
