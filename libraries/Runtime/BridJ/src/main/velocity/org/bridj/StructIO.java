package org.bridj;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import org.bridj.CallIO.NativeObjectHandler;
import org.bridj.util.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Member;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.nio.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bridj.ann.Virtual;
import org.bridj.ann.Array;
import org.bridj.ann.Union;
import org.bridj.ann.Bits;
import org.bridj.ann.Field;
import org.bridj.ann.Struct;
import org.bridj.ann.Alignment;
import static org.bridj.Pointer.*;

/**
 * Representation of a C struct's memory layout, built thanks to the annotations found in the Java bindings.<br>
 * End-users should not use this class, it's used by runtimes.<br>
 * Annotations currently used are {@link org.bridj.ann.Virtual}, {@link org.bridj.ann.Array}, {@link org.bridj.ann.Bits}, {@link org.bridj.ann.Field}, {@link org.bridj.ann.Alignment} and soon {@link org.bridj.ann.Struct}
 * @author ochafik
 */
public class StructIO {

    static Map<Type, StructIO> structIOs = new HashMap<Type, StructIO>();

    /**
     * Interface for type customizers that can be used to perform platform-specific type adjustments or other hacks.<br>
     * A type customizer can be specified with {@link Struct#customizer() }.<br>
     * Each implementation must have a default constructor, and an unique instance of each implementation class will be cached by {@link StructIO#getCustomizer(java.lang.Class) }.
     */
    public interface Customizer {
    	StructIO process(StructIO io);
    }
	static Customizer dummyCustomizer = new Customizer() {
    	public StructIO process(StructIO io) { return io; }
    };
    static Map<Class, Customizer> customizers = new HashMap<Class, Customizer>();
    static synchronized Customizer getCustomizer(Class<?> structClass) {
    	Customizer c = customizers.get(structClass);
    	if (c == null) {
    		Struct s = structClass.getAnnotation(Struct.class);
			if (s != null) {
				Class<? extends Customizer> customizerClass = s.customizer();
				if (customizerClass != null && customizerClass != Customizer.class) {
					try {
						c = customizerClass.newInstance();
					} catch (Throwable th) {
						throw new RuntimeException("Failed to create customizer of class " + customizerClass.getName() + " for struct class " + structClass.getName() + " : " + th, th);
					}
				}
			}
			if (c == null)
				c = dummyCustomizer;
			customizers.put(structClass, c);
    	}
    	return c;
    }
    public static StructIO getInstance(Class structClass, Type structType) {
    	synchronized (structIOs) {
            StructIO io = structIOs.get(structType == null ? structClass : structType);
            if (io == null) {
            	io = new StructIO(structClass, structType);
            	io = getCustomizer(structClass).process(io);
            	if (io != null)
            		registerStructIO(structClass, structType, io);
            }
            return (StructIO)io;
        }
    }

    public static synchronized StructIO registerStructIO(Class structClass, Type structType, StructIO io) {
        structIOs.put(structType, io);
        return io;
    }

    /**
     * Internal metadata on a struct field
     */
    public static class FieldDesc {
		public long alignment = -1;
        public long byteOffset = -1, byteLength = -1;
		public long bitOffset, bitLength = -1;
        public long arrayLength = 1;
        public boolean isArray, isNativeObject;
        public Type nativeTypeOrPointerTargetType;
        public java.lang.reflect.Field field;
        Type valueType;
        Method getter;
        String name;
        boolean isCLong, isSizeT;
		
        public void offset(long bytes) {
			byteOffset += bytes;
		}
        @Override
        public String toString() {
			return "Field(byteOffset = " + byteOffset + ", byteLength = " + byteLength + ", bitOffset = " + bitOffset + ", bitLength = " + bitLength + (nativeTypeOrPointerTargetType == null ? "" : ", ttype = " + nativeTypeOrPointerTargetType) + ")";
        }
	}
	protected static class FieldDecl {
		final FieldDesc desc = new FieldDesc();
		Method setter;
		long index = -1, unionWith = -1;//, byteOffset = -1;
		Class<?> valueClass;
        Class<?> declaringClass;
        boolean isBitField;

