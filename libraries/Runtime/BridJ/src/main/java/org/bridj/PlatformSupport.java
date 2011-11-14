/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bridj;

import java.io.IOException;

class PlatformSupport {
    PlatformSupport() {}
    
    public ClassDefiner getClassDefiner(ClassDefiner defaultDefiner, ClassLoader parentClassLoader) {
        return defaultDefiner;
    }
    volatile static PlatformSupport instance;
    public static synchronized PlatformSupport getInstance() {
        if (instance == null) {
            if (Platform.isAndroid())
                try {
                    instance = (PlatformSupport)Class.forName("org.bridj.AndroidSupport").newInstance();;
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to instantiate the Android support class... Was the BridJ jar tampered with / trimmed too much ?", ex);
                }
            
            if (instance == null)
                instance = new PlatformSupport();
        }
        return instance;
    }

    public static synchronized void setInstance(PlatformSupport instance) {
        PlatformSupport.instance = instance;
    }
    
    public NativeLibrary loadNativeLibrary(String name) throws IOException {
        return null;
    }
}