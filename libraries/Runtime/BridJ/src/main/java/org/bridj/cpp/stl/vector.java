package org.bridj.cpp.stl;

import org.bridj.ann.Template;
import org.bridj.cpp.CPPObject;


import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.ann.Field;
import org.bridj.ann.Struct;
import org.bridj.cpp.CPPRuntime;

import java.lang.reflect.Type;
import org.bridj.BridJRuntime;

import static org.bridj.Pointer.*;

/**
 * Binding for <a href="http://www.sgi.com/tech/stl/Vector.html">STL's std::vector</a> class.
 * @author ochafik
 * @param <T>
 */
@Template({ Type.class })
@Struct(customizer = std.class)
public class vector<T> extends CPPObject {
	@Field(0)
	protected Pointer<T> _M_start() {
		return io.getPointerField(this, 0);
	}
	@Field(1)
	protected Pointer<T> _M_finish() {
		return io.getPointerField(this, 1);
	}
	@Field(2)
	protected Pointer<T> _M_end_of_storage() {
		return io.getPointerField(this, 2);
	}
	//@Constructor(-1)
	public vector(Type t) {
		super((Void)null, CPPRuntime.SKIP_CONSTRUCTOR, t);
	}
	public vector(Pointer<? extends vector<T>> peer) {
		super(peer);
	}

	public T get(long index) {
		// TODO make this unnecessary
		Pointer<T> p = _M_start().as(T());
		return p.get(index);
	}
	public T get(int index) {
		return get((long)index);
	}
	public void push_back(T value) {
		throw new UnsupportedOperationException();
	}
	protected Type T() {
		return (Type)CPPRuntime.getInstance().getTemplateParameters(this, vector.class)[0];
	}
	protected long byteSize() {
		return _M_finish().getPeer() - _M_start().getPeer();
	}

	public long size() {
		long byteSize = byteSize();
		long elementSize = BridJ.sizeOf(T());

		return byteSize / elementSize;
	}
}
