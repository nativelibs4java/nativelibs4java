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

	
	public static Type getFieldType(StructObject struct, int fieldIndex) {
		assert struct != null;
		assert struct.io != null;
		assert struct.io.fields != null;
		return struct.io.fields[fieldIndex].valueType;
	}
    
    public static Pointer getPointerField(StructObject struct, int fieldIndex) {
        assert struct != null;
		assert struct.io != null;
		assert struct.io.fields != null;
		FieldIO field = struct.io.fields[fieldIndex];
        assert !field.isBitField;
        assert Pointer.class.isAssignableFrom(field.valueClass);
        assert field.callIO != null;
        
        return (Pointer)field.callIO.newInstance(Pointer.getPeer(struct).getSizeT(field.byteOffset));
    }
    public static void setPointerField(StructObject struct, int fieldIndex, Pointer p) {
        assert struct != null;
		assert struct.io != null;
		assert struct.io.fields != null;
		FieldIO field = struct.io.fields[fieldIndex];
        assert !field.isBitField;
        assert Pointer.class.isAssignableFrom(field.valueClass);

        Pointer.getPeer(struct).setPointer(field.byteOffset, p);
    }
    /*
	
    public void setRefreshableField(StructObject struct, int fieldIndex, PointerRefreshable value) {
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

    public <F extends PointerRefreshable> F getRefreshableField(StructObject struct, int fieldIndex, Class<F> fieldClass) {
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

    public static <F extends NativeObject> void setNativeObjectField(StructObject struct, int fieldIndex, F fieldValue) {
        assert struct != null;
		assert struct.io != null;
		assert struct.io.fields != null;
		FieldIO field = struct.io.fields[fieldIndex];
        assert field.valueClass.isInstance(fieldValue);
        if (field.isByValue) {
            if (fieldValue == null)
                throw new IllegalArgumentException("By-value struct struct.io.fields cannot be set to null");
            // Nothing to do : by-value struct already wrote its feeds as appropriate
        } else {
            struct.refreshableFields[field.refreshableFieldIndex] = fieldValue;
            Pointer.getPeer(struct).setPointer(field.byteOffset, Pointer.getPeer(fieldValue));
        }
	}

    public static <F extends NativeObject> F getNativeObjectField(StructObject struct, int fieldIndex, Class<F> fieldClass) {
        assert struct != null;
		assert struct.io != null;
		assert struct.io.fields != null;
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
    public <F extends Struct<F>> Array<F> getStructArrayField(StructObject struct, int fieldIndex, Class<F> fieldClass) {
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

	public static boolean getBoolField(StructObject struct, int fieldIndex) {
		return getByteField(struct, fieldIndex) != 0;
	}
	
	
	public static void setBoolField(StructObject struct, int fieldIndex, boolean fieldValue) {
		setByteField(struct, fieldIndex, (byte)(fieldValue ? 1 : 0));
	}

#foreach ($prim in $primitivesNoBool)
        
    /** $prim field getter */
    public static ${prim.Name} get${prim.CapName}Field(StructObject struct, int fieldIndex) {
        assert struct != null;
		assert struct.io != null;
		assert struct.io.fields != null;
		FieldIO field = struct.io.fields[fieldIndex];
        assert field.byteLength == (${prim.WrapperName}.SIZE / 8);
        assert ${prim.WrapperName}.TYPE.equals(field.valueClass) || ${prim.WrapperName}.class.equals(field.valueClass);

        if (field.isBitField)
            return BitFields.getPrimitiveValue(Pointer.getPeer(struct), field.byteOffset, field.bitOffset, field.bitLength, ${prim.WrapperName}.TYPE);

        return Pointer.getPeer(struct).get${prim.CapName}(field.byteOffset);
	}

    public static void set${prim.CapName}Field(StructObject struct, int fieldIndex, ${prim.Name} value) {
        assert struct != null;
		assert struct.io != null;
		assert struct.io.fields != null;
		FieldIO field = struct.io.fields[fieldIndex];
        assert field.byteLength == (${prim.WrapperName}.SIZE / 8);
        assert ${prim.WrapperName}.TYPE.equals(field.valueClass) || ${prim.WrapperName}.class.equals(field.valueClass);

        if (field.isBitField)
            BitFields.setPrimitiveValue(Pointer.getPeer(struct), field.byteOffset, field.bitOffset, field.bitLength, value, ${prim.WrapperName}.TYPE);
        else
            Pointer.getPeer(struct).set${prim.CapName}(field.byteOffset, value);
    }

	public static ${prim.BufferName} get${prim.CapName}BufferField(StructObject struct, int fieldIndex) {
        assert struct != null;
		assert struct.io != null;
		assert struct.io.fields != null;
		FieldIO field = struct.io.fields[fieldIndex];
        ${prim.BufferName} b = (${prim.BufferName})struct.refreshableFields[field.refreshableFieldIndex];
        if (b == null || !b.isDirect() || !Pointer.getPeer(struct).offset(field.byteOffset).equals(Pointer.pointerToBuffer(b))) {
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
    public static void set${prim.CapName}BufferField(StructObject struct, int fieldIndex, ${prim.BufferName} fieldValue) {
        assert struct != null;
		assert struct.io != null;
		assert struct.io.fields != null;
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

	public static ${prim.Name}[] get${prim.CapName}ArrayField(StructObject struct, int fieldIndex) {
        assert struct != null;
		assert struct.io != null;
		assert struct.io.fields != null;
		FieldIO field = struct.io.fields[fieldIndex];
		return Pointer.getPeer(struct).get${prim.CapName}s(field.byteOffset, field.arraySize);
    }
    public void set${prim.CapName}ArrayField(StructObject struct, int fieldIndex, ${prim.Name}[] fieldValue) {
        assert struct != null;
		assert struct.io != null;
		assert struct.io.fields != null;
		FieldIO field = struct.io.fields[fieldIndex];
        if (fieldValue == null)
            throw new IllegalArgumentException("By-value struct struct.io.fields cannot be set to null");

		Pointer.getPeer(struct).set${prim.CapName}s(field.byteOffset, fieldValue, 0, field.arraySize);
    }

#end

}
