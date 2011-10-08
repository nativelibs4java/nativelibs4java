package org.bridj.objc;
import org.bridj.*;
import static org.bridj.Pointer.*;

public class SEL extends TypedPointer {
		public SEL(long peer) { super(peer); }
		public SEL(Pointer<?> ptr) { super(ptr); }
		public SEL(String name) {
			this(pointerToCString(name));
		}
}

