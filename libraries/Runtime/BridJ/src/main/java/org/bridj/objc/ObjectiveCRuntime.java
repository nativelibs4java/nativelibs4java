package org.bridj.objc;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;                               

import org.bridj.*;
import static org.bridj.Pointer.*;
import org.bridj.NativeEntities.Builder;
import org.bridj.util.Utils;
import org.bridj.ann.Library;
import org.bridj.ann.Ptr;
import org.bridj.ann.Runtime;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import org.bridj.Platform;

/// http://developer.apple.com/mac/library/documentation/Cocoa/Reference/ObjCRuntimeRef/Reference/reference.html
@Library("/usr/lib/libobjc.A.dylib")
public class ObjectiveCRuntime extends CRuntime {

    public boolean isAvailable() {
        return Platform.isMacOSX();
    }
    Map<String, Pointer<? extends ObjCObject>> 
    		nativeClassesByObjCName = new HashMap<String, Pointer<? extends ObjCObject>>(),
    		nativeMetaClassesByObjCName = new HashMap<String, Pointer<? extends ObjCObject>>();

    public ObjectiveCRuntime() {
        BridJ.register();

    }

    <T extends ObjCObject> T realCast(Pointer<? extends ObjCObject> id) {
        if (id == null)
            return null;
        Pointer<Byte> cn = object_getClassName(id);
        if (cn == null)
            throw new RuntimeException("Null class name for this ObjectiveC object pointer !");

        String n = cn.getCString();

        Class<? extends ObjCObject> c = bridjClassesByObjCName.get(n);
        if (c == null)
            throw new RuntimeException("Class " + n + " was not registered yet in the BridJ runtime ! (TODO : auto create by scanning path, then reflection !)");
        return (T)id.getNativeObject(c);
    }
    /*

    public static class Class extends Pointer<? extends ObjCObject> {

        public Class(long peer) {
            super(peer);
        }

        public Class(Pointer<?> ptr) {
            super(ptr);
        }
    }*/

    protected static native Pointer<? extends ObjCObject> object_getClass(Pointer<? extends ObjCObject> obj);

    protected static native Pointer<? extends ObjCObject> objc_getClass(Pointer<Byte> name);

    protected static native Pointer<? extends ObjCObject> objc_getMetaClass(Pointer<Byte> name);

    protected static native Pointer<Byte> object_getClassName(Pointer<? extends ObjCObject> obj);

    protected static native Pointer<? extends ObjCObject> class_createInstance(Pointer<? extends ObjCObject> cls, @Ptr long extraBytes);
    
    protected static native boolean class_respondsToSelector(Pointer<? extends ObjCObject> cls, SEL sel);

    protected static native SEL sel_registerName(Pointer<Byte> name);
    protected static native Pointer<Byte> sel_getName(SEL sel);
    
    public String getMethodSignature(Method method) {
    		return getMethodSignature(method.getGenericReturnType(), method.getGenericParameterTypes());
    }
    public String getMethodSignature(Type returnType, Type... paramTypes) {
    		StringBuilder b = new StringBuilder();
    		
    		b.append(getTypeSignature(returnType));
		b.append(getTypeSignature(Pointer.class)); // implicit self
		b.append(getTypeSignature(SEL.class)); // implicit selector
		for (Type paramType : paramTypes)
            b.append(getTypeSignature(paramType));
    		return b.toString();
    }
    
    char getTypeSignature(Type type) {
    		Character c = signatureByType.get(type);
    		if (c == null)
    			c = signatureByType.get(Utils.getClass(type));
    		if (c == null)
    			throw new RuntimeException("Unknown type for Objective-C signatures : " + Utils.toString(type));
    		return c;
    }
    
    static final Map<Type, Character> signatureByType = new HashMap<Type, Character>();
    static final Map<Character, List<Type>> typesBySignature = new HashMap<Character, List<Type>>();
    static {
    		initSignatures();
    }
    
    static void addSignature(char sig, Type... types) {
    		List<Type> typesList = typesBySignature.get(sig);
    		if (typesList == null)
			typesBySignature.put(sig, typesList = new ArrayList<Type>());
		
		for (Type type : types) {
			signatureByType.put(type, sig);
    		
			if (type != null && !typesList.contains(type))
    				typesList.add(type);
    		}
    }
    static void initSignatures() {
    		boolean is32 = CLong.SIZE == 4;
    		addSignature('q', long.class, !is32 ? CLong.class : null);
    		addSignature('i', int.class, is32 ? CLong.class : null);
    		addSignature('I', int.class, is32 ? CLong.class : null);
    		addSignature('s', short.class, char.class);
    		addSignature('c', byte.class, boolean.class);
    		addSignature('f', float.class);
    		addSignature('d', double.class);
    		addSignature('v', void.class);
    		addSignature('@', Pointer.class);
    		addSignature(':', SEL.class);
    }
    
