package com.bridj;
import com.bridj.ann.*;

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
import com.bridj.StructIO;
import static com.bridj.StructIO.*;

class StructFieldsIO {

	
	public static Type getFieldType(int fieldIndex, StructObject struct) {
		return struct.io.fields[fieldIndex].valueType;
	}
    
    public static Pointer getPointerField(int fieldIndex, StructObject struct) {
        FieldIO field = struct.io.fields[fieldIndex];
        assert !field.isBitField;
        assert Pointer.class.isAssignableFrom(field.valueClass);

        return Pointer.getPeer(struct).getPointer(field.byteOffset);
    }
    public static void setPointerField(int fieldIndex, StructObject struct, Pointer p) {
        FieldIO field = struct.io.fields[fieldIndex];
        assert !field.isBitField;
        assert Pointer.class.isAssignableFrom(field.valueClass);

        Pointer.getPeer(struct).setPointer(field.byteOffset, p);
    }
    /*
	
    public void setRefreshableField(int fieldIndex, StructObject struct, PointerRefreshable value) {
        FieldIO field = struct.io.fields[fieldIndex];
        assert !field.isBitField;
        if (field.valueClass == Pointer.class) {
            Pointer.getPeer(struct).setPointer(field.byteOffset, (Pointer)value);
            return;
        }
        PointerRefreshable ref = (PointerRefreshable)value;
        struct.refreshableFields[field.refreshableFieldIndex] = ref;
        Pointer.getPeer(struct).setPointer(field.byteOffset, ref.getReference());
    }

    public <F extends PointerRefreshable> F getRefreshableField(int fieldIndex, StructObject struct, Class<F> fieldClass) {
        FieldIO field = struct.io.fields[fieldIndex];
        assert fieldClass.equals(field.valueClass);
        try {
            F sf = (F)struct.refreshableFields[field.refreshableFieldIndex];
            if (sf == null) 
                struct.refreshableFields[field.refreshableFieldIndex] = sf = fieldClass.newInstance();
            
            sf.setPointer(field.isByValue ? Pointer.getPeer(struct).offset(field.byteOffset) : Pointer.getPeer(struct).getPointer(field.byteOffset));
            return sf;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to instantiate struct of type " + fieldClass.getName(), ex);
        }
	}*/

    public static <F extends NativeObject> void setNativeObject(int fieldIndex, StructObject struct, Class<F> fieldClass, F fieldValue) {
        FieldIO field = struct.io.fields[fieldIndex];
        assert fieldClass.equals(field.valueClass);
        if (field.isByValue) {
            if (fieldValue == null)
                throw new IllegalArgumentException("By-value struct struct.io.fields cannot be set to null");
            // Nothing to do : by-value struct already wrote its feeds as appropriate
        } else {
            struct.refreshableFields[field.refreshableFieldIndex] = fieldValue;
            Pointer.getPeer(struct).setPointer(field.byteOffset, Pointer.getPeer(fieldValue));
        }
	}

    public static <F extends NativeObject> F getNativeObjectField(int fieldIndex, StructObject struct, Class<F> fieldClass) {
        FieldIO field = struct.io.fields[fieldIndex];
        assert fieldClass.equals(field.valueClass);
        F fieldValue = (F)struct.refreshableFields[field.refreshableFieldIndex];
        if (fieldValue == null) {
            try {
            	struct.refreshableFields[field.refreshableFieldIndex] = fieldValue = Pointer.getPeer(struct).offset(field.byteOffset).toNativeObject(fieldClass);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to instantiate struct of type " + fieldClass.getName(), ex);
            }
        }
        
        //fieldValue.setPointer(Pointer.getPeer(struct).offset(field.byteOffset));
        return fieldValue;
	}

	/*
    public <F extends Struct<F>> Array<F> getStructArrayField(int fieldIndex, StructObject struct, Class<F> fieldClass) {
        FieldIO field = struct.io.fields[fieldIndex];
        assert fieldClass.equals(field.valueClass);

        Pointer ptr = Pointer.getPeer(struct).offset(field.byteOffset);
        Array<F> fieldValue = (Array<F>)struct.refreshableFields[field.refreshableFieldIndex];
        if (fieldValue == null)
            struct.refreshableFields[field.refreshableFieldIndex] = fieldValue = new Array<F>(fieldClass, field.arraySize, ptr);
        else
            fieldValue.setPointer(ptr);
        
        return fieldValue;
	}
	*/

