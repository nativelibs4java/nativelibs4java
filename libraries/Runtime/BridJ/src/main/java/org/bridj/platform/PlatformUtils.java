package org.bridj.platform;
import org.bridj.platform.mac.MacPlatformUtils;
import org.bridj.platform.win.WinPlatformUtils;
import org.bridj.platform.x11.X11PlatformUtils;
import org.bridj.ann.*;
import static org.bridj.Platform.*;
import static org.bridj.Pointer.*;

public abstract class PlatformUtils {
	public abstract long getIdleTimeMillis();
	
	private volatile static PlatformUtils instance;
	public synchronized static PlatformUtils getInstance() {
		if (instance == null) {
			if (isMacOSX())
				instance = new MacPlatformUtils();
			else if (isWindows())
				instance = new WinPlatformUtils();
			else
				instance = new X11PlatformUtils();
		}
		return instance;
	}
	
}
