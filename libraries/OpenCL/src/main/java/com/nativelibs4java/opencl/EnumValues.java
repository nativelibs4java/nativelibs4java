/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.opencl;

import java.util.*;

/**
 * Methods to ease conversion between EnumValue-annotated enums and their integer value.
 * @author ochafik
 */
public class EnumValues {

	public static <E extends Enum<E>> E getEnum(long v, Class<E> ec) {
		for (E e : ec.getEnumConstants())
			if (getValue(e) == v)
				return e;
		//throw new NoSuchElementException("No value " + v + " for enum " + ec.getName());
		return null;
	}
	public static <E extends Enum<E>> EnumSet<E> getEnumSet(long v, Class<E> ec) {
		EnumSet<E> set = EnumSet.noneOf(ec);
		for (E e : ec.getEnumConstants()) {
			long ev = getValue(e);
			if ((ev & v) == ev)
				set.add(e);
		}
		return set;
	}
	public static long getValue(Enum e) {
		EnumValue v = null;
		try {
			v = e.getClass().getField(e.name()).getAnnotation(EnumValue.class);
		} catch (Exception ex) {
		}
		if (v == null)
			throw new IllegalArgumentException("Enum value is not annotated with the " + EnumValue.class.getName() + " annotation : " + e);
		return v.value();
	}
	public static <E extends Enum<E>> long getValue(EnumSet<E> set) {
		long v = 0;
		for (E e : set)
			v |= getValue(e);
		return v;
	}
}
