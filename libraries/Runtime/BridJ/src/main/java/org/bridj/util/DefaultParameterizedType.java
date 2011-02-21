/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bridj.util;

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
    
    public static Type paramType(Type rawType, Type... actualTypeArguments) {
    	return new DefaultParameterizedType(rawType, actualTypeArguments);
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
    
    
	@Override
	public int hashCode() {
		int h = getRawType().hashCode();
		if (getOwnerType() != null)
			h ^= getOwnerType().hashCode();
		for (int i = 0, n = actualTypeArguments.length; i < n; i++)
			h ^= actualTypeArguments[i].hashCode();
		return h;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof DefaultParameterizedType))
			return false;
		
		DefaultParameterizedType t = (DefaultParameterizedType)o;
		if (!getRawType().equals(t.getRawType()))
			return false;
		if (!getOwnerType().equals(t.getOwnerType()))
			return false;
		
		Object[] tp = t.actualTypeArguments;
		if (actualTypeArguments.length != tp.length)
			return false;
		
		for (int i = 0, n = actualTypeArguments.length; i < n; i++)
			if (!actualTypeArguments[i].equals(tp[i]))
				return false;
			
		return true;
	}
}
