/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 *
 * @author ochafik
 */
public class FlagSet<E extends Enum<E>> implements ValuedEnum<E> {
    private final long value;
    private final Class<E> enumClass;
    private E[] enumClassValues;

    protected FlagSet(long value, Class<E> enumClass, E[] enumClassValues) {
        this.enumClass = enumClass;
        this.value = value;
        this.enumClassValues = enumClassValues;
    }

    private static Map<Class<?>, Object[]> enumsCache = new WeakHashMap<Class<?>, Object[]>();

    @SuppressWarnings("unchecked")
	private static synchronized <EE extends Enum<EE>> EE[] getValues(Class<EE> enumClass) {
        EE[] values = (EE[])enumsCache.get(enumClass);
        if (values == null) {
            try {
                Method valuesMethod = enumClass.getMethod("values");
                Class<?> valuesType = valuesMethod.getReturnType();
                if (!valuesType.isArray() || !ValuedEnum.class.isAssignableFrom(valuesType.getComponentType()))
                    throw new RuntimeException();
                enumsCache.put(enumClass, values = (EE[])valuesMethod.invoke(null));
            } catch (Exception ex) {
                throw new IllegalArgumentException("Class " + enumClass + " does not have a public static " + ValuedEnum.class.getName() + "[] values() method.", ex);
            }
        }
        return (EE[])values;
    }

    //@Override
    public Iterator<E> iterator() {
        return getMatchingEnums().iterator();
    }
    public static <EE extends Enum<EE>> FlagSet<EE> fromValue(long value, Class<EE> enumClass) {
        return new FlagSet<EE>(value, enumClass, null);
    }
    public static <EE extends Enum<EE>> FlagSet<EE> fromValue(long value, EE[] enumValue) {
        return new FlagSet<EE>(value, null, enumValue);
    }
    /**
     * TODO
     * @param value
     * @return
     */
    public static List<Long> getBits(final long value) {
        List<Long> list = new ArrayList<Long>();
        for (int i = 0; i < 64; i++) {
            long bit = 1L << i;
            if ((value & bit) != 0)
                list.add(bit);
        }
        return list;
    }

    /**
     * TODO
     * @return
     */
    @Override
    public long value() {
        return value;
    }

    protected E[] getEnumClassValues() {
        return enumClassValues == null ? enumClassValues = getValues(enumClass) : enumClassValues;
    }

    protected List<E> getMatchingEnums() {
        List<E> ret = new ArrayList<E>();
        if (enumClass != null) {
            for (E e : getEnumClassValues()) {
                long eMask = ((ValuedEnum<?>)e).value();
                if ((value & eMask) == eMask)
                    ret.add((E)e);
            }
        }

        return ret;
    }

	public static <E extends Enum<E>> FlagSet<E> fromValues(E... enumValues) {
		long value = 0;
		Class cl = null;
		for (E enumValue : enumValues) {
			if (cl == null)
				cl = enumValue.getClass();
			value |= ((ValuedEnum)enumValue).value();
		}
		return new FlagSet<E>(value, cl, enumValues);
	}

}
