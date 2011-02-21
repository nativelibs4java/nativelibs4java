package org.bridj;

import org.bridj.Dyncall.CallingConvention;
import java.io.FileNotFoundException;

import java.util.Collection;

import org.junit.Test;
import static org.junit.Assert.*;

import org.bridj.ann.Constructor;
import org.bridj.ann.Convention;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
import org.bridj.ann.Symbol;
import org.bridj.ann.Template;
import org.bridj.ann.Ptr;
import org.bridj.ann.Virtual;
import org.bridj.cpp.CPPObject;


import org.bridj.BridJ;
import org.bridj.Pointer;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
import org.bridj.ann.Name;
import org.bridj.ann.Runtime;
import org.bridj.ann.Virtual;
import org.bridj.cpp.CPPRuntime;

import java.lang.reflect.Type;

import org.junit.After;
import org.junit.Before;
import static org.bridj.Pointer.*;

///http://www.codesourcery.com/public/cxx-abi/cxx-vtable-ex.html
@Library("test")
@Runtime(CPPRuntime.class)
public class CPPTemplateTest {
	static {
		BridJ.register();
	}

	@Template({ Integer.class, Class.class })
	public static class InvisibleSourcesTemplate<T> extends CPPObject {
		public final int n;
		@Constructor(0)
		public InvisibleSourcesTemplate(int n, Type t, int arg) {
			super(0, n, t, arg);
			this.n = n;
		}
		public native Pointer<T> createSome();
		public native void deleteSome(Pointer<T> pValue);
	}
	@Template({ Class.class })
	public static class Temp1<T> extends CPPObject { }
	
	@Template({ Class.class, Class.class })
	public static class Temp2<T1, T2> extends CPPObject { }
	
	@Template({ Class.class, Integer.class })
	public static class TempV<T> extends CPPObject { }
	
	@Test
	public void invisibleSourcesTemplateIntegerTest() {
		invisibleSourcesTemplateTest(Integer.class);
	}
	<T> void invisibleSourcesTemplateTest(Class<T> t) {
		InvisibleSourcesTemplate<T> ii = new InvisibleSourcesTemplate(10, t, 4);
		Pointer<T> p = ii.createSome();
		T v = p.get();
		System.out.println("Template created value : " + v);
		ii.deleteSome(p);
	}
}

