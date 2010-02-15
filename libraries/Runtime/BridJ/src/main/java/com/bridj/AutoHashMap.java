package com.bridj;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

class AutoHashMap<K, V> extends HashMap<K, V> {
	public AutoHashMap(Class<V> valueClass) {
		try {
			valueConstructor = valueClass.getConstructor();
		} catch (Exception ex) {
			throw new RuntimeException("No accessible default constructor in class " + (valueClass == null ? "null" : valueClass.getName()), ex);
		}
	}
	Constructor valueConstructor;
	protected V newInstance(K key) {
		try {
			return (V) valueConstructor.newInstance();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to call constructor " + valueConstructor, ex);
		}
	}
	public V get(Object key) {
		V v = super.get(key);
		if (v == null)
			put((K)key, v = newInstance((K)key));
		return v;
	}
}