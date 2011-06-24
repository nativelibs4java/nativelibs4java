package org.bridj;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;

import static org.bridj.Pointer.*;
import static org.junit.Assert.*;

public class MiscBugsTest {
  
	/**
	 * Issue 37 : BridJ: org.bridj.Pointer#iterator for native-allocated pointers is empty
	 * http://code.google.com/p/nativelibs4java/issues/detail?id=37
	 */
	@Test
	public void emptyIteratorFromUnmanagedPointer() {
		Pointer<Byte> ptr = allocateBytes(10);
		assertTrue(!ptr.toList().isEmpty());
		assertTrue(ptr.iterator().next() != null);
		
		Pointer<Byte> unmanaged = pointerToAddress(ptr.getPeer()).as(Byte.class);
		assertTrue(!unmanaged.toList().isEmpty());
		assertTrue(unmanaged.iterator().next() != null);
	}
	
	/**
	 * Issue 47: Pointer#pointerToAddress(long, Class, Releaser) does not use releaser argument
	 * http://code.google.com/p/nativelibs4java/issues/detail?id=37
	 */
	@Test
	public void usePointerReleaser() {
		final boolean[] released = new boolean[1];
		Pointer<Integer> p = allocateInt();
		long address = p.getPeer();
		
		{
			Pointer pp = pointerToAddress(address);
			assertEquals(address, pp.getPeer());
		}
		
		{
			Pointer pp = pointerToAddress(address, 123);
			assertEquals(address, pp.getPeer());
			assertEquals(123, pp.getValidBytes());
		}
		
		Releaser releaser = new Releaser() {
			//@Override
			public void release(Pointer<?> p) {
				released[0] = true;
			}
		};
		
		{
			released[0] = false;
			Pointer pp = pointerToAddress(address, Integer.class, releaser);
			assertEquals(address, pp.getPeer());
			assertEquals(Integer.class, pp.getTargetType());
			pp.release();
			assertEquals(true, released[0]);
		}
		
		{
			Pointer pp = pointerToAddress(address, PointerIO.getIntInstance());
			assertEquals(address, pp.getPeer());
			assertEquals(Integer.class, pp.getTargetType());
		}
		
		{
			released[0] = false;
			Pointer pp = pointerToAddress(address, PointerIO.getIntInstance(), releaser);
			assertEquals(address, pp.getPeer());
			assertEquals(Integer.class, pp.getTargetType());
			pp.release();
			assertEquals(true, released[0]);
		}
		
		{
			released[0] = false;
			Pointer pp = pointerToAddress(address, releaser);
			assertEquals(address, pp.getPeer());
			pp.release();
			assertEquals(true, released[0]);
		}
		
		{
			released[0] = false;
			Pointer pp = pointerToAddress(address, 123, releaser);
			assertEquals(address, pp.getPeer());
			assertEquals(123, pp.getValidBytes());
			pp.release();
			assertEquals(true, released[0]);
		}
		
		{
			Pointer pp = pointerToAddress(address, Integer.class);
			assertEquals(address, pp.getPeer());
			assertEquals(Integer.class, pp.getTargetType());
		}
		
		{
			Pointer pp = pointerToAddress(address, 123, PointerIO.getIntInstance());
			assertEquals(address, pp.getPeer());
			assertEquals(123, pp.getValidBytes());
			assertEquals(Integer.class, pp.getTargetType());
		}
		
	}
}
