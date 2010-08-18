package org.bridj.objc;

import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.ann.Library;

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
