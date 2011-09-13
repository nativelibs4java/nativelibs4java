package org.bridj.platform.x11;
import org.bridj.*;
import org.bridj.ann.*;
import static org.bridj.Pointer.*;

public class Drawable extends TypedPointer {
	public Drawable(long peer) { super(peer); }
	public Drawable(Pointer<?> ptr) { super(ptr); }
}
