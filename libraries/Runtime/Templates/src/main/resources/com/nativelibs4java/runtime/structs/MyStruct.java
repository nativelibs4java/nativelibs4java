#if ($useJNA.equals("true"))
#set ($package = "com.nativelibs4java.runtime.structs.jna")
#set ($annPackage = "com.nativelibs4java.runtime.ann.jna")
#else
#set ($package = "com.nativelibs4java.runtime.structs")
#set ($annPackage = "com.nativelibs4java.runtime.ann")
#end

package $package;

import ${memoryClass};
import ${pointerClass};

import ${annPackage}.*;
import ${package}.StructIO.FieldIO;
import java.nio.*;
import java.util.*;

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
        return io.getIntBufferField(2, this);
    }
    public void values(IntBuffer buf) {
        io.setIntBufferField(2, this, buf);
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
