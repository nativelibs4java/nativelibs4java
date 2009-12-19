/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.runtime.structs;

import com.nativelibs4java.runtime.ann.Field;
import com.sun.jna.Pointer;

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

    public MyStruct toto(int toto) {
        io.setIntField(0, this, toto);
        return this;
    }

    public static void main(String[] args) {
        MyStruct s = new MyStruct();
        s.toto(10);
        System.out.println(s.toto());
        System.out.println(s);
    }
}
