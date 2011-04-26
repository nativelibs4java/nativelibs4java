package org.bridj;
import static org.bridj.Pointer.*;
import org.junit.Test;
import static org.junit.Assert.*;

import org.bridj.ann.Library;

@org.bridj.ann.Runtime(CRuntime.class)
public class WindowsTest {
	static {
		BridJ.register();
	}
	
	@Library("user32")
	public static native void SendMessage(Pointer<?> hwnd, int Msg, int wParam, Pointer<?> lParam);
	
	boolean win = Platform.isWindows();
	
	@Test
	public void testUnicodeWorked() {
		if (!win) return;
		SendMessage(null, 0, 0, null);
	}
}
