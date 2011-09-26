package org.bridj.platform.idletime.x11;
import org.bridj.*;
import org.bridj.ann.*;
import static org.bridj.Pointer.*;

public class X11 {
	static {
		BridJ.register();
	}   
	public static native Display XOpenDisplay(Pointer<Byte> name);
    public static native Window XDefaultRootWindow(Display display);
    public static native int XFree(Pointer data);
    public static native int XCloseDisplay(Display display);
}
