package com.bridj.objc;

import com.bridj.JNI;
import com.bridj.CRuntime;

public class ObjectiveCRuntime extends CRuntime {

    public boolean isAvailable() {
        return JNI.isMacOSX();
    }
}
