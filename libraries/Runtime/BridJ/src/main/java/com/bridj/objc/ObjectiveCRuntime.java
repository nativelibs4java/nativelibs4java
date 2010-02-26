package com.bridj.objc;

import com.bridj.JNI;
import com.bridj.c.CRuntime;

public class ObjectiveCRuntime extends CRuntime {

    public boolean isAvailable() {
        return JNI.isMacOSX();
    }
}
