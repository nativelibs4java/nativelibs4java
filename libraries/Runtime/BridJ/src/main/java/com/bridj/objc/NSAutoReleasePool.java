package com.bridj.objc;

import com.bridj.BridJ;
import com.bridj.Pointer;
import com.bridj.ann.Library;

@Library("Foundation")
public class NSAutoReleasePool extends ObjCObject {
    static {
        BridJ.register();
    }

    public NSAutoReleasePool(Pointer<? extends NSAutoReleasePool> peer) {
        super(peer);
    }

    public NSAutoReleasePool() {
        super();
    }
    
}
