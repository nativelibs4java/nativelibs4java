/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.util;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author ochafik
 */
public abstract class Cache<K, V> {
	Map<K, V> map;
	public Cache() {
		this(new HashMap<K, V>());
	}
	public Cache(Map<K, V> map) {
		this.map = map;
	}
	public synchronized V get(K key) {
		V value = map.get(key);
		if (value == null)
			map.put(key, value = createValue(key));
		return value;
	}
	public abstract V createValue(K key);
}
