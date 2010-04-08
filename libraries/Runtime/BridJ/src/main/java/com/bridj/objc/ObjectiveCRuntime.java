package com.bridj.objc;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.bridj.BridJ;
import com.bridj.JNI;
import com.bridj.CRuntime;
import com.bridj.MethodCallInfo;
import com.bridj.NativeLibrary;
import com.bridj.NativeObject;
import com.bridj.Pointer;
import com.bridj.NativeEntities.Builder;
import com.bridj.ann.Library;
import com.bridj.ann.Ptr;
import com.bridj.ann.Runtime;

/// http://developer.apple.com/mac/library/documentation/Cocoa/Reference/ObjCRuntimeRef/Reference/reference.html
@Library("/usr/lib/libobjc.A.dylib")
@Runtime(CRuntime.class)
public class ObjectiveCRuntime extends CRuntime {

    public boolean isAvailable() {
        return JNI.isMacOSX();
    }
    
    Map<String, Pointer<ObjCClass>> classes = new HashMap<String, Pointer<ObjCClass>>();
    
    public ObjectiveCRuntime() {
    	BridJ.register();
    }
    
    
    protected static native Pointer<?> objc_getClass(Pointer<Byte> name);
    protected static native Pointer<ObjCClass> class_createInstance(Pointer<?> cls, @Ptr long extraBytes); 
    
    synchronized Pointer<ObjCClass> getClass(String name) {
    	Pointer<ObjCClass> c = classes.get(name);
    	if (c == null) {
    		Pointer<?> cls = objc_getClass(Pointer.pointerToCString(name));
    		c = class_createInstance(cls, 0);
    		if (c != null) {
    			c.setTargetClass(ObjCClass.class);
    			classes.put(name, c);
    		}
    	}
    	return c;
    }
    
    @Override
    protected NativeLibrary getNativeLibrary(Class<?> type) throws FileNotFoundException {
    	Library libAnn = type.getAnnotation(Library.class);
    	if (libAnn != null)
	    	try {
	    		String name = libAnn.value();
	    		return BridJ.getNativeLibrary(name, new File("/System/Library/Frameworks/" + name + ".framework/" + name));
	    	} catch (FileNotFoundException ex) {
	    		
	    	}
	    	
    	return super.getNativeLibrary(type);
    }
    
    @Override
    public void register(Class<?> type) {
    	Library libAnn = type.getAnnotation(Library.class);
    	if (libAnn != null) {
    		String name = libAnn.value();
    		File libraryFile = BridJ.getNativeLibraryFile(name);
    		if (libraryFile != null)
    			System.load(libraryFile.toString());
    	}
    	
    	super.register(type);
    }
    @Override
    protected void registerNativeMethod(Class<?> type,
    		NativeLibrary typeLibrary, Method method,
    		NativeLibrary methodLibrary, Builder builder)
    		throws FileNotFoundException {
    	
    	MethodCallInfo mci = new MethodCallInfo(method);
    	Selector sel = method.getAnnotation(Selector.class);
    	if (sel != null)
    		mci.setSymbolName(sel.value());
    	else {
    		mci.setSymbolName(method.getName() + ":");
    	}
    	builder.addObjCMethod(mci);
    }
    
    
	@Override
	public void initialize(NativeObject instance, int constructorId, Object... args) {
		Pointer<ObjCClass> c = getClass(instance.getClass());
		Pointer<ObjCObject> p = c.get().new$();//.alloc();
		if (constructorId == -1)
			p = p.get().create();
		else
			throw new UnsupportedOperationException("TODO handle constructors !");
		setNativeObjectPeer(instance, p);
	}

	private Pointer<ObjCClass> getClass(Class<? extends NativeObject> class1) {
		return getClass(class1.getSimpleName());
	}
}
