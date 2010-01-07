/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.runtime;

public interface ValuedEnum<E extends Enum<E>> {
    long value();

    public static class EnumWrapper<EE extends Enum<EE>> implements ValuedEnum<EE> {
        EE enumValue;
        public EnumWrapper(EE enumValue) {
            if (enumValue == null)
                throw new IllegalArgumentException("Null enum value !");
            this.enumValue = enumValue;
        }

        @Override
        public long value() {
            return enumValue.ordinal();
        }

    }
}