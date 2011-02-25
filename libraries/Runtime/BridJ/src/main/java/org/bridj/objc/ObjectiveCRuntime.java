package org.bridj.objc;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.bridj.BridJ;
import org.bridj.JNI;
import org.bridj.CRuntime;
import org.bridj.MethodCallInfo;
import org.bridj.NativeLibrary;
import org.bridj.NativeObject;
import org.bridj.Pointer;
import org.bridj.NativeEntities.Builder;
import org.bridj.TypedPointer;
import org.bridj.util.Utils;
import org.bridj.ann.Library;
import org.bridj.ann.Ptr;
import org.bridj.ann.Runtime;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import org.bridj.Platform;

/// http://developer.apple.com/mac/library/documentation/Cocoa/Reference/ObjCRuntimeRef/Reference/reference.html
@Library("/usr/lib/libobjc.A.dylib")
@Runtime(CRuntime.class)
public class ObjectiveCRuntime extends CRuntime {

    public boolean isAvailable() {
        return Platform.isMacOSX();
    }
    Map<String, Id> nativeClassesByObjCName = new HashMap<String, Id>();

    public ObjectiveCRuntime() {
        BridJ.register();

    }

    public static class Id extends TypedPointer {

        public Id(long peer) {
            super(peer);
        }

        public Id(Pointer<?> ptr) {
            super(ptr);
        }

        @Override
        public ObjCObject get() {
            return BridJ.getRuntimeByRuntimeClass(ObjectiveCRuntime.class).realCast(this);
        }
        @Override
        public ObjCObject get(long index) {
            if (index == 0)
                return get();
            throw new UnsupportedOperationException("Cannot read from an Objective-C object pointer with an index");
        }
        @Override
        public Object set(long index, Object value) {
            if (index == 0)
                return set(value);
            throw new UnsupportedOperationException("Cannot write to an Objective-C object pointer");
        }
    }

    <T extends ObjCObject> T realCast(Id id) {
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

    public static class Class extends Id {

        public Class(long peer) {
            super(peer);
        }

        public Class(Pointer<?> ptr) {
            super(ptr);
        }
    }*/

    protected static native Id object_getClass(Id obj);

    protected static native Id objc_getClass(Pointer<Byte> name);

    protected static native Id objc_getMetaClass(Pointer<Byte> name);

    protected static native Pointer<Byte> object_getClassName(Id obj);

    protected static native Id class_createInstance(Id cls, @Ptr long extraBytes);

    synchronized Id getClass(String name) {
        Id c = nativeClassesByObjCName.get(name);
        if (c == null) {
            c = objc_getClass(Pointer.pointerToCString(name));
            if (c != null) {
                assert object_getClassName(c).getCString().equals(name);
                nativeClassesByObjCName.put(name, c);
            }
        }
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

    @Override
    protected void registerNativeMethod(Class<?> type,
            NativeLibrary typeLibrary, Method method,
            NativeLibrary methodLibrary, Builder builder)
            throws FileNotFoundException {

        MethodCallInfo mci = new MethodCallInfo(method);
        Selector sel = method.getAnnotation(Selector.class);
        if (Modifier.isStatic(method.getModifiers())) {
            mci.setNativeClass(getClass((Class) type).getPeer());
        }

        if (sel != null) {
            mci.setSymbolName(sel.value());
        } else {
            mci.setSymbolName(method.getName() + ":");
        }
        builder.addObjCMethod(mci);
    }

    @Override
    public <T extends NativeObject> TypeInfo<T> getTypeInfo(Type type) {
        return new CTypeInfo<T>(type) {

            @Override
            public void initialize(T instance, int constructorId, Object... args) {
                Id c = ObjectiveCRuntime.this.getClass(typeClass);
                if (c == null)
                    throw new RuntimeException("Failed to get Objective-C class for type " + typeClass.getName());
                Pointer<ObjCClass> pc = c.as(ObjCClass.class);
                Pointer<ObjCObject> p = pc.get().new$();//.alloc();
                if (constructorId == -1) {
                    p = p.get().create();
                } else {
                    throw new UnsupportedOperationException("TODO handle constructors !");
                }
                setNativeObjectPeer(instance, p);
            }
        };
    }

    private Id getClass(Class<? extends NativeObject> class1) {
        return getClass(class1.getSimpleName());
    }
}