        @Override
        public String toString() {
            return desc.name + " (index = " + index + (unionWith < 0 ? "" : ", unionWith = " + unionWith) + ", desc = " + desc + ")";
        }
    }
	
    static class SolidRanges {
    		long[] offsets, lengths;
    		static class Builder {
    			List<Long> offsets = new ArrayList<Long>(), lengths = new ArrayList<Long>();
    			long lastOffset = -1, nextOffset = 0;
    			int count;
    			void add(FieldDesc f) {
    				long offset = f.byteOffset;
    				long length = f.byteLength;
    				
    				if (offset == lastOffset) {
    					lengths.set(count - 1, Math.max(lengths.get(count - 1), length));	
    				} else if (offset == nextOffset && count != 0) {
    					lengths.set(count - 1, lengths.get(count - 1) + length);
    				} else {
    					offsets.add(offset);
    					lengths.add(length);
    					count++;
    				}
    				lastOffset = offset;
    				nextOffset = offset + length;
    			}
    			SolidRanges toSolidRanges() {
    				SolidRanges r = new SolidRanges();
    				r.offsets = new long[count];
    				r.lengths = new long[count];
    				for (int i = 0; i < count; i++) {
    					r.offsets[i] = offsets.get(i);
    					r.lengths[i] = lengths.get(i);
    				}
    				return r;
    			}
		}
	
    }
	protected PointerIO<?> pointerIO;
	protected volatile FieldDesc[] fields;
	private long structSize = -1;
    private long structAlignment = -1;
	protected final Class<?> structClass;
	protected final Type structType;
	protected boolean hasFieldFields;

	public void prependBytes(long bytes) {
		build();
		for (FieldDesc field : fields) {
			field.offset(bytes);
		}
		structSize += bytes;
	}
	public void appendBytes(long bytes) {
		build();
		structSize += bytes;
	}
	public void setFieldOffset(String fieldName, long fieldOffset, boolean propagateChanges) {
		build();

		long propagatedOffset = 0;
		for (FieldDesc field : fields) {
			if (field.name.equals(fieldName)) {
				propagatedOffset = fieldOffset - field.byteOffset;
				field.offset(propagatedOffset);
				if (!propagateChanges)
					return;
				structSize += propagatedOffset;
			} else if (propagateChanges)
				field.offset(propagatedOffset);
			return;
		}
	}
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
	
	@Override
	public String toString() {
		return "StructIO(" + (structType instanceof Class ? structClass.getName() : structType.toString()) + ")";
	}
	
	public synchronized PointerIO<?> getPointerIO() {
		if (pointerIO == null)
			pointerIO = new CommonPointerIOs.StructPointerIO(this);
			
		return pointerIO;
	}

    protected long alignSize(long size, long alignment) {
        if (alignment > 1) {
            long r = size % alignment;
            if (r != 0)
                size += alignment - r;
        }
        return size;
    }


	/// Call whenever an instanceof a struct that depends on that StructIO is created
	void build() {
		if (fields == null) {
			synchronized (this) {
				if (fields == null) {
					fields = computeStructLayout();
					if (BridJ.debug)
						BridJ.log(Level.INFO, describe());
				}
			}
		}
	}
    
	public final long getStructSize() {
		build();
		return structSize;
	}

    public final long getStructAlignment() {
		build();
		return structAlignment;
	}
	
	/**
     * Orders the fields to match the actual structure layout
     */
	protected void orderFields(List<FieldDecl> fields) {
		Collections.sort(fields, new Comparator<FieldDecl>() {

            //@Override
            public int compare(FieldDecl o1, FieldDecl o2) {
                long d = o1.index - o2.index;
                if (d != 0)
                    return d < 0 ? -1 : d == 0 ? 0 : 1;

                if (o1.declaringClass.isAssignableFrom(o2.declaringClass))
                    return -1;
                if (o2.declaringClass.isAssignableFrom(o1.declaringClass))
                    return 1;
                
                throw new RuntimeException("Failed to order fields " + o2.desc.name + " and " + o2.desc.name);
            }

        });
	}
	
