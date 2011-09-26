package org.bridj.platform.idletime.mac;
import org.bridj.platform.idletime.*;
import static org.bridj.platform.mac.CoreGraphics.*;

public class MacIdleTimeUtils extends IdleTimeUtils {
	public long getIdleTimeMillis() {
		return (long)(CGEventSourceSecondsSinceLastEventType(kCGEventSourceStateHIDSystemState, kCGAnyInputEventType) * 1000);
	}
}
