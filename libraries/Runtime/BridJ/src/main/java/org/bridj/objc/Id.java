package org.bridj.objc;
import org.bridj.*;
import static org.bridj.Pointer.*;

public class Id extends TypedPointer {

        public Id(long peer) {
            super(peer);
        }

        public Id(Pointer<?> ptr) {
            super(ptr);
        }

        @Override
        public ObjCObject get() {
            return BridJ.getRuntimeByRuntimeClass(ObjectiveCRuntime.class).realCast(this);
        }
        @Override
        public ObjCObject get(long index) {
            if (index == 0)
                return get();
            throw new UnsupportedOperationException("Cannot read from an Objective-C object pointer with an index");
        }
        @Override
        public Object set(long index, Object value) {
            throw new UnsupportedOperationException("Cannot write to an Objective-C object pointer");
        }
    }
