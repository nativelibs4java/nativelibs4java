/*
	Copyright (c) 2009 Olivier Chafik (http://ochafik.free.fr/)
	
	This file is part of OpenCL4Java (http://code.google.com/p/nativelibs4java/wiki/OpenCL).
	
	OpenCL4Java is free software: you can redistribute it and/or modify
	it under the terms of the GNU Lesser General Public License as published by
	the Free Software Foundation, either version 2.1 of the License, or
	(at your option) any later version.
	
	OpenCL4Java is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU Lesser General Public License for more details.
	
	You should have received a copy of the GNU Lesser General Public License
	along with OpenCL4Java.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.nativelibs4java.opencl;
import com.bridj.Pointer;
import com.bridj.TypedPointer;
import static com.bridj.Pointer.*;

abstract class CLAbstractEntity<T extends TypedPointer> {
    protected volatile T entity;
	private final boolean nullable;
    //protected final Class<T> entityClass;

	CLAbstractEntity(/*Class<T> entityClass, */T entity) {
		this(/*entityClass, */entity, false);

	}
    CLAbstractEntity(/*Class<T> entityClass, */T entity, boolean nullable) {
		if (!nullable && entity == null) {
            throw new IllegalArgumentException("Null OpenCL " + getClass().getSimpleName() + " !");
        }
		this.nullable = nullable;
        this.entity = entity;
        //this.entityClass = entityClass;
    }

	/**
	 * Manual release of the OpenCL resources represented by this object.<br/>
	 * Note that resources are automatically released by the garbage collector, so in general there's no need to call this method.<br/>
	 * In an environment with fast allocation/deallocation of large objects, it might be safer to call release() manually, though.<br/>
	 * Note that release() does not necessarily free the object immediately : OpenCL maintains a reference count for all its objects, and an object released on the Java side might still be pointed to by running kernels or queued operations.
	 */
	public synchronized void release() {
		if (entity == null) {
            if (!nullable)
                throw new RuntimeException("This " + getClass().getSimpleName() + " has already been released ! Besides, keep in mind that manual release is not necessary, as it will automatically be done by the garbage collector.");
            else
                return;
        }

		doRelease();
	}

    public static <E extends TypedPointer, A extends CLAbstractEntity<E>> Pointer<E> getEntities(A[] objects, Pointer<E> out) {
        for (int i = 0, len = objects.length; i < len; i++)
            out.setPointer(i * Pointer.SIZE, objects[i].getEntity());
        return out;
    }
    synchronized T getEntity() {
		if (entity == null && !nullable)
			throw new RuntimeException("This " + getClass().getSimpleName() + " has been manually released and can't be used anymore !");

        return entity;
    }

	synchronized void doRelease() {
		if (entity != null) {
			clear();
			entity = null;
		}
	}
    @Override
    protected void finalize() throws Throwable {
		doRelease();
    }
	
    protected abstract void clear();

	/**
	 * Underyling implementation pointer-based hashCode computation
	 */
	@Override
	public int hashCode() {
		return getEntity() == null ? 0 : getEntity().hashCode();
	}

	/**
	 * Underyling implementation pointer-based equality test
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !getClass().isInstance(obj))
			return false;
		CLAbstractEntity<?> e = (CLAbstractEntity<?>)obj;
		return getEntity().equals(e.getEntity());
	}

}