package com.bridj.objc;

import com.bridj.BridJ;
import com.bridj.NativeObject;
import com.bridj.Pointer;
import com.bridj.ann.Library;

@Library("Foundation")
public class NSCalendar extends ObjCObject {
    static {
        BridJ.register();
    }

    public NSCalendar(Pointer<? extends NSCalendar> peer) {
        super(peer);
    }

    public NSCalendar() {
        super();
    }

    public static native Pointer<NSCalendar> currentCalendar();
    public native Pointer<NSString> calendarIdentifier();
}