    protected boolean acceptFieldGetter(Member member, boolean getter) {
        if ((member instanceof Method) && ((Method)member).getParameterTypes().length != (getter ? 0 : 1))
            return false;
        
        if (((AnnotatedElement)member).getAnnotation(Field.class) == null)
            return false;

        int modifiers = member.getModifiers();
        
        return //Modifier.isNative(modifiers) && 
                !Modifier.isStatic(modifiers);// &&
                //!forbiddenGetterNames.contains(method.getName());
    }

    protected FieldDecl createFieldDecl(java.lang.reflect.Field getter) {
        FieldDecl field = createFieldDecl((Member)getter);
        field.desc.field = getter;
        field.desc.valueType = getter.getGenericType();
        field.valueClass = getter.getType();
        return field;
    }
    protected FieldDecl createFieldDecl(Method getter) {
        FieldDecl field = createFieldDecl((Member)getter);
        field.desc.getter = getter;
        field.desc.valueType = getter.getGenericReturnType();
        field.valueClass = getter.getReturnType();
        return field;
    }
    	protected FieldDecl createFieldDecl(Member member) {
        FieldDecl field = new FieldDecl();
        field.declaringClass = member.getDeclaringClass();

        String name = member.getName();
        if (name.matches("get[A-Z].*"))
            name = Character.toLowerCase(name.charAt(3)) + name.substring(4);

        field.desc.name = name;

        AnnotatedElement getter = (AnnotatedElement)member;
        Field fil = getter.getAnnotation(Field.class);
        Bits bits = getter.getAnnotation(Bits.class);
        Array arr = getter.getAnnotation(Array.class);
        if (fil != null) {
            field.index = fil.value();
            //field.byteOffset = fil.offset();
            field.unionWith = fil.unionWith();
        }
        if (field.unionWith < 0 && field.declaringClass.getAnnotation(Union.class) != null)
        		field.unionWith = 0;
        	
        if (bits != null)
            field.desc.bitLength = bits.value();
        if (arr != null) {
            long length = 1;
            for (long dim : arr.value())
                length *= dim;
            field.desc.arrayLength = length;
            field.desc.isArray = true;
        }
        field.desc.isCLong = getter.getAnnotation(org.bridj.ann.CLong.class) != null;
        field.desc.isSizeT = getter.getAnnotation(org.bridj.ann.Ptr.class) != null;
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
        
        int nFieldFields = 0;
        for (java.lang.reflect.Field field : structClass.getFields()) {
            if (acceptFieldGetter(field, true)) {
                FieldDecl io = createFieldDecl(field);
                if (io != null) {             
                    list.add(io);
                    nFieldFields++;
                }
            }
        }
		if (nFieldFields > 0)
			BridJ.log(Level.WARNING, "Struct " + structClass.getName() + " has " + nFieldFields + " struct fields implemented as Java fields, which won't give the best performance and might require counter-intuitive calls to BridJ.readFromNative / .writeToNative. Please consider using JNAerator to generate your struct instead.");
		
		return list;
	}
	
