/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj.cpp.com;

import com.bridj.Pointer;
import com.bridj.ann.Array;
import com.bridj.ann.Field;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.text.ParseException;

/**
 *
 * @author Olivier
 */
public class GUID {

    private static final String model = "00000000-0000-0000-0000-000000000000";

    // Need to parse as (int, short, short, char[8])
    public static Pointer<?> parseGUID128Bits(String descriptor) {
        Pointer<?> out = Pointer.allocateBytes(16 + 4);
        descriptor = descriptor.replaceAll("-", "");
        if (descriptor.length() != 32)
            throw throwBad(descriptor);

        out.setInt(0, (int)Long.parseLong(descriptor.substring(0, 8), 16));
        out.setShort(4, (short)Long.parseLong(descriptor.substring(8, 12), 16));
        out.setShort(6, (short)Long.parseLong(descriptor.substring(12, 16), 16));
        for (int i = 0; i < 8; i++)
            out.setByte(8 + i, (byte)Long.parseLong(descriptor.substring(16 + i * 2, 16 + i * 2 + 2), 16));

        return out;
    }
    

    private static RuntimeException throwBad(String descriptor) {
        return new RuntimeException("Expected something like :\n" + model + "\nBut got instead :\n" +descriptor);
    }
    /*
     * public GUID() {
        super();
    }

    public GUID(String descriptor) {
        super();
        if (descriptor.length() != model.length())
            throw throwBad(descriptor);
        String[] parts = descriptor.split("-");
        if (parts.length != 5 ||
                parts[0].length() != 8 ||
                parts[1].length() != 4 ||
                parts[2].length() != 4 ||
                parts[3].length() != 4 ||
                parts[4].length() != 12)
            throw throwBad(descriptor);

        Data1(Integer.parseInt(parts[0], 16));
        Data2(Short.parseShort(parts[1], 16));
        Data3(Short.parseShort(parts[2], 16));
        Pointer<Byte> p = Data4();
        p.setShort(0, Short.parseShort(parts[3], 16));
        String last = parts[4];
        p.setInt(2, Integer.parseInt(last.substring(0, 8), 16));
        p.setShort(6, Short.parseShort(last.substring(8), 16));
    }
    
    @Field(0)
    public native int Data1();
    public native void Data1(int Data1);
    @Field(1)
    public native short Data2();
    public native void Data2(short Data2);
    @Field(2)
    public native short Data3();
    public native void Data3(short Data3);
    
    @Field(3) @Array(8)
    public native Pointer<Byte> Data4();*/
}