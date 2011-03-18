package org.bridj.jawt;
import static org.bridj.jawt.JawtLibrary.*;
import org.bridj.BridJ;
import org.bridj.JNI;
import org.bridj.NativeLibrary;
import org.bridj.Pointer;
import static org.bridj.Pointer.*;
import java.awt.*;
import java.io.File;
import org.bridj.Platform;

import org.bridj.ann.Convention;
/**
 * Contains a method that returns the native peer handle of an AWT component : BridJ JAWT utilities {@link org.bridj.jawt.JAWTUtils#getNativePeerHandle(java.awt.Component)}
 */
public class JAWTUtils {
    
	/**
	 * 
	 */
	public static long getNativePeerHandle(Component component) {
		try {
			if (GraphicsEnvironment.isHeadless())
				throw new HeadlessException("No native peers in headless mode.");
        
			if (component.isLightweight())
				throw new IllegalArgumentException("Lightweight components do not have native peers.");
        
			if (!component.isDisplayable()) 
				throw new IllegalArgumentException("Component that are not displayable do not have native peers.");
			
			JNIEnv env = new JNIEnv(JNI.getEnv());
			JAWT awt = new JAWT().version(JAWT_VERSION_1_4);
			Pointer<JAWT> pAwt = pointerTo(awt);
			if (!JAWT_GetAWT(env, pAwt))
				throw new RuntimeException("Failed to get JAWT pointer !");
				
			Pointer<?> componentPointer = pointerToAddress(JNI.newGlobalRef(component));
			try {
				//Pointer<JAWT_DrawingSurface> pSurface = pointerToAddress((int)((Integer)awt.GetDrawingSurface().
				//	asDynamicFunction(Convention.Style.StdCall, int.class, int.class, int.class).apply((int)env.getPeer(), (int)componentPointer.getPeer()))).as(JAWT_DrawingSurface.class);
				Pointer<JAWT_DrawingSurface> pSurface = awt.GetDrawingSurface().as(JAWT.GetDrawingSurface_callback.class).get().invoke(env, componentPointer).as(JAWT_DrawingSurface.class);
				if (pSurface == null)
					throw new RuntimeException("Cannot get drawing surface from " + component);
				
				JAWT_DrawingSurface surface = pSurface.get();
	  
				try {
					int lock = surface.Lock().get().invoke(pSurface);
					if ((lock & JAWT_LOCK_ERROR) != 0)
						throw new RuntimeException("Cannot lock drawing surface of " + component);
					try {
						Pointer<JAWT_DrawingSurface.GetDrawingSurfaceInfo_callback> cb = surface.GetDrawingSurfaceInfo().as(JAWT_DrawingSurface.GetDrawingSurfaceInfo_callback.class);
						Pointer<org.bridj.jawt.JAWT_DrawingSurfaceInfo > pInfo = cb.get().invoke(pSurface);
						if (pInfo != null)
							pInfo = pInfo.as(JAWT_DrawingSurfaceInfo.class);
						Pointer<?> platformInfo = pInfo.get().platformInfo();
						return platformInfo.getSizeT(); // on win, mac, x11 platforms, the relevant field is the first in the struct !
						/*if (Platform.isWindows())
						{
							@Library("jawt") 
							public static class JAWT_Win32DrawingSurfaceInfo extends StructObject {
								public JAWT_Win32DrawingSurfaceInfo() {
									super();
								}
								public JAWT_Win32DrawingSurfaceInfo(Pointer pointer) {
									super(pointer);
								}
								@Field(0)
								public static native Pointer<?> hwnd();
							}
							JAWT_Win32DrawingSurfaceInfo wdsi = new JAWT_Win32DrawingSurfaceInfo(platformInfo);
							return wdsi.hwnd().getPeer();
						} else if (Platform.isMacOSX())
						{
							return 0;//JAWT_MacOSXDrawingSurfaceInfo mdsi = new JAWT_MacOSXDrawingSurfaceInfo(platformInfo); 
							//return mdsi.cocoaViewRef();
						} else if (Platform.isUnix())
						{
							return 0;//JAWT_X11DrawingSurfaceInfo xdsi = new JAWT_X11DrawingSurfaceInfo(platformInfo);	
							//return xdsi.drawable();
						} else 
							throw new UnsupportedOperationException("Native peer can only be fetched on Windows, MacOS X and X11-powered platforms");*/
					} finally {
						surface.Unlock().get().invoke(pSurface);
					}
				} finally {
					awt.FreeDrawingSurface().get().invoke(pSurface);
				}
			} finally {
				JNI.deleteGlobalRef(componentPointer.getPeer());
			}
		} catch (Throwable ex) {
			ex.printStackTrace();
			return 0;
		}
	}
    
}