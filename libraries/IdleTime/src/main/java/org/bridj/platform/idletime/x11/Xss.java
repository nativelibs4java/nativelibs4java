package org.bridj.platform.idletime.x11;
import org.bridj.*;
import org.bridj.ann.Ptr;
import org.bridj.ann.Field;
import static org.bridj.Pointer.*;

public class Xss {
	static {
		BridJ.register();
	}
	public static class XScreenSaverInfo extends StructObject {
		public XScreenSaverInfo() {
			super();
		}
		public XScreenSaverInfo(Pointer pointer) {
			super(pointer);
		}
		/**
		 * screen saver window<br>
		 * C type : Window
		 */
		@Ptr 
		@Field(0) 
		public long window() {
			return this.io.getSizeTField(this, 0);
		}
		/**
		 * screen saver window<br>
		 * C type : Window
		 */
		@Ptr 
		@Field(0) 
		public XScreenSaverInfo window(long window) {
			this.io.setSizeTField(this, 0, window);
			return this;
		}
		/// ScreenSaver{Off,On,Disabled}
		@Field(1) 
		public int state() {
			return this.io.getIntField(this, 1);
		}
		/// ScreenSaver{Off,On,Disabled}
		@Field(1) 
		public XScreenSaverInfo state(int state) {
			this.io.setIntField(this, 1, state);
			return this;
		}
		/// ScreenSaver{Blanked,Internal,External}
		@Field(2) 
		public int kind() {
			return this.io.getIntField(this, 2);
		}
		/// ScreenSaver{Blanked,Internal,External}
		@Field(2) 
		public XScreenSaverInfo kind(int kind) {
			this.io.setIntField(this, 2, kind);
			return this;
		}
		/// milliseconds
		@org.bridj.ann.CLong 
		@Field(3) 
		public long til_or_since() {
			return this.io.getCLongField(this, 3);
		}
		/// milliseconds
		@org.bridj.ann.CLong 
		@Field(3) 
		public XScreenSaverInfo til_or_since(long til_or_since) {
			this.io.setCLongField(this, 3, til_or_since);
			return this;
		}
		/// milliseconds
		@org.bridj.ann.CLong 
		@Field(4) 
		public long idle() {
			return this.io.getCLongField(this, 4);
		}
		/// milliseconds
		@org.bridj.ann.CLong 
		@Field(4) 
		public XScreenSaverInfo idle(long idle) {
			this.io.setCLongField(this, 4, idle);
			return this;
		}
		/// events
		@org.bridj.ann.CLong 
		@Field(5) 
		public long event_mask() {
			return this.io.getCLongField(this, 5);
		}
		/// events
		@org.bridj.ann.CLong 
		@Field(5) 
		public XScreenSaverInfo event_mask(long event_mask) {
			this.io.setCLongField(this, 5, event_mask);
			return this;
		}
	}
	
	public static native Pointer<XScreenSaverInfo> XScreenSaverAllocInfo();
	public static native int XScreenSaverQueryInfo(Display dpy, Drawable drawable, Pointer<XScreenSaverInfo> saver_info);
}
