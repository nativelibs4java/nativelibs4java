package com.bridj;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.*;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

class DefaultPointer<T> extends Pointer<T>
{
	protected long peerOrOffsetInOwner;
    protected Object memoryOwner;
	
	public DefaultPointer(PointerIO<T> io, long peer) {
		if (peer == 0)
			throw new IllegalArgumentException("Cannot instantiate a pointer with a null peer");
		this.io = io;
        this.peerOrOffsetInOwner = peer;
    }
    public DefaultPointer(long peer) {
		this(null, peer);
    }

    @Override
    public Pointer<T> clone() {
    		return new DefaultPointer(io, peerOrOffsetInOwner);
    }
    @Override
    protected Pointer<T> disorderedClone() {
    		return new DefaultDisorderedPointer(io, peerOrOffsetInOwner);
    }
    
	protected void free(long address) {}
	
	protected void deallocate() {
		long peer = getPeer();
		if (peer == 0)
			return;
		
		if (memoryOwner != null) {
			if (memoryOwner instanceof Pointer)
				((Pointer)memoryOwner).setSizeT(this.peerOrOffsetInOwner, 0);
			//memoryOwner = null;
			return;
		} else
			this.peerOrOffsetInOwner = 0;
			
		free(peer);
    }
    
	@Override
	public Pointer<Pointer<T>> getReference() {
		if (memoryOwner != null) {
			if (memoryOwner instanceof Pointer)
				return ((Pointer)memoryOwner).offset(peerOrOffsetInOwner);
			else if (memoryOwner instanceof Buffer) {
				Buffer b = (Buffer)memoryOwner;
				if (b.isDirect())
					return (Pointer)pointerToBuffer(b, peerOrOffsetInOwner);
				else
					throw new RuntimeException("Cannot get a pointer to this pointer because it is based on a non-direct NIO " + memoryOwner.getClass().getSimpleName());
			} else
				throw new RuntimeException("Cannot get a pointer to this pointer because it is based on a " + memoryOwner.getClass() + " object");
			
		} else {
			Pointer p = pointerToAddress(peerOrOffsetInOwner);
			memoryOwner = p;
			peerOrOffsetInOwner = 0;
			return p;
		}
	}
	
	/// TODO merge with Memory.share
	@Override    
    public Pointer<T> offset(long byteOffset) {
		if (byteOffset == 0)
			return this;
		
        PointerIO<T> io = getIO();
        int size = io == null ? io.getTargetSize() : 1;
        DefaultPointer<T> p = new DefaultPointer(io, getCheckedPeer(byteOffset, size));
		p.memoryOwner = memoryOwner;
		//p.peerOrOffsetInOwner += byteOffset;
		return p;
    }
	
	protected long getCheckedPeer(long byteOffset, long validityCheckLength) {
		/*if (memoryOwner != null) {
			if (memoryOwner instanceof Pointer) {
				return ((Pointer)memoryOwner).getSizeT(peerOrOffsetInOwner) + byteOffset;
			} else if (memoryOwner instanceof Buffer) {
				Buffer b = (Buffer)memoryOwner;
				if (b.isDirect())
					return JNI.getDirectBufferAddress(b) + peerOrOffsetInOwner + byteOffset;
				else
					throw new RuntimeException("Cannot get the peer value of this pointer because it is based on a non-direct NIO " + memoryOwner.getClass().getSimpleName());
			} else
				throw new RuntimeException("Cannot get the peer value of this pointer because it is based on a " + memoryOwner.getClass().getName() + " object");
		}*/
		return peerOrOffsetInOwner + byteOffset;
    }
    
	@Override
    public long getPeer() {
		return getCheckedPeer(0, 0);
	}
	
    public boolean hasPeer() {
		return true;
	}
	
	public long getOrAllocateTempPeer() {
		return getPeer();
	}
    
	public void deleteTempPeer(long tempPeer, boolean refresh) {}
	
	@Override
    public int compareTo(Pointer<?> p) {
		if (p == null || !p.hasPeer())
			return 1;
		
		long p1 = getPeer(), p2 = p.getPeer();
		return p1 < p2 ? -1 : 1;
	}
	
    @Override
    public int hashCode() {
		int hc = new Long(peerOrOffsetInOwner).hashCode();
		return memoryOwner != null ? memoryOwner.hashCode() ^ hc : hc;
    }

#foreach ($prim in $primitivesNoBool)

    @Override
    public Pointer<T> set${prim.CapName}(long byteOffset, ${prim.Name} value) {
        JNI.set_${prim.Name}(getCheckedPeer(byteOffset, ${prim.Size}), value);
        return this;
    }

    @Override
    public Pointer<T> set${prim.CapName}s(long byteOffset, ${prim.Name}[] values, int valuesOffset, int length) {
        JNI.set_${prim.Name}_array(getCheckedPeer(byteOffset, ${prim.Size} * length), values, valuesOffset, length);
        return this;
    }

    @Override
    public Pointer<T> set${prim.CapName}s(long byteOffset, ${prim.BufferName} values, long valuesOffset, long length) {
        if (values.isDirect()) {
            long len = length * ${prim.Size}, off = valuesOffset * ${prim.Size};
            long cap = JNI.getDirectBufferCapacity(values);
            if (cap < off + len)
                throw new IndexOutOfBoundsException("The provided buffer has a capacity (" + cap + " bytes) smaller than the requested write operation (" + len + " bytes starting at byte offset " + off + ")");
            
			JNI.memcpy(getCheckedPeer(byteOffset, ${prim.Size} * length), JNI.getDirectBufferAddress(values) + off, len);
        } else if (values.isReadOnly()) {
            get${prim.BufferName}(byteOffset, length).put(values.duplicate());
        } else {
            set${prim.CapName}s(byteOffset, values.array(), (int)(values.arrayOffset() + valuesOffset), (int)length);
        }
        return this;
    }

	@Override
    public ${prim.BufferName} get${prim.BufferName}(long byteOffset, long length) {
        long blen = ${prim.Size} * length;
        ByteBuffer buffer = JNI.newDirectByteBuffer(getCheckedPeer(byteOffset, blen), blen);
		buffer.order(order());
        #if ($prim.Name == "byte")
        return buffer;
        #else
        return buffer.as${prim.BufferName}();
        #end
    }
    
    @Override
    public ${prim.Name} get${prim.CapName}(long byteOffset) {
        return JNI.get_${prim.Name}(getCheckedPeer(byteOffset, ${prim.Size}));
    }

    @Override
    public ${prim.Name}[] get${prim.CapName}s(long byteOffset, int length) {
        return JNI.get_${prim.Name}_array(getCheckedPeer(byteOffset, ${prim.Size} * length), length);
    }

#end

    @Override
    protected long strlen(long byteOffset) {
		return JNI.strlen(getCheckedPeer(byteOffset, 1));
	}
	
}
