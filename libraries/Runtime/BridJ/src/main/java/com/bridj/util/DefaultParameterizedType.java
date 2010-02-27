/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.bridj.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 *
 * @author Olivier
 */
public class DefaultParameterizedType implements ParameterizedType {
    private final Type[] actualTypeArguments;
    private final Type ownerType;
    private final Type rawType;

    public DefaultParameterizedType(Type ownerType, Type rawType, Type[] actualTypeArguments) {
        this.ownerType = ownerType;
        this.actualTypeArguments = actualTypeArguments;
        this.rawType = rawType;
    }
    public DefaultParameterizedType(Type rawType, Type... actualTypeArguments) {
        this(null, rawType, actualTypeArguments);
    }
    @Override
    public Type[] getActualTypeArguments() {
        return actualTypeArguments.clone();
    }

    @Override
    public java.lang.reflect.Type getOwnerType() {
        return ownerType;
    }

    @Override
    public java.lang.reflect.Type getRawType() {
        return rawType;
    }
}
