/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj;

import java.util.Collections;
import java.util.Iterator;


public interface ValuedEnum<E extends Enum<E>> {
    long value();
//
//    public static class EnumWrapper<EE extends Enum<EE>> implements ValuedEnum<EE> {
//        EE enumValue;
//        public EnumWrapper(EE enumValue) {
//            if (enumValue == null)
//                throw new IllegalArgumentException("Null enum value !");
//            this.enumValue = enumValue;
//        }
//
//        @Override
//        public long value() {
//            return enumValue.ordinal();
//        }
//
//        @Override
//        public Iterator<EE> iterator() {
//            return Collections.singleton(enumValue).iterator();
//        }
//
//    }
//
//    public enum MyEnum implements ValuedEnum<MyEnum> {
//        A(1), B(2);
//
//        MyEnum(long value) { this.value = value; }
//        long value;
//        @Override
//        public long value() {
//            return ordinal();
//        }
//
//        @Override
//        public Iterator<MyEnum> iterator() {
//            return Collections.singleton(this).iterator();
//        }
//
//        public static ValuedEnum<MyEnum> fromValue(long value) {
//            return FlagSet.fromValue(value, values());
//        }
//    }
}