package com.bridj;

import com.bridj.AbstractBridJRuntime;
import java.io.FileNotFoundException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.bridj.BridJ;
import com.bridj.BridJRuntime;
import com.bridj.Callback;
import com.bridj.MethodCallInfo;
import com.bridj.NativeEntities;
import com.bridj.NativeLibrary;
import com.bridj.NativeObject;
import com.bridj.Pointer;
import com.bridj.Demangler.Symbol;
import com.bridj.NativeEntities.Builder;
import com.bridj.ann.Struct;
import com.bridj.ann.Virtual;
import com.bridj.cpp.CPPObject;
import com.bridj.util.AutoHashMap;
import java.lang.reflect.Type;

public class CRuntime extends AbstractBridJRuntime {

	Set<Class<?>> registeredClasses = new HashSet<Class<?>>();
	CallbackNativeImplementer callbackNativeImplementer;

    public CRuntime() {
        callbackNativeImplementer = new CallbackNativeImplementer(BridJ.getOrphanEntities(), this);
    }
    public boolean isAvailable() {
        return true;
    }
	@Override
	public <T extends NativeObject> Class<? extends T> getActualInstanceClass(Pointer<T> pInstance, Class<T> officialType) {
		return officialType;
	}

	@Override
	public void register(Class<?> type) {
		if (!registeredClasses.add(type))
			return;

        assert log(Level.INFO, "Registering type " + type.getName());
        
		int typeModifiers = type.getModifiers();
		
		AutoHashMap<NativeEntities, NativeEntities.Builder> builders = new AutoHashMap<NativeEntities, NativeEntities.Builder>(NativeEntities.Builder.class);
		try {
            Set<Method> handledMethods = new HashSet<Method>();
			if (StructObject.class.isAssignableFrom(type)) {
				StructIO io = StructIO.getInstance(type, type, this); // TODO handle differently with templates...
                io.build();
                StructIO.FieldIO[] fios = io == null ? null : io.getFields();
                if (fios != null)
                    for (StructIO.FieldIO fio : fios) {
                        NativeEntities.Builder builder = builders.get(BridJ.getOrphanEntities());

                        try {
                            {
                                MethodCallInfo getter = new MethodCallInfo(fio.getter);
                                getter.setIndex(fio.index);
                                builder.addGetter(getter);
                                handledMethods.add(fio.getter);
                            }
                            if (fio.setter != null) {
                                MethodCallInfo setter = new MethodCallInfo(fio.setter);
                                setter.setIndex(fio.index);
                                builder.addSetter(setter);
                                handledMethods.add(fio.setter);
                            }
                        } catch (FileNotFoundException ex) {
                            ex.printStackTrace();
                        }
                    }
			}
			
			if (Callback.class.isAssignableFrom(type)) {
				if (Callback.class == type)
					return;
				
				if (Modifier.isAbstract(typeModifiers))
	                callbackNativeImplementer.getCallbackImplType((Class) type);
			}
		
		
//		for (; type != null && type != Object.class; type = type.getSuperclass()) {
			try {
				NativeLibrary typeLibrary = getNativeLibrary(type);
				for (Method method : type.getDeclaredMethods()) {
                    if (handledMethods.contains(method))
                        continue;
					try {
						int modifiers = method.getModifiers();
						if (!Modifier.isNative(modifiers))
							continue;
						
						NativeEntities.Builder builder = builders.get(BridJ.getNativeEntities(method));
						NativeLibrary methodLibrary = BridJ.getNativeLibrary(method);
						
						registerNativeMethod(type, typeLibrary, method, methodLibrary, builder);

                        handledMethods.add(method);
					} catch (Exception ex) {
						assert log(Level.SEVERE, "Method " + method.toGenericString() + " cannot be mapped : " + ex, ex);
					}
				}
			} catch (Exception ex) {
				throw new RuntimeException("Failed to register class " + type.getName(), ex);
			}
//		}
		} finally {
			for (Map.Entry<NativeEntities, NativeEntities.Builder> e : builders.entrySet()) {
				e.getKey().addDefinitions(type, e.getValue());
			}
			
			type = type.getSuperclass();
			if (type != null && type != Object.class)
				register(type);
		}
	}

	protected NativeLibrary getNativeLibrary(Class<?> type) throws FileNotFoundException {
		return BridJ.getNativeLibrary(type);
	}
	protected void registerNativeMethod(Class<?> type, NativeLibrary typeLibrary, Method method, NativeLibrary methodLibrary, Builder builder) throws FileNotFoundException {
        MethodCallInfo mci = new MethodCallInfo(method);
		if (Callback.class.isAssignableFrom(type)) {
            log(Level.INFO, "Registering java -> native callback : " + method);
            builder.addJavaToNativeCallback(mci);
        } else {
            Symbol symbol = methodLibrary == null ? null : methodLibrary.getSymbol(method);
            if (symbol == null)
            {
//                for (Demangler.Symbol symbol : methodLibrary.getSymbols()) {
//                    if (symbol.matches(method)) {
//                        address = symbol.getAddress();
//                        break;
//                    }
//                }
//                if (address == null) {
                    log(Level.SEVERE, "Failed to get address of method " + method);
                    return;
//                }
            }
            mci.setForwardedPointer(symbol.getAddress());
            builder.addFunction(mci);
            log(Level.INFO, "Registering " + method + " as C function " + symbol.getName());
        }
	}
	