	protected static int primTypeLength(Class<?> primType) {
        if (primType == Integer.class || primType == int.class)
            return 4;
        else if(primType == Long.class || primType == long.class)
            return 8;
        else if(primType == Short.class || primType == short.class)
            return 2;
        else if(primType == Byte.class || primType == byte.class)
            return 1;
        else if(primType == Character.class || primType == char.class)
            return 2;
        else if(primType == Boolean.class || primType == boolean.class) {
            /*
            BOOL is int, not C++'s bool !
            if (Platform.isWindows())
				return 4;
			else
			*/ 
			return 1;
        } else if(primType == Float.class || primType == float.class)
            return 4;
        else if(primType == Double.class || primType == double.class)
            return 8;
        else if(Pointer.class.isAssignableFrom(primType))
            return Pointer.SIZE;
        else
			throw new UnsupportedOperationException("Field type " + primType.getName() + " not supported yet");

	}
	protected FieldDesc[] computeStructLayout() {
		List<FieldDecl> list = listFields();
		orderFields(list);
		
		Map<Pair<Class<?>, Long>, List<FieldDecl>> fieldsMap = new LinkedHashMap<Pair<Class<?>, Long>, List<FieldDecl>>();
        for (FieldDecl field : list) {
        		if (field.index < 0)
        			throw new RuntimeException("Negative field index not allowed for field " + field.desc.name);
        		
        		long index = field.unionWith >= 0 ? field.unionWith : field.index;
        		Pair<Class<?>, Long> key = new Pair<Class<?>, Long>(field.declaringClass, index);
        		List<FieldDecl> siblings = fieldsMap.get(key);
        		if (siblings == null)
        			fieldsMap.put(key, siblings = new ArrayList<FieldDecl>());
        		siblings.add(field);
        }
        	
        	Alignment alignment = structClass.getAnnotation(Alignment.class);
        structAlignment = alignment != null ? alignment.value() : 1; //TODO get platform default alignment

        structSize = 0;
        if (isVirtual()) {
        		structSize += Pointer.SIZE;
        		if (Pointer.SIZE >= structAlignment)
        			structAlignment = Pointer.SIZE;
			/*FieldDecl d = new FieldDecl();
			d.byteLength = Pointer.SIZE;
			d.valueType = d.valueClass = Pointer.class;
			d.name = "vtablePtr";
			list.add(0, d);*/
		}

        int cumulativeBitOffset = 0;
        
        List<FieldDesc> aggregatedDescs = new ArrayList<FieldDesc>();
        //List<Type> declaringTypes = new ArrayList<Type>();
        for (List<FieldDecl> fieldGroup : fieldsMap.values()) {
    		FieldDesc aggregatedDesc = new FieldDesc();
    		aggregatedDescs.add(aggregatedDesc);
    		boolean isMultiFields = fieldGroup.size() > 1;
    		for (FieldDecl field : fieldGroup) {
                if (field.valueClass.isArray())
                	throw new RuntimeException("Struct fields cannot be array types : please use a combination of Pointer and @Array (for instance, an int[10] is a @Array(10) Pointer<Integer>).");
				if (field.valueClass.isPrimitive()) {
					if (field.desc.isCLong)
						field.desc.byteLength = CLong.SIZE;
					else if (field.desc.isSizeT)
						field.desc.byteLength = SizeT.SIZE;
					else
						field.desc.byteLength = primTypeLength(field.valueClass);
				} else if (field.valueClass == CLong.class) {
					field.desc.byteLength = CLong.SIZE;
				} else if (field.valueClass == SizeT.class) {
					field.desc.byteLength = SizeT.SIZE;
				} else if (StructObject.class.isAssignableFrom(field.valueClass)) {
					field.desc.nativeTypeOrPointerTargetType = field.desc.valueType;
					StructIO io = StructIO.getInstance(field.valueClass, field.desc.valueType);		
					field.desc.byteLength = io.getStructSize();				
					field.desc.alignment = io.getStructAlignment();
					field.desc.isNativeObject = true;
				} else if (ValuedEnum.class.isAssignableFrom(field.valueClass)) {
					field.desc.nativeTypeOrPointerTargetType = (field.desc.valueType instanceof ParameterizedType) ? PointerIO.getClass(((ParameterizedType)field.desc.valueType).getActualTypeArguments()[0]) : null;
					Class c = PointerIO.getClass(field.desc.nativeTypeOrPointerTargetType);
					if (IntValuedEnum.class.isAssignableFrom(c))
						field.desc.byteLength = 4;
					else
						throw new RuntimeException("Enum type unknown : " + c);
					//field.callIO = CallIO.Utils.createPointerCallIO(field.valueClass, field.desc.valueType);
				} else if (TypedPointer.class.isAssignableFrom(field.valueClass)) {
					field.desc.nativeTypeOrPointerTargetType = field.desc.valueType;
                    if (field.desc.isArray)
                        throw new RuntimeException("Typed pointer field cannot be an array : " + field.desc.name);
                    field.desc.byteLength = Pointer.SIZE;
					//field.callIO = CallIO.Utils.createPointerCallIO(field.valueClass, field.desc.valueType);
				} else if (Pointer.class.isAssignableFrom(field.valueClass)) {
					Type tpe = (field.desc.valueType instanceof ParameterizedType) ? ((ParameterizedType)field.desc.valueType).getActualTypeArguments()[0] : null;
                    if (!(tpe instanceof WildcardType) && !(tpe instanceof TypeVariable))
							field.desc.nativeTypeOrPointerTargetType = tpe;
					if (field.desc.isArray) {
                        field.desc.byteLength = BridJ.sizeOf(field.desc.nativeTypeOrPointerTargetType);
                    } else
                        field.desc.byteLength = Pointer.SIZE;
					//field.callIO = CallIO.Utils.createPointerCallIO(field.valueClass, field.desc.valueType);
				} else if (Buffer.class.isAssignableFrom(field.valueClass)) {
					if (field.valueClass == IntBuffer.class)
						field.desc.byteLength = 4;
					else if (field.valueClass == LongBuffer.class)
						field.desc.byteLength = 8;
					else if (field.valueClass == ShortBuffer.class)
						field.desc.byteLength = 2;
					else if (field.valueClass == ByteBuffer.class)
						field.desc.byteLength = 1;
					else if (field.valueClass == FloatBuffer.class)
						field.desc.byteLength = 4;
					else if (field.valueClass == DoubleBuffer.class)
						field.desc.byteLength = 8;
					else
						throw new UnsupportedOperationException("Field array type " + field.valueClass.getName() + " not supported yet");
					
				} else if (field.valueClass.isArray() && field.valueClass.getComponentType().isPrimitive()) {
					field.desc.byteLength = primTypeLength(field.valueClass.getComponentType());
				} else {
					//throw new UnsupportedOperationException("Field type " + field.valueClass.getName() + " not supported yet");
					StructIO io = StructIO.getInstance(field.valueClass, field.desc.valueType);
					long s = io.getStructSize();
					if (s > 0)
						field.desc.byteLength = s;
					else
						throw new UnsupportedOperationException("Field type " + field.valueClass.getName() + " not supported yet");
				}
				
				long length = field.desc.arrayLength * field.desc.byteLength;
				if (length >= aggregatedDesc.byteLength)
					aggregatedDesc.byteLength = length;
				
				aggregatedDesc.alignment = Math.max(
					aggregatedDesc.alignment, 
					field.desc.alignment >= 0 ?
						field.desc.alignment :
						field.desc.byteLength
				);
				
				structAlignment = Math.max(structAlignment, aggregatedDesc.alignment);
				
				if (field.desc.bitLength >= 0) {
					if (isMultiFields)
						throw new RuntimeException("No support for bit fields unions yet !");
					aggregatedDesc.bitLength = field.desc.bitLength;
					aggregatedDesc.byteLength = (aggregatedDesc.bitLength >>> 3) + ((aggregatedDesc.bitLength & 7) != 0 ? 1 : 0);
				}
				
				//if (!isMultiFields) {
				//aggregatedDesc.isArray = field.desc.isArray;
				//aggregatedDesc.isNativeObject = field.desc.isNativeObject; 
				//aggregatedDesc.nativeTypeOrPointerTargetType = field.desc.nativeTypeOrPointerTargetType;
				//aggregatedDesc.name = field.desc.name;
				//aggregatedDesc.valueType = field.desc.valueType;
				//}
				//if (fieldDeclaringType == null)
				//	fieldDeclaringType = field.declaringClass;
				//else if (!fieldDeclaringType.equals(field.declaringClass))
				//	throw new RuntimeException("Fields in the same field group must pertain to the same declaring class : " + fieldGroup);
				
			}
			//declaringTypes.add(fieldDeclaringType);
		}
		int iAggregatedDesc = 0;
		//Type lastFieldDeclaringType = null;
		for (List<FieldDecl> fieldGroup : fieldsMap.values()) {
			//Type fieldDeclaringType = declaringTypes.get(iAggregatedDesc);
			FieldDesc aggregatedDesc = aggregatedDescs.get(iAggregatedDesc++);

			//if (lastFieldDeclaringType != null && !fieldDeclaringType.equals(lastFieldDeclaringType)) {
			//	StructIO io = StructIO.getInstance(Utils.getClass(lastFieldDeclaringType), lastFieldDeclaringType);
			//	structSize = alignSize(structSize, io.getStructSize());
			//}
			//lastFieldDeclaringType = fieldDeclaringType;
            if (aggregatedDesc.bitLength < 0) {
				// Align fields as appropriate
				if (cumulativeBitOffset != 0) {
					cumulativeBitOffset = 0;
					structSize++;
				}
                //structAlignment = Math.max(structAlignment, aggregatedDesc.alignment);
                structSize = alignSize(structSize, aggregatedDesc.alignment);
			}
			long 
				fieldByteOffset = structSize, 
				fieldBitOffset = cumulativeBitOffset;
			
			//field.index = fieldCount++;

			if (aggregatedDesc.bitLength >= 0) {
				//fieldByteLength = (aggregatedDesc.bitLength >>> 3) + ((aggregatedDesc.bitLength & 7) != 0 ? 1 : 0);
                cumulativeBitOffset += aggregatedDesc.bitLength;
				structSize += cumulativeBitOffset >>> 3;
				cumulativeBitOffset &= 7;
			} else {
                structSize += aggregatedDesc.byteLength;
			}
			
			for (FieldDecl field : fieldGroup) {
				//field.desc.byteLength = fieldByteLength;
				field.desc.byteOffset = fieldByteOffset;
				field.desc.bitOffset = fieldBitOffset;
			}
        }
        if (cumulativeBitOffset > 0)
			structSize = alignSize(structSize + 1, structAlignment);
        else if (structSize > 0)
            structSize = alignSize(structSize, structAlignment);

        List<FieldDesc> filtered = new ArrayList<FieldDesc>();
        for (FieldDecl fio : list)
            //if (fio.declaringClass != null && fio.declaringClass.equals(structClass))
            filtered.add(fio.desc);
        
            SolidRanges.Builder b = new SolidRanges.Builder();
            for (FieldDesc f : filtered) {
            		b.add(f);
            		hasFieldFields = hasFieldFields || f.field != null;
            }
            	
            	solidRanges = b.toSolidRanges();
		/*System.out.println();
		System.out.println("FILTERED(" + structClass.getName() + ") = ");
		for (FieldDecl field : list)
			System.out.println("\t" + field);*/
		return filtered.toArray(new FieldDesc[filtered.size()]);
	}

