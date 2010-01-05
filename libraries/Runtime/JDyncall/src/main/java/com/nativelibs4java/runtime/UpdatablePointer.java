package com.nativelibs4java.runtime;

import java.lang.reflect.Type;

/**
 *
 * @author Olivier
 */
public abstract class UpdatablePointer<T> extends Memory.SharedPointer<T> {
    UpdatablePointer(PointerIO<T> io, long peer) {
        super(io, peer, null);
    }
    UpdatablePointer(PointerIO<T> io, Memory ref) {
        super(io, ref.peer, ref);
    }
    
    public abstract void update();
}