	public static boolean getBoolField(int fieldIndex, StructObject struct) {
		return getByteField(fieldIndex, struct) != 0;
	}
	
	
	public static void setBoolField(int fieldIndex, StructObject struct, boolean fieldValue) {
		setByteField(fieldIndex, struct, (byte)(fieldValue ? 1 : 0));
	}

#foreach ($prim in $primitivesNoBool)
        
    /** $prim field getter */
    public static ${prim.Name} get${prim.CapName}Field(int fieldIndex, StructObject struct) {
        FieldIO field = struct.io.fields[fieldIndex];
        assert field.byteLength == (${prim.WrapperName}.SIZE / 8);
        assert ${prim.WrapperName}.TYPE.equals(field.valueClass) || ${prim.WrapperName}.class.equals(field.valueClass);

        if (field.isBitField)
            return BitFields.getPrimitiveValue(Pointer.getPeer(struct), field.byteOffset, field.bitOffset, field.bitLength, ${prim.WrapperName}.TYPE);

        return Pointer.getPeer(struct).get${prim.CapName}(field.byteOffset);
	}

    public static void set${prim.CapName}Field(int fieldIndex, StructObject struct, ${prim.Name} value) {
        FieldIO field = struct.io.fields[fieldIndex];
        assert field.byteLength == (${prim.WrapperName}.SIZE / 8);
        assert ${prim.WrapperName}.TYPE.equals(field.valueClass) || ${prim.WrapperName}.class.equals(field.valueClass);

        if (field.isBitField)
            BitFields.setPrimitiveValue(Pointer.getPeer(struct), field.byteOffset, field.bitOffset, field.bitLength, value, ${prim.WrapperName}.TYPE);
        else
            Pointer.getPeer(struct).set${prim.CapName}(field.byteOffset, value);
    }

	public static ${prim.BufferName} get${prim.CapName}BufferField(int fieldIndex, StructObject struct) {
        FieldIO field = struct.io.fields[fieldIndex];
        ${prim.BufferName} b = (${prim.BufferName})struct.refreshableFields[field.refreshableFieldIndex];
        if (b == null || !b.isDirect() || !Pointer.getPeer(struct).offset(field.byteOffset).equals(Pointer.pointerTo(b))) {
            int len = field.arraySize * field.byteLength;
            struct.refreshableFields[field.refreshableFieldIndex] = b = 
                Pointer.getPeer(struct).getByteBuffer(field.byteOffset, len)
                #if (!$prim.Name.equals("byte"))
                    .as${prim.BufferName}()
                #end
            ;
        }
        return b;
    }
    public static void set${prim.CapName}BufferField(int fieldIndex, StructObject struct, ${prim.BufferName} fieldValue) {
        FieldIO field = struct.io.fields[fieldIndex];
        if (fieldValue == null)
            throw new IllegalArgumentException("By-value struct struct.io.fields cannot be set to null");

        assert fieldValue.capacity() >= field.arraySize;
        struct.refreshableFields[field.refreshableFieldIndex] = fieldValue;
        int len = field.arraySize * field.byteLength;
        Pointer.getPeer(struct).getByteBuffer(field.byteOffset, len)
        #if (!$prim.Name.equals("byte"))
            .as${prim.BufferName}()
        #end
            .put(fieldValue.duplicate());
    }

	public static ${prim.Name}[] get${prim.CapName}ArrayField(int fieldIndex, StructObject struct) {
        FieldIO field = struct.io.fields[fieldIndex];
		return Pointer.getPeer(struct).get${prim.CapName}s(field.byteOffset, field.arraySize);
    }
    public void set${prim.CapName}ArrayField(int fieldIndex, StructObject struct, ${prim.Name}[] fieldValue) {
        FieldIO field = struct.io.fields[fieldIndex];
        if (fieldValue == null)
            throw new IllegalArgumentException("By-value struct struct.io.fields cannot be set to null");

		Pointer.getPeer(struct).write(field.byteOffset, fieldValue, 0, field.arraySize);
    }

#end

}
