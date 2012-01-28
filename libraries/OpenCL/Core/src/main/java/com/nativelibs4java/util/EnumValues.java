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

    private static class Cache<E extends Enum<?>> {

        final Map<Long, E> enumsByValue = new LinkedHashMap<Long, E>();
        final Map<E, Long> valuesByEnum = new LinkedHashMap<E, Long>();

        public Cache(Class<E> enumClass) {

            if (ValuedEnum.class.isAssignableFrom(enumClass)) {
                for (E e : enumClass.getEnumConstants()) {
                    long value = ((ValuedEnum)e).value();
                    enumsByValue.put(value, e);
                    valuesByEnum.put(e, value);
                }
            } else {
                for (E e : enumClass.getEnumConstants()) {
                    EnumValue ev = null;
                    try {
                        ev = enumClass.getField(e.name()).getAnnotation(EnumValue.class);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                    if (ev == null) {
                        throw new IllegalArgumentException("Enum value is not annotated with the " + EnumValue.class.getName() + " annotation : " + e);
                    }
                    long value = ev.value();
                    enumsByValue.put(value, e);
                    valuesByEnum.put(e, value);
                }
            }
        }
    }
    private static final Map<Class<? extends Enum<?>>, Cache<?>> caches = new HashMap<Class<? extends Enum<?>>, Cache<?>>();

    @SuppressWarnings("unchecked")
    private static synchronized <E extends Enum<?>> Cache<E> getCache(Class<E> enumClass) {
        Cache<E> cache = (Cache<E>) caches.get(enumClass);
        if (cache == null) {
            caches.put(enumClass, cache = new Cache(enumClass));
        }
        return cache;
    }

    /**
     * Get the first enum item in enum class E which EnumValue value is equal to value
     * @param <E> type of the enum
     * @param value
     * @param enumClass
     * @return first enum item with matching value, null if there is no matching enum item
     */
    public static <E extends Enum<E>> E getEnum(long value, Class<E> enumClass) {
        return getCache(enumClass).enumsByValue.get(value);
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
        for (Map.Entry<Long, E> pair : getCache(enumClass).enumsByValue.entrySet()) {
            long ev = pair.getKey();
            if ((ev & value) == ev) {
                set.add(pair.getValue());
            }
        }
        return set;
    }

    /**
     * Get the integer value associated with an enum item
     * @see EnumValue
     * @param enumItem
     * @return the numeric value of the enum
     */
    @SuppressWarnings("unchecked")
    public static <E extends Enum<?>> long getValue(E enumItem) {
        return getCache((Class<E>) enumItem.getDeclaringClass()).valuesByEnum.get(enumItem);
    }

    /**
     * Get the integer value resulting from ORing all the values of all the enum items present in the enum set.
     * see {@link EnumValues#getValue(java.lang.Enum) }
     * @see EnumValue
     * @param set the EnumSet to process
     * @return the OR of all the values of the enums in the set
     */
    public static <E extends Enum<E>> long getValue(EnumSet<E> set) {
        long v = 0;
        for (E e : set) {
            v |= getValue(e);
        }
        return v;
    }
}
