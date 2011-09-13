package org.bridj.platform.win;
import org.bridj.*;
import org.bridj.ann.*;

@Convention(Convention.Style.StdCall)
@Library("user32")
public class User32 {	
	static {
		BridJ.register();
	}
	/**
	 * Contains the time of the last input.
	 * @see http://msdn.microsoft.com/library/default.asp?url=/library/en-us/winui/winui/windowsuserinterface/userinput/keyboardinput/keyboardinputreference/keyboardinputstructures/lastinputinfo.asp
	 */
	public static class LASTINPUTINFO extends StructObject {
		@Field(0)
		public int cbSize() {
			return this.io.getIntField(this, 0);
		}
		/// Tick count of when the last input event was received.
		@Field(1)
		public int dwTime() {
			return this.io.getIntField(this, 1);
		}
	}
	
	/**
	 * Retrieves the time of the last input event.
	 * @see http://msdn.microsoft.com/library/default.asp?url=/library/en-us/winui/winui/windowsuserinterface/userinput/keyboardinput/keyboardinputreference/keyboardinputfunctions/getlastinputinfo.asp
	 * @return time of the last input event, in milliseconds
	 */
	public static native boolean GetLastInputInfo(Pointer<LASTINPUTINFO> result);
}

