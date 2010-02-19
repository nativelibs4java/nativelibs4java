package com.bridj;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.*;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

class DefaultDisorderedPointer<T> extends DefaultPointer<T>
{
	public DefaultDisorderedPointer(PointerIO<T> io, long peer) {
		super(io, peer);
    }
    public DefaultDisorderedPointer(long peer) {
		super(peer);
    }
    
    @Override
    public Pointer<T> clone() {
    		return new DefaultDisorderedPointer(io, peerOrOffsetInOwner);
    }
    @Override
    protected Pointer<T> disorderedClone() {
    		return new DefaultPointer(io, peerOrOffsetInOwner);
    }
    
	@Override
    public ByteOrder order() {
		return ByteOrder.BIG_ENDIAN.equals(ByteOrder.nativeOrder()) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
    }
    
	/// TODO merge with Memory.share
	@Override    
    public Pointer<T> offset(long byteOffset) {
        PointerIO<T> io = getIO();
        int size = io == null ? io.getTargetSize() : 1;
        DefaultPointer<T> p = new DefaultDisorderedPointer(io, getCheckedPeer(byteOffset, size));
		p.memoryOwner = memoryOwner;
		p.peerOrOffsetInOwner += byteOffset;
		return p;
    }
	
#foreach ($prim in $primitivesNoBool)

    @Override
    public Pointer<T> set${prim.CapName}(long byteOffset, ${prim.Name} value) {
        JNI.set_${prim.Name}_disordered(getCheckedPeer(byteOffset, ${prim.Size}), value);
        return this;
    }

    @Override
    public Pointer<T> set${prim.CapName}s(long byteOffset, ${prim.Name}[] values, int valuesOffset, int length) {
        JNI.set_${prim.Name}_array_disordered(getCheckedPeer(byteOffset, ${prim.Size} * length), values, valuesOffset, length);
        return this;
    }

    @Override
    public ${prim.Name} get${prim.CapName}(long byteOffset) {
        return JNI.get_${prim.Name}_disordered(getCheckedPeer(byteOffset, ${prim.Size}));
    }

    @Override
    public ${prim.Name}[] get${prim.CapName}s(long byteOffset, int length) {
        return JNI.get_${prim.Name}_array_disordered(getCheckedPeer(byteOffset, ${prim.Size} * length), length);
    }

#end

}
