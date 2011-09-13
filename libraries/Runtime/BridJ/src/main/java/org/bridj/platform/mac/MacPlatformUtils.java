package org.bridj.platform.mac;
import org.bridj.*;
import org.bridj.platform.*;
import static org.bridj.platform.mac.CoreGraphics.*;

public class MacPlatformUtils extends PlatformUtils {
	public long getIdleTimeMillis() {
		return (long)(CGEventSourceSecondsSinceLastEventType(kCGEventSourceStateHIDSystemState, kCGAnyInputEventType) * 1000);
	}
}
