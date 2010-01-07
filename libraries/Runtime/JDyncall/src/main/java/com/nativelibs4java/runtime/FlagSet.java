/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.runtime;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.WeakHashMap;

/**
 *
 * @author ochafik
 */
public abstract class FlagSet<E> implements Iterable<E> {
    private final Class<E> enumClass;
    private final long value;
    private E[] enumClassValues;

    protected FlagSet(Class<E> enumClass, long value) {
        this.enumClass = enumClass;
        this.value = value;
    }

    private static Map<Class<?>, Object[]> enumsCache = new WeakHashMap<Class<?>, Object[]>();

    protected abstract long getValue(E enumItem);

    private static synchronized <EE> EE[] getValues(Class<EE> enumClass) {
        Object[] values = enumsCache.get(enumClass);
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


    /**
     * TODO
     * @param value
     * @return
     */
    public static <EE extends ValuedEnum> FlagSet<EE> fromValuedEnums(long value, Class<EE> enumClass) {
        return new FlagSet<EE>(enumClass, value) {
            @Override
            protected long getValue(EE enumItem) {
                return enumItem.value();
            }

            @Override
            public Iterator<EE> iterator() {
                return getMatchingEnums().iterator();
            }

        };
    }
    
    public static <EE extends Enum<EE>> FlagSet<EE> fromEnum(long value, Class<EE> enumClass) {
        if (ValuedEnum.class.isAssignableFrom(enumClass))
            return (FlagSet<EE>)fromValuedEnums(value, (Class)enumClass);
        
        return new FlagSet<EE>(enumClass, value) {

            @Override
            protected long getValue(EE enumItem) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            /**
             * TODO JavaDoc
             * @return
             */
            @Override
            public Iterator<EE> iterator() {
                // TODO optimize this
                return Arrays.asList(getSingleMatchingEnum()).iterator();
            }
        };
    }
    /**
     * TODO
     * @param value
     * @return
     */
    public static FlagSet<Long> fromValue(final long value) {
        return new FlagSet(null, value) {
            @Override
            protected long getValue(Object enumItem) {
                throw new UnsupportedOperationException("Not supported");
            }
            
            @Override
            public Iterator<Long> iterator() {
                List<Long> list = new ArrayList<Long>();
                for (int i = 0; i < 64; i++) {
                    long bit = 1L << i;
                    if ((value & bit) != 0)
                        list.add(bit);
                }
                return list.iterator();
            }

        };
    }

    /**
     * TODO
     * @param <EE>
     * @param enumValue
     * @return
     */
    public static <EE extends Enum<EE>> FlagSet<EE> fromEnum(final EE enumValue) {
        if (enumValue == null)
            return null;
        if (enumValue instanceof ValuedEnum)
            return (FlagSet<EE>)(FlagSet)fromValuedEnums((ValuedEnum)enumValue);

        return new FlagSet<EE>((Class<EE>)enumValue.getClass(), enumValue.ordinal()) {
            @Override
            protected long getValue(EE enumItem) {
                return enumItem.ordinal();
            }

            @Override
            public Iterator<EE> iterator() {
                return new UniqueIterator<EE>() {
                    @Override
                    public EE get() {
                        return enumValue;
                    }
                };
            }

        };
    }

    /**
     * TODO
     * @param <EE>
     * @param enumValues
     * @return
     */
    public static <EE extends ValuedEnum> FlagSet<EE> fromValuedEnums(final EE... enumValues) {
        if (enumValues.length == 0)
            return null;

        Class<EE> enumClass = (Class<EE>) enumValues[0].getClass();

        long value = 0;
        for (EE enumValue : enumValues)
            value |= enumValue.value();

        return new FlagSet<EE>(enumClass, value) {
            @Override
            protected long getValue(EE enumItem) {
                return enumItem.value();
            }

            @Override
            public Iterator<EE> iterator() {
                return new Iterator<EE>() {

                    int index = -1;

                    @Override
                    public boolean hasNext() {
                        return index < enumValues.length - 1;
                    }

                    @Override
                    public EE next() {
                        if (!hasNext())
                            throw new NoSuchElementException();

                        return enumValues[++index];
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Not supported.");
                    }
                };
            }

        };
    }

    /**
     * TODO
     * @return
     */
    public long getValue() {
        return value;
    }

    protected Object[] getEnumClassValues() {
        return enumClassValues == null ? enumClassValues = getValues(enumClass) : enumClassValues;
    }

    protected E getSingleMatchingEnum() {
        if (enumClass != null)
            for (Object e : getEnumClassValues())
                if (getValue((E)e) == value)
                    return (E)e;

        return null;
    }

    protected List<E> getMatchingEnums() {
        List<E> ret = new ArrayList<E>();
        if (enumClass != null) {
            for (Object e : getEnumClassValues()) {
                long eMask = getValue((E)e);
                if ((value & eMask) == eMask)
                    ret.add((E)e);
            }
        }

        return ret;
    }

    protected abstract static class UniqueIterator<I> implements Iterator<I> {
        boolean done;
        @Override
        public boolean hasNext() {
            return !done;
        }

        @Override
        public I next() {
            if (done)
                throw new NoSuchElementException();
            done = true;
            return get();
        }

        protected abstract I get();

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported (" + FlagSet.class.getName() + " instances are immutable).");
            }
    }

}
