package com.bridj;

import com.bridj.*;
import static com.bridj.Pointer.*;
import com.bridj.ann.*;
import java.io.File;
import java.io.FileNotFoundException;

/**
 *
 * @author Olivier
 */
public class MyLibrary {
    public MyLibrary() {
		BridJ.register(getClass());
	}
    public static class MyCallback {
        
    }
    public static class MyTypedPtr extends DefaultPointer {
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
		TempPointers temp = new TempPointers(pointerTo(arg1), errOut);
        try {
        	return someFunction_native(temp.get(0), temp.get(1));
        } finally {
            temp.release();
        }
	}
	
	protected native int someFunction2_native(@PointerSized long size, @PointerSized long sizeOut);
	public int someFunction2(long size, Pointer<SizeT> sizeOut) {
		return someFunction2_native(size, Pointer.getPeer(sizeOut));
	}
}
