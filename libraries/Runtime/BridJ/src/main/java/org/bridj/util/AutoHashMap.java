package org.bridj.util;

import java.lang.reflect.Constructor;
import java.util.HashMap;

/**
 * HashMap that creates its missing values automatically, using the value class' default constructor (override @see AutoHashMap#newInstance(Object) to call another constructor)
 */
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
	public synchronized V get(Object key) {
		V v = super.get(key);
		if (v == null)
			put((K)key, v = newInstance((K)key));
		return v;
	}
}