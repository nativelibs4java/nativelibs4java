/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nativelibs4java.util;

import java.util.EnumSet;

/**
 *
 * @author ochafik
 */
public class ValuedEnums {
	public static <E extends Enum<E>> long or(EnumSet<E> enums) {
		long value = 0;
		for (E e : enums)
			value |= ((ValuedEnum)e).value();
		return value;
	}
	public static <E extends Enum<E>> long and(EnumSet<E> enums) {
		long value = 0;
		boolean first = true;
		for (E e : enums) {
			long ev = ((ValuedEnum)e).value();
			if (first) {
				value = ev;
				first = false;
			} else
				value &= ev;
		}
		return value;
	}
}