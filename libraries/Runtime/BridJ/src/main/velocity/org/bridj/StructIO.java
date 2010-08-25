package org.bridj;
import org.bridj.CallIO.NativeObjectHandler;
import org.bridj.ann.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StructIO {

    static Map<Type, StructIO> structIOs = new HashMap<Type, StructIO>();

    public static StructIO getInstance(Class structClass, Type structType) {
        synchronized (structIOs) {
            StructIO io = structIOs.get(structType == null ? structClass : structType);
            if (io == null)
                registerStructIO(structClass, structType, (StructIO)(io = new StructIO(structClass, structType)));
            return (StructIO)io;
        }
    }

    public static synchronized StructIO registerStructIO(Class structClass, Type structType, StructIO io) {
        structIOs.put(structType, io);
        return io;
    }

    public static class FieldDesc {
        public int byteOffset;
		public int bitOffset, bitLength = -1;
        public long arrayLength = 1;
        public Type nativeTypeOrPointerTargetType;
	}
	protected static class FieldDecl {
		final FieldDesc desc = new FieldDesc();
		Method getter, setter;
		String name;
		int index = -1;
		int byteLength;
		Type valueType;
        Class<?> valueClass;
        Class<?> declaringClass;
        boolean isBitField, isByValue, isWide;
    }
	
	protected PointerIO<?> pointerIO;
	protected volatile FieldDesc[] fields;
	private int structSize = -1;
    private int structAlignment = -1;
	protected final Class<?> structClass;
	protected final Type structType;

    public StructIO(Class<?> structClass, Type structType) {
		this.structClass = structClass;
        this.structType = structType;
        // Don't call build here, for recursive initialization cases (TODO test this)
	}
	
	boolean isVirtual() {
		for (Method m : structClass.getMethods()) {
			if (m.getAnnotation(Virtual.class) != null)
				return true;
		}
		return false;
	}
	public Class<?> getStructClass() {
		return structClass;
	}
	public Type getStructType() {
		return structType;
	}
	
	public synchronized PointerIO<?> getPointerIO() {
		if (pointerIO == null)
			pointerIO = new CommonPointerIOs.StructPointerIO(this);
			
		return pointerIO;
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
	void build() {
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
	
	public final int getStructSize() {
//		build();
		return structSize;
	}

    public final int getStructAlignment() {
//		build();
		return structAlignment;
	}
	
	/**
     * Orders the fields to match the actual structure layout
     */
	protected void orderFields(List<FieldDecl> fields) {
		Collections.sort(fields, new Comparator<FieldDecl>() {

            @Override
            public int compare(FieldDecl o1, FieldDecl o2) {
                int d = o1.index - o2.index;
                if (d != 0)
                    return d;

                if (o1.declaringClass.isAssignableFrom(o2.declaringClass))
                    return -1;
                if (o2.declaringClass.isAssignableFrom(o1.declaringClass))
                    return 1;
                
                throw new RuntimeException("Failed to order fields " + o2.name + " and " + o2.name);
            }

        });
	}
	
    protected boolean acceptFieldGetter(Method method, boolean getter) {
        if (method.getParameterTypes().length != (getter ? 0 : 1))
            return false;
        
        if (getter && method.getAnnotation(Field.class) == null)
            return false;

        int modifiers = method.getModifiers();
        
        return //Modifier.isNative(modifiers) && 
                !Modifier.isStatic(modifiers);// &&
                //!forbiddenGetterNames.contains(method.getName());
    }

    protected FieldDecl createFieldDecl(Method getter) {
        FieldDecl field = new FieldDecl();
        field.getter = getter;
        field.valueType = getter.getGenericReturnType();
        field.valueClass = getter.getReturnType();
        field.declaringClass = getter.getDeclaringClass();

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
            field.desc.bitLength = bits.value();
        if (arr != null) {
            long length = 1;
            for (long dim : arr.value())
                length *= dim;
            field.desc.arrayLength = length;
        }
        field.isWide = getter.getAnnotation(Wide.class) != null;
        field.isByValue = getter.getAnnotation(ByValue.class) != null;
        return field;
    }

    /**
     * Creates a list of structure fields
     */
	protected List<FieldDecl> listFields() {
		List<FieldDecl> list = new ArrayList<FieldDecl>();

        for (Method method : structClass.getMethods()) {
            if (acceptFieldGetter(method, true)) {
                FieldDecl io = createFieldDecl(method);
                try {
                	Method setter = structClass.getMethod(method.getName(), io.valueClass);
                	if (acceptFieldGetter(setter, false))
                		io.setter = setter;
                } catch (Exception ex) {
                		//assert BridJ.log(Level.INFO, "No setter for getter " + method);
                }
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
		else if (Pointer.class.isAssignableFrom(primType))
			return 8;
		else
			throw new UnsupportedOperationException("Field type " + primType.getName() + " not supported yet");

	}
	protected FieldDesc[] computeStructLayout() {
		List<FieldDecl> list = listFields();
		orderFields(list);
		
		Alignment alignment = structClass.getAnnotation(Alignment.class);
        structAlignment = alignment != null ? alignment.value() : 1; //TODO get platform default alignment

        //int fieldCount = 0;
        int refreshableFieldCount = 0;
        structSize = 0;
        if (isVirtual()) {
        	structSize += Pointer.SIZE;
			/*FieldDecl d = new FieldDecl();
			d.byteLength = Pointer.SIZE;
			d.valueType = d.valueClass = Pointer.class;
			d.name = "vtablePtr";
			list.add(0, d);*/
		}

        int cumulativeBitOffset = 0;
        for (FieldDecl field : list) {
            field.desc.byteOffset = structSize;
            if (field.valueClass.isPrimitive()) {
				field.byteLength = primTypeLength(field.valueClass);
            } else if (StructObject.class.isAssignableFrom(field.valueClass)) {
            	field.desc.nativeTypeOrPointerTargetType = field.valueType;
                if (field.isByValue)		
                    field.byteLength = Pointer.SIZE;		
                else {		
                    StructIO io = StructIO.getInstance(field.valueClass, field.valueType);		
                    field.byteLength = io.getStructSize();		
                }		
                //field.refreshableFieldIndex = refreshableFieldCount++;		
            } else if (IntValuedEnum.class.isAssignableFrom(field.valueClass)) {
                field.desc.nativeTypeOrPointerTargetType = (field.valueType instanceof ParameterizedType) ? PointerIO.getClass(((ParameterizedType)field.valueType).getActualTypeArguments()[0]) : null;
                field.byteLength = 4;
                //field.callIO = CallIO.Utils.createPointerCallIO(field.valueClass, field.valueType);
            } else if (TypedPointer.class.isAssignableFrom(field.valueClass)) {
                field.desc.nativeTypeOrPointerTargetType = field.valueType;
                field.byteLength = Pointer.SIZE;
                //field.callIO = CallIO.Utils.createPointerCallIO(field.valueClass, field.valueType);
            } else if (Pointer.class.isAssignableFrom(field.valueClass)) {
                field.desc.nativeTypeOrPointerTargetType = (field.valueType instanceof ParameterizedType) ? ((ParameterizedType)field.valueType).getActualTypeArguments()[0] : null;
                field.byteLength = Pointer.SIZE;
                //field.callIO = CallIO.Utils.createPointerCallIO(field.valueClass, field.valueType);
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
                
                //field.refreshableFieldIndex = refreshableFieldCount++;
            } else if (field.valueClass.isArray() && field.valueClass.getComponentType().isPrimitive()) {
				field.byteLength = primTypeLength(field.valueClass.getComponentType());
			} else {
                //throw new UnsupportedOperationException("Field type " + field.valueClass.getName() + " not supported yet");
                if (!field.isByValue)
                    field.byteLength = Pointer.SIZE;
                else {
                    StructIO io = StructIO.getInstance(field.valueClass, field.valueType);
                    int s = io.getStructSize();
                    if (s > 0)
                        field.byteLength = s;
                    else
                        throw new UnsupportedOperationException("Field type " + field.valueClass.getName() + " not supported yet");
                }
            }   

            if (field.desc.bitLength < 0) {
				// Align fields as appropriate
				if (cumulativeBitOffset != 0) {
					cumulativeBitOffset = 0;
					structSize++;
				}
                int fieldAlignment = field.byteLength;
				structAlignment = Math.max(structAlignment, fieldAlignment);
                structSize = alignSize(structSize, fieldAlignment);
			}
			field.desc.byteOffset = structSize;
            field.desc.bitOffset = cumulativeBitOffset;
            //field.index = fieldCount++;

			if (field.desc.bitLength >= 0) {
				field.byteLength = (field.desc.bitLength >>> 3) + ((field.desc.bitLength & 7) != 0 ? 1 : 0);
                cumulativeBitOffset += field.desc.bitLength;
				structSize += cumulativeBitOffset >>> 3;
				cumulativeBitOffset &= 7;
			} else {
                structSize += field.desc.arrayLength * field.byteLength;
			}
        }
        if (cumulativeBitOffset > 0)
			structSize = alignSize(structSize + 1, structAlignment);
        else if (structSize > 0)
            structSize = alignSize(structSize, structAlignment);

        List<FieldDesc> filtered = new ArrayList<FieldDesc>();
        for (FieldDecl fio : list)
            if (fio.declaringClass != null && fio.declaringClass.equals(structClass))
                filtered.add(fio.desc);
        
		return filtered.toArray(new FieldDesc[filtered.size()]);
	}

	public final <T> Pointer<T> getPointerField(StructObject struct, int fieldIndex) {
        FieldDesc fd = fields[fieldIndex];
		Pointer<T> p = struct.peer.getPointer(fd.byteOffset, fd.nativeTypeOrPointerTargetType);
        if (fd.arrayLength != 1)
            p = p.validElements(fd.arrayLength);
        return p;
	}
	
	public final <T> void setPointerField(StructObject struct, int fieldIndex, Pointer<T> value) {
		FieldDesc fd = fields[fieldIndex];
		struct.peer.setPointer(fd.byteOffset, value);
	}
	
	public final <T extends TypedPointer> T getTypedPointerField(StructObject struct, int fieldIndex) {
		FieldDesc fd = fields[fieldIndex];
		PointerIO<T> pio = PointerIO.getInstance(fd.nativeTypeOrPointerTargetType);
		return pio.castTarget(struct.peer.getSizeT(fd.byteOffset));
	}
	public final <O extends NativeObject> O getNativeObjectField(StructObject struct, int fieldIndex) {
		FieldDesc fd = fields[fieldIndex];
		return (O)struct.peer.getNativeObject(fd.byteOffset, fd.nativeTypeOrPointerTargetType);
	}

	public final <E extends Enum<E>> ValuedEnum<E> getEnumField(StructObject struct, int fieldIndex) {
        FieldDesc fd = fields[fieldIndex];
		return FlagSet.fromValue(struct.peer.getInt(fd.byteOffset), (Class<E>)fd.nativeTypeOrPointerTargetType);
	}
	
	public final void setEnumField(StructObject struct, int fieldIndex, ValuedEnum<?> value) {
		FieldDesc fd = fields[fieldIndex];
		struct.peer.setInt(fd.byteOffset, (int)value.value());
	}
	
#foreach ($prim in $primitives)
    public final void set${prim.CapName}Field(StructObject struct, int fieldIndex, ${prim.Name} value) {
		FieldDesc fd = fields[fieldIndex];
		struct.peer.set${prim.CapName}(fd.byteOffset, value);
	}
	public final ${prim.Name} get${prim.CapName}Field(StructObject struct, int fieldIndex) {
		FieldDesc fd = fields[fieldIndex];
		return struct.peer.get${prim.CapName}(fd.byteOffset);
	}
#end	

#foreach ($sizePrim in ["SizeT", "CLong"])
    public final void set${sizePrim}Field(StructObject struct, int fieldIndex, long value) {
		FieldDesc fd = fields[fieldIndex];
		struct.peer.set${sizePrim}(fd.byteOffset, value);
	}
	public final long get${sizePrim}Field(StructObject struct, int fieldIndex) {
		FieldDesc fd = fields[fieldIndex];
		return struct.peer.get${sizePrim}(fd.byteOffset);
	}
#end
}
