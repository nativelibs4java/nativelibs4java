package com.nativelibs4java.jni;

import com.nativelibs4java.jni.*;
import static com.nativelibs4java.jni.JvmLibrary.*;
import java.nio.charset.Charset;
import org.bridj.Pointer;
import org.bridj.JNI;
import static org.bridj.Pointer.*;
import static org.bridj.util.JNIUtils.*;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ochafik
 */
public class ReflectionUtils {
    static final JavaVM_ jvm;
    static final Charset UTF8;
    static {
        jvm = new JavaVM_(pointerToAddress(JNI.getJVM()));
        UTF8 = Charset.forName("utf-8");
    }
    static JNIEnv_ getEnv() {
        /*
        Pointer<Pointer<JNIEnv_>> ppenv = allocatePointer(JNIEnv_.class);
        jvm.GetEnv((Pointer)ppenv, JNI_VERSION_1_4);
        JNIEnv_ env = ppenv.get().get();
        */
        return new JNIEnv_(pointerToAddress(JNI.getEnv()));
    }
    static jobject newRef(Object instance) {
        return new jobject(JNI.newGlobalRef(instance));
    }
    static jclass newClassRef(Object instance) {
        return new jclass(JNI.newGlobalRef(instance));
    }
    static void delRef(jobject ref) {
        JNI.deleteGlobalRef(ref.getPeer());
    }
    static void delRef(jclass ref) {
        JNI.deleteGlobalRef(ref.getPeer());
    }
    static Pointer<Byte> pointerToUTF8String(String s) {
        return (Pointer)pointerToString(s, StringType.C, UTF8);
    }
    public static class ObjectField<T> extends NativeField<T> {
        public ObjectField(jclass jcls, jfieldID field, boolean isStatic) { super(jcls, field, isStatic); }
        public T get(Object instance) {
            JNIEnv_ env = getEnv();
            jobject vref;
            if (isStatic) {
                vref = env.GetStaticObjectField(jcls, field);
            } else {
                jobject iref = newRef(instance);
                try {
                    vref = env.GetObjectField(iref, field);
                } finally {
                    delRef(iref);
                }
            }
            return (T)JNI.refToObject(env.NewGlobalRef(vref).getPeer());
        }
        public void set(Object instance, T value) {
            JNIEnv_ env = getEnv();
            jobject vref = newRef(value);
            try {
                if (isStatic)
                    env.SetStaticObjectField(jcls, field, vref);
                else {
                    jobject iref = newRef(instance);
                    try {
                        env.SetObjectField(iref, field, vref);
                    } finally {
                        delRef(iref);
                    }   
                }
            } finally {
                delRef(vref);
            }
        }
    }
    public abstract static class NativeField<T> {
        protected final jclass jcls;
        protected final jfieldID field;
        protected final boolean isStatic;
        NativeField(jclass jcls, jfieldID field, boolean isStatic) {
            this.jcls = jcls;
            this.field = field;
            this.isStatic = isStatic;
        }
        public abstract T get(Object instance);
        public abstract void set(Object instance, T value);
    }
    
    public static NativeClass getNativeClass(Class c) {
        return new NativeClass(c);
    }
    public static class NativeClass {
        final Class cls;
        final jclass jcls;
        public NativeClass(Class cls) {
            this.cls = cls;
            jcls = newClassRef(getEnv().FindClass(pointerToUTF8String(cls.getName().replace('.', '/'))));
            if (jcls == null)
                throw new RuntimeException(new ClassNotFoundException(cls.getName()));
        }
        
        public <T> NativeField<T> getField(String name, Class<T> tpe, boolean isStatic) throws NoSuchFieldException {
            Pointer<Byte> pName = pointerToUTF8String(name), pSig = pointerToUTF8String(getNativeSignature(tpe));
            jfieldID field = 
                    isStatic ? 
                    getEnv().GetStaticFieldID(jcls, pName, pSig) :
                    getEnv().GetFieldID(jcls, pName, pSig);
            
            if (field == null)
                throw new NoSuchFieldException(cls.getName() + "." + name + " of type " + tpe.getName());
            
#foreach ($prim in $primitives)
            if (tpe == ${prim.Name}.class)
                return (NativeField<T>)new ${prim.CapName}Field(jcls, field, isStatic);
#end
            return new ObjectField<T>(jcls, field, isStatic);
        }

        @Override
        protected void finalize() throws Throwable {
            delRef(jcls);
        }
        
    }
    
#foreach ($prim in $primitives)
    public static class ${prim.CapName}Field extends NativeField<${prim.WrapperName}> {
        public ${prim.CapName}Field(jclass jcls, jfieldID field, boolean isStatic) { super(jcls, field, isStatic); }
        public ${prim.Name} get${prim.CapName}(Object instance) {
            JNIEnv_ env = getEnv();
            if (isStatic)
                return env.GetStatic${prim.CapName}Field(jcls, field);
                
            jobject iref = newRef(instance);
            try {
                return env.Get${prim.CapName}Field(iref, field);
            } finally {
                delRef(iref);
            }
        }
        public void set${prim.CapName}(Object instance, ${prim.Name} value) {
            JNIEnv_ env = getEnv();
            if (isStatic)
                env.SetStatic${prim.CapName}Field(jcls, field, value);
            else {
                jobject iref = newRef(instance);
                try {
                    env.Set${prim.CapName}Field(iref, field, value);
                } finally {
                    delRef(iref);
                }
            }
        }
        public ${prim.WrapperName} get(Object instance) {
            return get${prim.CapName}(instance);
        }
        public void set(Object instance, ${prim.WrapperName} value) {
            set${prim.CapName}(instance, value);
        }
    }
#end
}
