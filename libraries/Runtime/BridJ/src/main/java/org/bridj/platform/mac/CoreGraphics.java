package org.bridj.platform.mac;
import org.bridj.*;

//@Library("CoreGraphics")
public class CoreGraphics {	
	static {
		BridJ.register();
	}
	
	public static final int kCGAnyInputEventType = ~0;
	public static final int kCGEventSourceStatePrivate = -1;
	public static final int kCGEventSourceStateCombinedSessionState = 0;
	public static final int kCGEventSourceStateHIDSystemState = 1;
	public static native double CGEventSourceSecondsSinceLastEventType(int source, int eventType);
}
