/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.runtime.structs;

import ${memoryClass};
import ${pointerClass};

import java.nio.ByteBuffer;

/**
 *
 * @author ochafik
 */
public class StructIOWithNIO<S extends StructWithNIO<S>> extends StructIO<S> {
    public StructIOWithNIO(Class<S> structClass) {
        super(structClass);
    }

    @Override
    public int getIntField(int fieldIndex, S struct) {
        if (struct.isDirect())
            return super.getIntField(fieldIndex, struct);

        FieldIO field = fields[fieldIndex];
        if (field.isBitField)
            throw new UnsupportedOperationException("Indirect Buffer-backed bit fields are not supported yet");

        return ((ByteBuffer)struct.getBuffer().duplicate().position(field.byteOffset)).asIntBuffer().get();
	}

    @Override
    public void setIntField(int fieldIndex, S struct, int value) {
        if (struct.isDirect()) {
            super.setIntField(fieldIndex, struct, value);
            return;
        }

        FieldIO field = fields[fieldIndex];
        if (field.isBitField)
            throw new UnsupportedOperationException("Indirect Buffer-backed bit fields are not supported yet");

        ((ByteBuffer)struct.getBuffer().duplicate().position(field.byteOffset)).asIntBuffer().put(value);
	}
}
