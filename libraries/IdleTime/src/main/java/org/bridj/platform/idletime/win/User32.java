package org.bridj.platform.idletime.win;
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
	 * @see <a href="http://msdn.microsoft.com/library/default.asp?url=/library/en-us/winui/winui/windowsuserinterface/userinput/keyboardinput/keyboardinputreference/keyboardinputstructures/lastinputinfo.asp">MSDN LASTINPUTINFO structure</a>
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
	 * @see <a href="http://msdn.microsoft.com/library/default.asp?url=/library/en-us/winui/winui/windowsuserinterface/userinput/keyboardinput/keyboardinputreference/keyboardinputfunctions/getlastinputinfo.asp">MSDN GetLastInputInfo function</a>
	 * @return time of the last input event, in milliseconds
	 */
	public static native boolean GetLastInputInfo(Pointer<LASTINPUTINFO> result);
}

