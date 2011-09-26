package org.bridj.platform.idletime.x11;
import org.bridj.platform.idletime.*;
import org.bridj.*;
import org.bridj.ann.*;
import static org.bridj.Pointer.*;

import static org.bridj.platform.idletime.x11.Xss.*;
import static org.bridj.platform.idletime.x11.X11.*;

public class X11IdleTimeUtils extends IdleTimeUtils {
	public long getIdleTimeMillis() {
		Display display = null;
		Window window = null;
		Pointer<XScreenSaverInfo> pInfo = null;
		
		try {
			display = XOpenDisplay(null);
			window = XDefaultRootWindow(display);
			pInfo = XScreenSaverAllocInfo();
			if (XScreenSaverQueryInfo(display, window, pInfo) == 0)
				throw new UnsupportedOperationException("XSreenSaver extension not supported !");
		
			return pInfo.get().idle();
		} finally {
			if (pInfo != null)
				XFree(pInfo);
		
			if (display != null)
				XCloseDisplay(display);
		}
	}	
}