    synchronized Pointer<? extends ObjCObject> getClass(String name, boolean meta) throws ClassNotFoundException {
    		Map<String, Pointer<? extends ObjCObject>> map = meta ? nativeMetaClassesByObjCName : nativeClassesByObjCName; 
        Pointer<? extends ObjCObject> c = map.get(name);
        if (c == null) {
        		Pointer<Byte> pName = pointerToCString(name);
        		c = meta ? objc_getMetaClass(pName) : objc_getClass(pName);
            if (c != null) {
                assert object_getClassName(c).getCString().equals(name);
                map.put(name, c);
            }
        }
        if (c == null)
    			throw new ClassNotFoundException("Objective C class not found : " + name);
        
        return c;
    }

    @Override
    protected NativeLibrary getNativeLibrary(Class<?> type) throws FileNotFoundException {
        Library libAnn = type.getAnnotation(Library.class);
        if (libAnn != null) {
            try {
                String name = libAnn.value();
                return BridJ.getNativeLibrary(name, new File("/System/Library/Frameworks/" + name + ".framework/" + name));
            } catch (FileNotFoundException ex) {
            }
        }

        return super.getNativeLibrary(type);
    }

    Map<String, Class<? extends ObjCObject>> bridjClassesByObjCName = new HashMap<String, Class<? extends ObjCObject>>();

    @Override
    public void register(Type type) {
        Class<?> typeClass = Utils.getClass(type);
        typeClass.getAnnotation(Library.class);
        Library libAnn = typeClass.getAnnotation(Library.class);
        if (libAnn != null) {
            String name = libAnn.value();
            File libraryFile = BridJ.getNativeLibraryFile(name);
            if (libraryFile != null) {
                System.load(libraryFile.toString());
            }
            if (ObjCObject.class.isAssignableFrom(typeClass)) {
                bridjClassesByObjCName.put(typeClass.getSimpleName(), (Class<? extends ObjCObject>)typeClass);
            }
        }

        super.register(type);
    }

    public String getSelector(Method method) {
    		Selector selAnn = method.getAnnotation(Selector.class);
    		if (selAnn != null)
    			return selAnn.value();
            
    		String n = method.getName();
		if (n.endsWith("_"))
			n = n.substring(0, n.length() - 1);
		
		if (method.getParameterTypes().length > 0)
			n += ":";
				
		n = n.replace('_', ':');
		return n;
	}
	
    @Override
    protected void registerNativeMethod(
    			Class<?> type,
            NativeLibrary typeLibrary, 
            Method method,
            NativeLibrary methodLibrary, 
            Builder builder, 
            MethodCallInfoBuilder methodCallInfoBuilder
		) throws FileNotFoundException 
	{

        if (method == null)
            return;

        try {
            MethodCallInfo mci = methodCallInfoBuilder.apply(method);
            boolean isStatic = Modifier.isStatic(method.getModifiers());
            
            if (isStatic) {
                Pointer<ObjCClass> pObjcClass = getClass((Class) type).as(ObjCClass.class);
				ObjCClass objcClass = pObjcClass.get();
				mci.setNativeClass(pObjcClass.getPeer());
            }

			mci.setSymbolName(getSelector(method));
            builder.addObjCMethod(mci);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("Failed to register method " + method + " : " + ex, ex);
        }
    }

    public static ObjectiveCRuntime getInstance() {
        return BridJ.getRuntimeByRuntimeClass(ObjectiveCRuntime.class);
    }
    
    @Override
    public <T extends NativeObject> TypeInfo<T> getTypeInfo(Type type) {
        return new CTypeInfo<T>(type) {

        		@Override
        		public void initialize(T instance, Pointer peer) {
        			if (instance instanceof ObjCClass) {
        				setNativeObjectPeer(instance, peer);
        			} else {
        				super.initialize(instance, peer);
        			}
        		}
            @Override
            public void initialize(T instance, int constructorId, Object... args) {
                try {
                    Pointer<? extends ObjCObject> c = ObjectiveCRuntime.this.getClass(typeClass);
                    if (c == null) {
                        throw new RuntimeException("Failed to get Objective-C class for type " + typeClass.getName());
                    }
                    Pointer<ObjCClass> pc = c.as(ObjCClass.class);
                    Pointer<ObjCObject> p = pc.get().new$(); //.alloc();
                    if (constructorId == -1) {
                        p = p.get().create();
                    } else {
                        throw new UnsupportedOperationException("TODO handle constructors !");
                    }
                    setNativeObjectPeer(instance, p);
                } catch (ClassNotFoundException ex) {
                    throw new RuntimeException("Failed to initialize instance of type " + Utils.toString(type) + " : " + ex, ex);
                }
            }
        };
    }

    private Pointer<? extends ObjCObject> getClass(Class<? extends NativeObject> cls) throws ClassNotFoundException {
    		if (cls == ObjCClass.class)
    			return getClass("NSObject", true);
            else if (cls == ObjCObject.class)
    			return getClass("NSObject", false);
    		
    		String n = cls.getSimpleName();
    		Pointer<? extends ObjCObject> id = getClass(n, false);
    		return id;
    }
}