	SolidRanges solidRanges;
	
	public boolean equal(StructObject a, StructObject b) {
		return compare(a, b) == 0;	
	}
	public int compare(StructObject a, StructObject b) {
		Pointer<StructObject> pA = pointerTo(a), pB = pointerTo(b);
		if (pA == null || pB == null)
			return pA != null ? 1 : pB != null ? -1 : 0;
		
		long[] offsets = solidRanges.offsets, lengths = solidRanges.lengths;
		for (int i = 0, n = offsets.length; i < n; i++) {
			long offset = offsets[i], length = lengths[i];
			int cmp = pA.compareBytesAtOffset(offset, pB, offset, length);
			if (cmp != 0)
				return cmp;	
		}
    		return 0;
	}
	
	public final String describe(StructObject struct) {
		StringBuilder b = new StringBuilder();
		b.append(describe(structType)).append(" { ");
		for (FieldDesc fd : fields) {
			b.append("\n\t").append(fd.name).append(" = ");
			try {
				Object value;
				if (fd.getter != null)
					value = fd.getter.invoke(struct);
				else
					value = fd.field.get(struct);
				
				if (value instanceof String)
					b.append('"').append(value.toString().replaceAll("\"", "\\\"")).append('"');
				else if (value instanceof Character)
					b.append('\'').append(value).append('\'');
				else if (value instanceof NativeObject) {
					String d = BridJ.describe((NativeObject)value);
					b.append(d.replaceAll("\n", "\n\t"));
				} else
					b.append(value);
			} catch (Throwable th) {
				if (BridJ.debug)
					th.printStackTrace();
				b.append("?");
			}
			b.append("; ");
		}
		b.append("\n}");
		return b.toString();
	}
	static String describe(Type t) {
		if (t == null)
    			return "?";
    		if (t instanceof Class)
			return ((Class)t).getSimpleName();
		return t.toString().
			replaceAll("\\bjava\\.lang\\.", "").
			replaceAll("\\borg\\.bridj\\.cpp\\.com\\.", "").
			replaceAll("\\borg\\.bridj\\.Pointer\\b", "Pointer");
			
	}
    
