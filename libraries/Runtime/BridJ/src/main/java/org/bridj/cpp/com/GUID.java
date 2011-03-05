/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bridj.cpp.com;

import org.bridj.Pointer;
import org.bridj.ann.Array;
import org.bridj.ann.Field;
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

        out.setIntAtOffset(0, (int)Long.parseLong(descriptor.substring(0, 8), 16));
        out.setShortAtOffset(4, (short)Long.parseLong(descriptor.substring(8, 12), 16));
        out.setShortAtOffset(6, (short)Long.parseLong(descriptor.substring(12, 16), 16));
        for (int i = 0; i < 8; i++)
            out.setByteAtOffset(8 + i, (byte)Long.parseLong(descriptor.substring(16 + i * 2, 16 + i * 2 + 2), 16));

        return out;
    }
    

    private static RuntimeException throwBad(String descriptor) {
        return new RuntimeException("Expected something like :\n" + model + "\nBut got instead :\n" +descriptor);
    }
    
    /*
    // 4315D437-5B8C-11d0-BD3B-00A0C911CE86
    OUR_GUID_ENTRY(CLSID_CDeviceMoniker, 0x4315D437,0x5B8C,0x11d0,0xBD,0x3B,0x00,0xA0,0xC9,0x11,0xCE,0x86)
                             1                2        3      4     5     6    7   8    9    10   11   12
                                              
    Regexp replace (jEdit) on C:\Program Files\Microsoft SDKs\Windows\v6.0A\Include\\uuids.h :
    (?m)OUR_GUID_ENTRY\(\s*(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*,\s*0x(\w+)\s*\)
    "public static final String " + _1 + " = \"" + _2 + "-" + _3 + "-" + _4 + "-" + _5 + _6 + "-" + _7 + _8 + _9 + _10 + _11 + _12
    */
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