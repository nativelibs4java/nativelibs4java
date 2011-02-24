package org.bridj;

import java.lang.reflect.Type;

/**
 * Interface that each specific pluggable native runtime must implement.<br>
 * A runtime is attached to a class via the {@link org.bridj.ann.Runtime} annotation, so any runtime can be added in thirdparty libraries.<br>
 * A runtime typically defines NativeObject subclasses and deals with their instances lifecycle through the type information metadata {@link TypeInfo} class.<br>
 * @author ochafik
 */
public interface BridJRuntime {

	
	public interface TypeInfo<T extends NativeObject> {
		T cast(Pointer peer);
		void initialize(T instance);
		void initialize(T instance, Pointer peer);
		void initialize(T instance, int constructorId, Object[] args);
        void destroy(T instance);
		T clone(T instance) throws CloneNotSupportedException;
		BridJRuntime getRuntime();
		Type getType();

		boolean equal(T instance, T other);
		int compare(T instance, T other);
        long sizeOf(T instance);
	}
	
	void register(Type type);
	void unregister(Type type);
	<T extends NativeObject> TypeInfo<T> getTypeInfo(final Type type);

    boolean isAvailable();
	<T extends NativeObject> Class<? extends T> getActualInstanceClass(Pointer<T> pInstance, Type officialType);
    
}
