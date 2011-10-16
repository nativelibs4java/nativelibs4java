package org.bridj.objc;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import org.bridj.*;
import static org.bridj.Pointer.*;
import java.util.*;
import org.bridj.util.Pair;

public class ObjCProxy extends ObjCObject {
	final Map<SEL, Pair<NSMethodSignature, Method>> signatures = new HashMap<SEL, Pair<NSMethodSignature, Method>>();
	final Object invocationTarget;
	
	protected ObjCProxy() {
		super(null);
		peer = JNI.createObjCProxyPeer(this);
		assert getClass() != ObjCProxy.class;
		this.invocationTarget = this;
	}
	public ObjCProxy(Object invocationTarget) {
		super(null);
		peer = JNI.createObjCProxyPeer(this);
		assert invocationTarget != null;
		this.invocationTarget = invocationTarget;
	}
	
	public Object getInvocationTarget() {
		return invocationTarget;
	}
	public Pointer<NSMethodSignature> methodSignatureForSelector(SEL sel) {
		Pair<NSMethodSignature, Method> sig = getMethodAndSignature(sel);
        return sig == null ? null : pointerTo(sig.getFirst());
	}
    public synchronized Pair<NSMethodSignature, Method> getMethodAndSignature(SEL sel) {
		Pair<NSMethodSignature, Method> sig = signatures.get(sel);
		if (sig == null) {
			sig = computeMethodAndSignature(sel);
			if (sig != null)
				signatures.put(sel, sig);
		}
		return sig;
	}
	Pair<NSMethodSignature, Method> computeMethodAndSignature(SEL sel) {
		String name = sel.getName();
		ObjectiveCRuntime rt = ObjectiveCRuntime.getInstance();
		for (Method method : invocationTarget.getClass().getMethods()) {
			String msel = rt.getSelector(method);
			if (msel.equals(name)) {
				String sig = rt.getMethodSignature(method);
				NSMethodSignature ms = NSMethodSignature.signatureWithObjCTypes(pointerToCString(sig)).get();
                long nArgs = ms.numberOfArguments() - 2;
                if (nArgs != method.getParameterTypes().length)
                    throw new RuntimeException("Bad method signature (mismatching arg types) : '" + sig + "' for " + method);
				return new Pair<NSMethodSignature, Method>(ms, method);
			}
		}
		return null;
	}
	public synchronized void forwardInvocation(Pointer<NSInvocation> pInvocation) {
        NSInvocation invocation = pInvocation.get();
        SEL sel = invocation.selector();
		Pair<NSMethodSignature, Method> sigMet = getMethodAndSignature(sel);
        NSMethodSignature sig = sigMet.getFirst();
        Method method = sigMet.getSecond();
        
        //System.out.println("forwardInvocation(" + invocation + ") : sel = " + sel);
        Type[] paramTypes = method.getGenericParameterTypes();
        int nArgs = paramTypes.length;//(int)sig.numberOfArguments();
        Object[] args = new Object[nArgs];
        for (int i = 0; i < nArgs; i++) {
            Type paramType = paramTypes[i];
            PointerIO<?> paramIO = PointerIO.getInstance(paramType);
            Pointer<?> pArg = allocate(paramIO);
            invocation.getArgument_atIndex(pArg, i + 2);
            Object arg = pArg.get();
            args[i] = arg;
        }
		try {
            method.setAccessible(true);
            Object ret = method.invoke(getInvocationTarget(), args);
            //System.out.println("Invoked  " + method + " : " + ret);
            Type returnType = method.getGenericReturnType();
            if (returnType == void.class) {
                assert ret == null;
            } else {
                PointerIO<?> returnIO = PointerIO.getInstance(returnType);
                Pointer<Object> pRet = (Pointer)allocate(returnIO);
                pRet.set(ret);
                invocation.setReturnValue(pRet);
            }
        } catch (Throwable ex) {
            throw new RuntimeException("Failed to forward invocation from Objective-C to Java invocation target " + getInvocationTarget() + " for method " + method + " : " + ex, ex);
        }
	}
	
	
}

