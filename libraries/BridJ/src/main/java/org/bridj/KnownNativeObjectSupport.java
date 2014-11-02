package org.bridj;

import java.util.HashMap;
import java.util.Map;

import org.bridj.WeakReferenceMonitor.ReleaseListener;

public class KnownNativeObjectSupport {
	
	 	private Map<Long, NativeObject> knownNativeObjects = new HashMap<Long, NativeObject>();

	    public <O extends NativeObject> void setJavaObjectFromNativePeer(final long peer, O object) {
	        if (object == null) {
	            knownNativeObjects.remove(peer);
	        } else {
	        	knownNativeObjects.put(peer, object);
	        	WeakReferenceMonitor.monitor(object, new ReleaseListener() {
					public void released() {
						knownNativeObjects.remove(peer);						
					}	        		
	        	});
	            
	        }
	    }

	    public Object getJavaObjectFromNativePeer(long peer) {
	        return knownNativeObjects.get(peer);
	    }
}
