package com.bridj;

import com.bridj.Pointer;
import java.nio.*;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

class IndirectBufferPointer<T> extends Pointer<T>
{
	#foreach ($prim in $primitivesNoBool)
	${prim.BufferName} ${prim.Name}Buffer;
	#end
	
	final long byteOffset;
	
	public IndirectBufferPointer(PointerIO<T> io, ByteBuffer byteBuffer) {
		this(io, byteBuffer, 0);
	}
	public IndirectBufferPointer(PointerIO<T> io, ByteBuffer byteBuffer, long byteOffset) {
        this.io = io;
		this.byteBuffer = byteBuffer;
		this.byteOffset = byteOffset;
    }
	
	@Override
    public Pointer<T> clone() {
		return new IndirectBufferPointer(io, byteBuffer.duplicate(), byteOffset);
    }
    @Override
    protected Pointer<T> disorderedClone() {
		ByteOrder order = byteBuffer.order(), newOrder = ByteOrder.BIG_ENDIAN.equals(order) ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
		return new IndirectBufferPointer(io, byteBuffer.duplicate().order(newOrder), byteOffset);
    }
    
	
	@Override
    public ByteOrder order() {
		return byteBuffer.order();
    }
	
	@Override    
    public Pointer<T> offset(long byteOffset) {
        return new IndirectBufferPointer(io, byteBuffer, this.byteOffset + byteOffset);
    }
	
    public boolean hasPeer() {
		return false;
	}
	
	public long getOrAllocateTempPeer() {
		return 0; // TODO
	}
    
	public void deleteTempPeer(long tempPeer, boolean refresh) {}
	
    @Override
    public int hashCode() {
        return byteBuffer.hashCode() ^ (int)byteOffset;
    }

#foreach ($prim in $primitivesNoBool)

	#if (${prim.Name} != "byte") 
    private synchronized ${prim.BufferName} getAligned${prim.BufferName}() {
		if (${prim.Name}Buffer == null)
			${prim.Name}Buffer = byteBuffer.as${prim.BufferName}();
		return ${prim.Name}Buffer;
	}
	#end
	
    @Override
    public Pointer<T> set${prim.CapName}(long byteOffset, ${prim.Name} value) {
		int actualOffset = SizeT.safeIntCast(this.byteOffset + byteOffset);
		int ind = actualOffset / ${prim.Size};
		#if (${prim.Name} != "byte") 
        int mod = actualOffset - ind * ${prim.Size};
		if (mod != 0)
			((ByteBuffer)byteBuffer.duplicate().position(${prim.Size} - mod)).as${prim.BufferName}().put(ind, value);
		else
			getAligned${prim.BufferName}().put(ind, value);
		#else
		byteBuffer.put(ind, value);
		#end
		
        return this;
    }
	
	@Override
    public ${prim.Name} get${prim.CapName}(long byteOffset) {
        int actualOffset = SizeT.safeIntCast(this.byteOffset + byteOffset);
		int ind = actualOffset / ${prim.Size};
		#if (${prim.Name} != "byte") 
        int mod = actualOffset - ind * ${prim.Size};
		if (mod != 0)
			return ((ByteBuffer)byteBuffer.duplicate().position(${prim.Size} - mod)).as${prim.BufferName}().get(ind);
		else
			return getAligned${prim.BufferName}().get(ind);
		#else
		return byteBuffer.get(ind);
		#end
    }

    @Override
    public Pointer<T> set${prim.CapName}s(long byteOffset, ${prim.Name}[] values, int valuesOffset, int length) {
		// TODO
		throw new UnsupportedOperationException();
    }

    @Override
    public Pointer<T> set${prim.CapName}s(long byteOffset, ${prim.BufferName} values, long valuesOffset, long length) {
        // TODO
		throw new UnsupportedOperationException();
    }

    @Override
    public ${prim.BufferName} get${prim.BufferName}(long byteOffset, long length) {
        // TODO
		throw new UnsupportedOperationException();
    }

    @Override
    public ${prim.Name}[] get${prim.CapName}s(long byteOffset, int length) {
        // TODO
		throw new UnsupportedOperationException();
    }

#end

    @Override
    protected long strlen(long byteOffset) {
		byte b;
		int actualOffset = (int)(this.byteOffset + byteOffset);
		int i = actualOffset;
		int n = 0;
		while ((b = byteBuffer.get(i++)) != 0)
			n++;
		return n;
	}
	
	@Override
	public long getPeer() {
		throw new UnsupportedOperationException("Cannot get pointer value for indirect NIO buffer-backed pointers ! Use getOrAllocateTempPeer() instead.");
	}
	
	
	@Override
	public Pointer<Pointer<T>> getReference() {
		throw new UnsupportedOperationException("Cannot get a reference to an indirect NIO buffer-backed pointer !");
	}
	
    @Override
    public int compareTo(Pointer<?> o) {
        if (o == null || !(o instanceof Pointer))
            return 1;

        if (o instanceof IndirectBufferPointer) {
            IndirectBufferPointer p = (IndirectBufferPointer)o;
            if (p.equals(byteOffset)) {
                return byteOffset < p.byteOffset ? -1 : 1;
            }
        }

        int d = hashCode() - o.hashCode();
        return d == 0 ? 1 : d;
    }
}
