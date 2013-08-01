/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nativelibs4java.opencl.proxy;

import org.bridj.NativeObject;
import org.bridj.Pointer;
import static org.bridj.Pointer.*;

public class PointerUtils {
   
    protected static void setSizeT(long peer, long value) {
        pointerToAddress(peer).setSizeT(value);
    }
    
    protected static void setSizeTAtIndex(long peer, int index, long value) {
        pointerToAddress(peer).setSizeTAtIndex(index, value);
    }
    
    protected static void setPointerAtIndex(long peer, int index, Pointer<?> value) {
        pointerToAddress(peer).setPointerAtIndex(index, value);
    }
    
    protected static void setPointerAtIndex(long peer, int index, NativeObject value) {
        pointerToAddress(peer).setSizeTAtIndex(index, Pointer.getPeer(Pointer.getPointer(value)));
    }
    
    protected static long getSizeT(long peer) {
        return pointerToAddress(peer).getSizeT();
    }
    
    protected static <T> Pointer<T> getPointer(long peer, Class<T> targetClass) {
        return pointerToAddress(peer).getPointer(targetClass);
    }
    
    protected static void setInt(long peer, int value) {
        pointerToAddress(peer).setInt(value);
    }
    
    protected static int getInt(long peer) {
        return pointerToAddress(peer).getInt();
    }
}