	public final String describe() {
		StringBuilder b = new StringBuilder();
		b.append("// ");
		b.append("size = ").append(structSize).append(", ");
		b.append("alignment = ").append(structAlignment);
		b.append("\nstruct ");
		b.append(describe(structType)).append(" { ");
		for (int iField = 0, nFields = fields.length; iField < nFields; iField++) {
			FieldDesc fd = fields[iField];
			b.append("\n\t");
			b.append("@Field(").append(iField).append(") ");
			if (fd.isCLong)
				b.append("@CLong ");
			else if (fd.isSizeT)
				b.append("@Ptr ");
			b.append(describe(fd.valueType)).append(" ").append(fd.name).append("; ");
			
			b.append("// ");
			b.append("offset = ").append(fd.byteOffset).append(", ");
			b.append("length = ").append(fd.byteLength).append(", ");
			if (fd.bitOffset != 0)
				b.append("bitOffset = ").append(fd.bitOffset).append(", ");
			if (fd.bitLength != -1)
				b.append("bitLength = ").append(fd.bitLength).append(", ");
			if (fd.arrayLength != 1)
				b.append("arrayLength = ").append(fd.arrayLength);//.append(", ");
		}
		b.append("\n}");
		return b.toString();
	}
	
	public final void writeFieldsToNative(StructObject struct) {
            if (!hasFieldFields)
                return;
            try {
                for (FieldDesc fd : fields) {
                    if (fd.field == null)
                        continue;
				
                    if (fd.isArray)
                        continue;

                    Object value = fd.field.get(struct);
                    if (value instanceof NativeObject) {//fd.isNativeObject) {
                    		if (value != null) 
                    			BridJ.writeToNative((NativeObject)value);
                    		continue;
                    }
                    Pointer ptr = struct.peer.offset(fd.byteOffset);
                    Type tpe = fd.isNativeObject || fd.isArray ? fd.nativeTypeOrPointerTargetType : fd.field.getGenericType();
                    ptr = ptr.as(tpe);
                    ptr.set(value);
                }
            } catch (Throwable th) {
                throw new RuntimeException("Unexpected error while writing fields from struct " + Utils.toString(structType) + " (" + pointerTo(struct) + ")", th);
            }
	}
	public final void readFieldsFromNative(StructObject struct) {
            if (!hasFieldFields)
                return;
            try {
                for (FieldDesc fd : fields) {
                    if (fd.field == null)
                        continue;

                    Pointer ptr = struct.peer.offset(fd.byteOffset);
                    Type tpe = fd.isNativeObject || fd.isArray ? fd.nativeTypeOrPointerTargetType : fd.field.getGenericType();
                    ptr = ptr.as(tpe);
                    Object value;
                    if (fd.isArray) {
                        ptr = ptr.validElements(fd.arrayLength);
                        value = ptr;
                    } else {
                        value = ptr.get();
                    }
                    fd.field.set(struct, value);
                    
                    if (value instanceof NativeObject) {//if (fd.isNativeObject) {
                    		if (value != null)
                    			BridJ.readFromNative((NativeObject)value);
                    }
                }
            } catch (Throwable th) {
                throw new RuntimeException("Unexpected error while reading fields from struct " + Utils.toString(structType) + " (" + pointerTo(struct) + ") : " + th, th);
            }
	}
	public final <T> Pointer<T> getPointerField(StructObject struct, int fieldIndex) {
        FieldDesc fd = fields[fieldIndex];
        Pointer<T> p;
        if (fd.isArray) {
        		p = struct.peer.offset(fd.byteOffset).as(fd.nativeTypeOrPointerTargetType);
        		p = p.validElements(fd.arrayLength);
        } else {
        		p = struct.peer.getPointerAtOffset(fd.byteOffset, fd.nativeTypeOrPointerTargetType);
        }
		return p;
	}
	
