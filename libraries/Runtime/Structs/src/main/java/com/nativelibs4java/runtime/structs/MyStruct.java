/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.runtime.structs;

import com.nativelibs4java.runtime.ann.Length;
import com.nativelibs4java.runtime.ann.Bits;
import com.nativelibs4java.runtime.ann.Field;
import com.nativelibs4java.runtime.structs.StructIO.FieldIO;
import com.sun.jna.Pointer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author ochafik
 */ 
public class MyStruct extends Struct<MyStruct> {
    private static StructIO<MyStruct> io = StructIO.getInstance(MyStruct.class);
    /*private static StructIO<MyStruct> io2 = StructIO.registerStructIO(MyStruct.class, new StructIO<MyStruct>(MyStruct.class) {

        @Override
        protected void orderFields(List<FieldIO> fields) {
            super.orderFields(fields); //...
        }

    });*/
    public MyStruct() {
        super(io);
    }
    public static MyStruct cast(Pointer p) {
        return new MyStruct().setPointer(p);
    }

    @Field(0)
    public int toto() {
        return io.getIntField(0, this);
    }

    @Field(1) @Bits(1)
    public int isOk() {
        return io.getIntField(1, this);
    }

    @Field(2) @Length(10)
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
