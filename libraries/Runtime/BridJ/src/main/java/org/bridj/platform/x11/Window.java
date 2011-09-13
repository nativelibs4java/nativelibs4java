package org.bridj.platform.x11;
import org.bridj.*;
import org.bridj.ann.*;
import static org.bridj.Pointer.*;

public class Window extends Drawable {
	public Window(long peer) { super(peer); }
	public Window(Pointer<?> ptr) { super(ptr); }
}
