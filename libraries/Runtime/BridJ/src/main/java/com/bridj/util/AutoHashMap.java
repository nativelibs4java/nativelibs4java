package com.bridj.util;

import java.lang.reflect.Constructor;
import java.util.HashMap;

public class AutoHashMap<K, V> extends HashMap<K, V> {
	private static final long serialVersionUID = 1693618702345072811L;
	public AutoHashMap(Class<V> valueClass) {
		try {
			valueConstructor = valueClass.getConstructor();
		} catch (Exception ex) {
			throw new RuntimeException("No accessible default constructor in class " + (valueClass == null ? "null" : valueClass.getName()), ex);
		}
	}
	Constructor<V> valueConstructor;
	protected V newInstance(K key) {
		try {
			return valueConstructor.newInstance();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to call constructor " + valueConstructor, ex);
		}
	}
	@SuppressWarnings("unchecked")
	public V get(Object key) {
		V v = super.get(key);
		if (v == null)
			put((K)key, v = newInstance((K)key));
		return v;
	}
}