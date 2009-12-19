/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.runtime.structs;

import com.nativelibs4java.runtime.ann.Field;

/**
 *
 * @author ochafik
 */
public class MyStruct extends Struct<MyStruct> {
    private static StructIO<MyStruct> io = new StructIO<MyStruct>(MyStruct.class);
    public MyStruct() {
        super(io);
    }

    @Field(index=0)
    public int getToto() {
        return io.getIntField(0, this);
    }

    public static void main(String[] args) {
        MyStruct s = new MyStruct();

        System.out.println(s);
    }
}