	public final <T> void setPointerField(StructObject struct, int fieldIndex, Pointer<T> value) {
		FieldDesc fd = fields[fieldIndex];
		struct.peer.setPointerAtOffset(fd.byteOffset, value);
	}
	
	public final <T extends TypedPointer> T getTypedPointerField(StructObject struct, int fieldIndex) {
		FieldDesc fd = fields[fieldIndex];
		PointerIO<T> pio = PointerIO.getInstance(fd.nativeTypeOrPointerTargetType);
		return pio.castTarget(struct.peer.getSizeTAtOffset(fd.byteOffset));
	}
	public final <O extends NativeObject> O getNativeObjectField(StructObject struct, int fieldIndex) {
		FieldDesc fd = fields[fieldIndex];
		return (O)struct.peer.offset(fd.byteOffset).getNativeObject(fd.nativeTypeOrPointerTargetType);
	}

	public final <E extends Enum<E>> ValuedEnum<E> getEnumField(StructObject struct, int fieldIndex) {
        FieldDesc fd = fields[fieldIndex];
		return FlagSet.fromValue(struct.peer.getIntAtOffset(fd.byteOffset), (Class<E>)fd.nativeTypeOrPointerTargetType);
	}
	
	public final void setEnumField(StructObject struct, int fieldIndex, ValuedEnum<?> value) {
		FieldDesc fd = fields[fieldIndex];
		struct.peer.setIntAtOffset(fd.byteOffset, (int)value.value());
	}
	
#foreach ($prim in $primitives)
    public final void set${prim.CapName}Field(StructObject struct, int fieldIndex, ${prim.Name} value) {
		FieldDesc fd = fields[fieldIndex];
		struct.peer.set${prim.CapName}AtOffset(fd.byteOffset, value);
	}
	public final ${prim.Name} get${prim.CapName}Field(StructObject struct, int fieldIndex) {
		FieldDesc fd = fields[fieldIndex];
		return struct.peer.get${prim.CapName}AtOffset(fd.byteOffset);
	}
#end	

#foreach ($sizePrim in ["SizeT", "CLong"])
    public final void set${sizePrim}Field(StructObject struct, int fieldIndex, long value) {
		FieldDesc fd = fields[fieldIndex];
		struct.peer.set${sizePrim}AtOffset(fd.byteOffset, value);
	}
	public final long get${sizePrim}Field(StructObject struct, int fieldIndex) {
		FieldDesc fd = fields[fieldIndex];
		return struct.peer.get${sizePrim}AtOffset(fd.byteOffset);
	}
#end
}
