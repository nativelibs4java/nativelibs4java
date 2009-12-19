/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.runtime.structs;

import com.nativelibs4java.runtime.ann.Field;
import com.sun.jna.Pointer;
import java.nio.IntBuffer;
import java.util.Arrays;

/**
 *
 * @author ochafik
 */ 
public class MyStruct extends Struct<MyStruct> {
    private static StructIO<MyStruct> io = StructIO.getInstance(MyStruct.class);
    public MyStruct() {
        super(io);
    }
    public static MyStruct cast(Pointer p) {
        return new MyStruct().setPointer(p);
    }

    @Field(index=0)
    public int toto() {
        return io.getIntField(0, this);
    }

    @Field(index=1, bits=1)
    public int isOk() {
        return io.getIntField(1, this);
    }

    @Field(index=2, arraySize=10)
    public IntBuffer values() {
        return io.getIntArrayField(2, this);
    }
    public void values(IntBuffer buf) {
        io.setIntArrayField(2, this, buf);
    }

    public MyStruct toto(int toto) {
        io.setIntField(0, this, toto);
        return this;
    }

    public static void main(String[] args) {
        MyStruct s = new MyStruct();
        s.toto(10);
        s.values(IntBuffer.wrap(new int[] { 1, 2, 3}));
        int[] out = new int[3];
        s.values().get(out);
        System.out.println(Arrays.toString(out));

        MyStruct ns = s.clone();
        ns.values().get(out);
        System.out.println(Arrays.toString(out));

        System.out.println(s.toto());
        System.out.println(s);
    }
}
