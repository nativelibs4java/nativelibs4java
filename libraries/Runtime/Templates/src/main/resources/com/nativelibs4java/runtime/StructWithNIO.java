#if ($useJNA == "true")
#set ($package = "com.nativelibs4java.runtime.jna")
#else                                        
#set ($package = "com.jdyncall")
#end

package $package;

import ${memoryClass};
import ${pointerClass};

import java.nio.*;

/**
 *
 * @author ochafik
 */
public class StructWithNIO<S extends StructWithNIO<S>> extends Struct<S> {

	protected ByteBuffer buffer;

    public StructWithNIO(StructIOWithNIO io) {
        super(io);
    }

    public synchronized ByteBuffer getBuffer() {
        if (buffer == null)
            throw new UnsupportedOperationException("Struct is not backed by an NIO buffer (TODO: implement creation of a buffer out of a pointer)");

        return buffer;
    }

    @Override
    public synchronized S setPointer(Pointer pointer) {
        super.setPointer(pointer);
        this.buffer = null;
        return (S)this;
    }

    public synchronized void setBuffer(ByteBuffer buffer) {
        Pointer pointer = null;
        if (buffer == null || buffer.isDirect() && ((pointer = ${getDirectBufferPointer}(buffer)) == null || pointer.equals(Pointer.NULL)))
            throw new NullPointerException("Cannot set null pointer as struct address !");

        this.buffer = buffer;
        this.pointer = pointer;
    }


    public synchronized boolean isDirect() {
        return pointer != null;
    }

    public synchronized boolean hasBuffer() {
        return buffer != null;
    }

    @Override
    public S clone() {
        if (!isDirect())
            throw new UnsupportedOperationException("NIO clone not implemented yet");

        return super.clone();
    }


}