	public <T extends NativeObject> Pointer<T> allocate(Class<T> type, int constructorId, Object... args) {
	    if (Callback.class.isAssignableFrom(type)) {
        	if (constructorId != -1 || args.length != 0)
        		throw new RuntimeException("Callback should have a constructorId == -1 and no constructor args !");
        	return null;//newCallbackInstance(type);
        }
        throw new RuntimeException("Cannot allocate instance of type " + type.getName() + " (unhandled NativeObject subclass)");
	}
	
	static final int defaultObjectSize = 128;
	public static final String PROPERTY_bridj_c_defaultObjectSize = "bridj.c.defaultObjectSize";
	
	public int getDefaultStructSize() {
		String s = System.getProperty(PROPERTY_bridj_c_defaultObjectSize);
    	if (s != null)
    		try {
    			return Integer.parseInt(s);
	    	} catch (Throwable th) {
	    		log(Level.SEVERE, "Invalid value for property " + PROPERTY_bridj_c_defaultObjectSize + " : '" + s + "'");
	    	}
    	return defaultObjectSize;
	}
	protected int sizeOf(Class<? extends StructObject> structClass, Type structType, StructIO io) {
		if (io == null)
			io = StructIO.getInstance(structClass, structType, this);
		int size;
		if (io == null || (size = io.getStructSize()) == 0)
			return getDefaultStructSize();
		return size;	
    }

    static Method getUniqueAbstractCallbackMethod(Class type) {
        Class<?> parent = null;
    	while ((parent = type.getSuperclass()) != null && parent != Callback.class) {
    		type = parent;
    	}

    	Method method = null;
    	for (Method dm : type.getDeclaredMethods()) {
    		int modifiers = dm.getModifiers();
    		if (!Modifier.isAbstract(modifiers))
    			continue;

    		method = dm;
    		break;
    	}
    	if (method == null)
    		throw new RuntimeException("Type doesn't have any abstract method : " + type.getName());
    	return method;
    }

    @Override
    public <T extends NativeObject> Class<? extends T> getTypeForCast(Class<T> type) {
        if (Callback.class.isAssignableFrom(type))
            return callbackNativeImplementer.getCallbackImplType((Class) type);
        else
            return super.getTypeForCast(type);
    }


    private <T extends Callback<?>> Pointer<T> registerCallbackInstance(T instance) {
		try {
            Class<?> c = instance.getClass();
			MethodCallInfo mci = new MethodCallInfo(getUniqueAbstractCallbackMethod(c));
            mci.setDeclaringClass(c);
            mci.setJavaCallback(instance);
            final long handle = JNI.createCToJavaCallback(mci);
            long peer = JNI.getActualCToJavaCallback(handle);
            return (Pointer)Pointer.pointerToAddress(peer, c, new Pointer.Deallocator() {

                @Override
                public void deallocate(long peer) {
                    JNI.freeCToJavaCallback(handle);
                }
            });
		} catch (FileNotFoundException e) {
			throw new RuntimeException("Failed to register callback instance of type " + instance.getClass().getName(), e);
		}
	}
    @Override
    public void initialize(NativeObject instance) {
        if (!BridJ.isCastingNativeObjectInCurrentThread()) {
            if (instance instanceof Callback<?>)
                setNativeObjectPeer(instance, registerCallbackInstance((Callback<?>)instance));
            else
                initialize(instance, -1);
        } else if (instance instanceof StructObject) {
            StructObject s = (StructObject)instance;
            Class<? extends StructObject> c = (Class)instance.getClass();
    		StructIO io = StructIO.getInstance(c, c, this);
    		s.io = io;
        }
    }


    @Override
    public void initialize(NativeObject instance, Pointer peer) {
        instance.peer = peer;
        Class c = instance.getClass();
        ((StructObject)instance).io = StructIO.getInstance(c, c, this);
    }

    
    @Override
    public void initialize(NativeObject instance, int constructorId, Object... args) {
    	if (!(instance instanceof StructObject))
    		return;
    	
    	StructObject s = (StructObject)instance;
    	if (constructorId < 0) {
    		
    		Class<? extends StructObject> c = (Class)instance.getClass();
    		StructIO io = StructIO.getInstance(c, c, this);
    		s.io = io; 
    		instance.peer = Pointer.allocate(PointerIO.getInstance(io), io.getStructSize());//sizeOf(c, c, io));
    	} else
    		throw new UnsupportedOperationException("TODO implement structs constructors !");
    }

    protected void setNativeObjectPeer(NativeObject instance, Pointer<? extends NativeObject> peer) {
        instance.peer = peer;
    }
    
    @Override
    public void destroy(NativeObject instance) {
        if (instance instanceof Callback)
            return;        
    }
    
    @Override
    public <T extends NativeObject> T clone(T instance) throws CloneNotSupportedException {
    	if (instance instanceof NativeObject) {
    		return (T) Pointer.getPeer(instance).toNativeObject(instance.getClass());
    	}
    	return super.clone(instance);
    }

}