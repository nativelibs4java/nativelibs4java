/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.util;

import java.util.*;

/**
 * Methods to ease conversion between EnumValue-annotated enums and their integer value.
 * @author ochafik
 */
public class EnumValues {

	/**
	 * Get the first enum item in enum class E which EnumValue value is equal to value
	 * @param <E> type of the enum
	 * @param value
	 * @param enumClass
	 * @return first enum item with matching value, null if there is no matching enum item
	 */
	public static <E extends Enum<E>> E getEnum(long value, Class<E> enumClass) {
		for (E e : enumClass.getEnumConstants())
			if (getValue(e) == value)
				return e;
		return null;
	}

	/**
	 * Get the set of all the enum item in enum class E which EnumValue value flags are all present in value
	 * @param <E> type of the enum
	 * @param value
	 * @param enumClass
	 * @return enum items with matching value flags
	 */
	public static <E extends Enum<E>> EnumSet<E> getEnumSet(long value, Class<E> enumClass) {
		EnumSet<E> set = EnumSet.noneOf(enumClass);
		for (E e : enumClass.getEnumConstants()) {
			long ev = getValue(e);
			if ((ev & value) == ev)
				set.add(e);
		}
		return set;
	}

	/**
	 * Get the integer value associated with an enum item
	 * @see EnumValue
	 * @param enumItem
	 * @return
	 */
	public static long getValue(Enum enumItem) {
		EnumValue v = null;
		try {
			v = enumItem.getClass().getField(enumItem.name()).getAnnotation(EnumValue.class);
		} catch (Exception ex) {
		}
		if (v == null)
			throw new IllegalArgumentException("Enum value is not annotated with the " + EnumValue.class.getName() + " annotation : " + enumItem);
		return v.value();
	}

	/**
	 * Get the integer value resulting from ORing all the values of all the enum items present in the enum set.
	 * @see EnumValues#getValue(java.lang.Enum) 
	 * @see EnumValue
	 * @param enumItem
	 * @return
	 */
	public static <E extends Enum<E>> long getValue(EnumSet<E> set) {
		long v = 0;
		for (E e : set)
			v |= getValue(e);
		return v;
	}
}
