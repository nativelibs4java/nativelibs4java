/*
 * JavaCL - Java API and utilities for OpenCL
 * http://javacl.googlecode.com/
 *
 * Copyright (c) 2009-2011, Olivier Chafik (http://ochafik.com/)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Olivier Chafik nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY OLIVIER CHAFIK AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.nativelibs4java.opencl;
import org.bridj.*;
import static org.bridj.Pointer.*;

abstract class CLAbstractEntity<T extends TypedPointer> {
	private long entityPeer;
    private T entity;
	private final boolean nullable;

	CLAbstractEntity(T entity) {
		this(entity, false);
	}
    CLAbstractEntity(long entityPeer) {
		this(entityPeer, false);
	}
    CLAbstractEntity(T entity, boolean nullable) {
		this(entity, getPeer(entity), nullable);
    }

    CLAbstractEntity(long entityPeer, boolean nullable) {
		this(null, entityPeer, nullable);
    }
    private CLAbstractEntity(T entity, long entityPeer, boolean nullable) {
		this.nullable = nullable;
        this.entity = entity;
        this.entityPeer = entityPeer;
        checkNullity(entityPeer);
    }
    
    private void checkNullity(long entityPeer) {
    	if (!nullable && entityPeer == 0) {
            throw new IllegalArgumentException("Null OpenCL " + getClass().getSimpleName() + " !");
        }
    }
    
    protected void setEntity(T entity) {
    	long entityPeer = getPeer(entity);
    	checkNullity(entityPeer);
    	this.entity = entity;
    	this.entityPeer = entityPeer;
    }
    
    protected void setEntity(long entityPeer) {
    	checkNullity(entityPeer);
    	this.entity = null;
    	this.entityPeer = entityPeer;
    }

    static <T> Pointer<T> copyNonNullEntities(CLAbstractEntity[] entities, int[] countOut, ReusablePointer tmp) {
		int n;
		if (entities == null || (n = entities.length) == 0) {
		    countOut[0] = 0;
			return null;
		}
		Pointer<T> out = null;
		
		int count = 0;
		for (int i = 0; i < n; i++) {
		    CLAbstractEntity entity = entities[i];
		    if (entity != null && entity != CLEvent.FIRE_AND_FORGET) {
		        long pointer = entity.getEntityPeer();
		        if (pointer != 0) {
		            if (out == null)
                        out = tmp.getPointerToBytes(Pointer.SIZE * (n - i));
		            
                    out.setSizeTAtOffset(Pointer.SIZE * count, pointer);
                    
                    count++;
		        }
		    }
		}
        countOut[0] = count;
        return out;
	}
    
	/**
	 * Manual release of the OpenCL resources represented by this object.<br/>
	 * Note that resources are automatically released by the garbage collector, so in general there's no need to call this method.<br/>
	 * In an environment with fast allocation/deallocation of large objects, it might be safer to call release() manually, though.<br/>
	 * Note that release() does not necessarily free the object immediately : OpenCL maintains a reference count for all its objects, and an object released on the Java side might still be pointed to by running kernels or queued operations.
	 */
	public synchronized void release() {
		if (entityPeer == 0) {
            if (!nullable)
                throw new RuntimeException("This " + getClass().getSimpleName() + " has already been released ! Besides, keep in mind that manual release is not necessary, as it will automatically be done by the garbage collector.");
            else
                return;
        }

		doRelease();
	}

    public static <E extends TypedPointer, A extends CLAbstractEntity<E>> Pointer<E> getEntities(A[] objects, Pointer<E> out) {
        for (int i = 0, len = objects.length; i < len; i++)
            out.setSizeTAtOffset(i * Pointer.SIZE, objects[i].getEntityPeer());
        return out;
    }
    private void checkNullity() {
    	if (entityPeer == 0 && !nullable)
			throw new RuntimeException("This " + getClass().getSimpleName() + " has been manually released and can't be used anymore !");
    }
    
    protected abstract T createEntityPointer(long peer);
    
    synchronized T getEntity() {
    	checkNullity();
    	if (entity == null && entityPeer != 0) {
    		entity = createEntityPointer(entityPeer);
    	}
		return entity;
    }
    synchronized long getEntityPeer() {
    	checkNullity();
        return entityPeer;
    }

	synchronized void doRelease() {
		if (entityPeer != 0) {
			clear();
			entity = null;
			entityPeer = 0;
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
		return entityPeer == 0 
			? 0 
			: entity == null 
				? Long.valueOf(entityPeer).hashCode() 
				: entity.hashCode();
	}

	/**
	 * Underyling implementation pointer-based equality test
	 */
	@Override
	public boolean equals(Object obj) {
		if (!getClass().isInstance(obj))
			return false;
		CLAbstractEntity<?> e = (CLAbstractEntity<?>)obj;
		return getEntityPeer() == e.getEntityPeer();
	}

}