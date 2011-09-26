package org.bridj.platform.idletime;
import org.bridj.platform.idletime.mac.MacIdleTimeUtils;
import org.bridj.platform.idletime.win.WinIdleTimeUtils;
import org.bridj.platform.idletime.x11.X11IdleTimeUtils;
import org.bridj.ann.*;
import static org.bridj.Platform.*;
import static org.bridj.Pointer.*;

public abstract class IdleTimeUtils {
	public abstract long getIdleTimeMillis();
	
	private volatile static IdleTimeUtils instance;
	public synchronized static IdleTimeUtils getInstance() {
		if (instance == null) {
			if (isMacOSX())
				instance = new MacIdleTimeUtils();
			else if (isWindows())
				instance = new WinIdleTimeUtils();
			else
				instance = new X11IdleTimeUtils();
		}
		return instance;
	}
	
}
