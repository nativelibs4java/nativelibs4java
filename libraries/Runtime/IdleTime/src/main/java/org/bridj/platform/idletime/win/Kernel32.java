package org.bridj.platform.idletime.win;
import org.bridj.*;
import org.bridj.ann.*;

@Convention(Convention.Style.StdCall)
@Library("kernel32")
public class Kernel32 {
	static {
		BridJ.register();
	}
	/**
	 * Retrieves the number of milliseconds that have elapsed since the system was started.
	 * @see http://msdn2.microsoft.com/en-us/library/ms724408.aspx
	 * @return number of milliseconds that have elapsed since the system was started.
	 */
	public static native int GetTickCount();
}

