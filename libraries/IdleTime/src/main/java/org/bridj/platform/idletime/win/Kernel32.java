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
	 * @see <a href="http://msdn2.microsoft.com/en-us/library/ms724408.aspx">MSDN GetTickCount function</a>
	 * @return number of milliseconds that have elapsed since the system was started.
	 */
	public static native int GetTickCount();
}

