package org.bridj;

import org.bridj.ann.Convention;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.bridj.Pointer.*;

/**
 * Factory that is able to create dynamic functions bindings with a given signature
 */
public class DynamicFunctionFactory {

    final Constructor<? extends DynamicFunction> constructor;
    final Method method;
    final long callbackHandle;

    DynamicFunctionFactory(Class<? extends DynamicFunction> callbackClass, Method method, Convention.Style style) {
        try {
            this.constructor = callbackClass.getConstructor();
            this.method = method;
            
            MethodCallInfo mci = new MethodCallInfo(method);
            mci.setDeclaringClass(callbackClass);
            if (style != null)
                mci.setCallingConvention(style);
            
            callbackHandle = JNI.bindJavaToCCallbacks(mci);
        } catch (Throwable th) {
            th.printStackTrace();
            throw new RuntimeException("Failed to instantiate callback" + " : " + th, th);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        JNI.freeJavaToCCallbacks(callbackHandle, 1);
    }


    public DynamicFunction newInstance(Pointer<?> functionPointer) {
        if (functionPointer == null)
            return null;
        
        try {
            DynamicFunction dcb = constructor.newInstance();
            dcb.peer = (Pointer) functionPointer;
            dcb.method = method;
            dcb.factory = this;
            
            return dcb;
        } catch (Throwable th) {
            th.printStackTrace();
            throw new RuntimeException("Failed to instantiate callback" + " : " + th, th);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + method + ")";
    }


}
