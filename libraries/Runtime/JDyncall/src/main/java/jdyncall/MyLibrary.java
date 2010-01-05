package jdyncall;

import com.nativelibs4java.runtime.*;
import com.nativelibs4java.runtime.ann.*;
import java.io.File;

/**
 *
 * @author Olivier
 */
public class MyLibrary extends NativeLib {
    public MyLibrary() {
		this("myLibrary");
	}
	public MyLibrary(String libraryName) {
		super(libraryName);
	}
	public MyLibrary(File libraryFile) {
		super(libraryFile);
	}

    public static class MyCallback {
        
    }
    public static class MyTypedPtr extends Pointer {
        public MyTypedPtr(Pointer<?> ptr) {
            super(ptr.getPeer());
        }
        @Deprecated
        public MyTypedPtr(long peer) {
            super(peer);
        }
    }
	protected native @PointerSized long someFunction_native(@PointerSized long arg1);
	public MyCallback someFunction(MyTypedPtr arg1) {
		return null;//Callback.wrapCallback(someFunction_native(Pointer.getPeer(arg1)), MyCallback.class);
	}
	
	protected native int someFunction_native(@PointerSized long stringArray, @PointerSized long errOut);
	public int someFunction(String[] arg1, Pointer<Integer> errOut) {
		UpdatablePointer<String> pArg1 = Pointer.updatablePointerTo(arg1);
		try {
			return someFunction_native(Pointer.getPeer(pArg1), Pointer.getPeer(errOut));
		} finally {
			Pointer.update(pArg1);
		}
	}
	
	protected native int someFunction2_native(@PointerSized long size, @PointerSized long sizeOut);
	public int someFunction2(long size, Pointer<SizeT> sizeOut) {
		return someFunction2_native(size, Pointer.getPeer(sizeOut));
	}
}
