package com.bridj.cpp.com;

import com.bridj.*;
import com.bridj.ann.*;
import com.bridj.ann.Runtime;
import com.bridj.cpp.CPPObject;
import com.bridj.cpp.mfc.MFCRuntime;

import static com.bridj.cpp.com.COMRuntime.*;

//@Runtime(COMRuntime.class)
@Convention(Convention.Style.StdCall) // TODO make it inheritable
@IID("00000000-0000-0000-C000-000000000046")
@Runtime(COMRuntime.class)
public class IUnknown extends CPPObject {
	protected boolean autoRelease;

	public IUnknown() {}
	public IUnknown(Pointer<? extends IUnknown> peer) {
		super(peer);
	}

	public static IUnknown wrap(Object object) {
		if (object instanceof IUnknown)
			return (IUnknown)object;
		
		return new COMCallableWrapper(object);
	}
    @Override
    protected void finalize() throws Throwable {
        if (autoRelease)
            Release();
        super.finalize();
    }


	@Virtual(0)
	@Deprecated
	public native int QueryInterface(
		Pointer<Byte> riid,
		Pointer<Pointer<IUnknown>> ppvObject
	);
	
	public <I extends IUnknown> I QueryInterface(Class<I> type) {
		Pointer<Pointer<IUnknown>> p = Pointer.allocatePointer(IUnknown.class);
		int ret = QueryInterface(getIID(type), p);
		if (ret != S_OK)
			return null;
		
		return p.get().toNativeObject(type);
	}
	
	@Virtual(1)
	public native int AddRef();
	
	@Virtual(2)
	public native int Release();
	
}