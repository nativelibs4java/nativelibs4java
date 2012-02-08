package com.nativelibs4java.mono.examples;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.IntBuffer;

import com.nativelibs4java.mono.bridj.MonoLibrary;
import com.nativelibs4java.mono.bridj.MonoObject;
import com.nativelibs4java.mono.bridj.MonoLibrary.MonoClass;
import com.nativelibs4java.mono.bridj.MonoLibrary.MonoDomain;
import com.nativelibs4java.mono.bridj.MonoLibrary.MonoImage;

import org.bridj.*;
import static org.bridj.Pointer.*;
import static org.bridj.util.DefaultParameterizedType.*;

public class HelloDotNetWorldBridJ {
	public static void main(String[] args) {
		try {
			String path = "C:\\Program Files\\Mono-2.4.2.3\\bin\\mono.dll";
			if (!new File(path).exists())
				path = "/Library/Frameworks/Mono.framework/Mono";
			
			if (args.length == 1)
				path = args[0];
			helloDotNetWorld(new File(path));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private static void helloDotNetWorld(File monoDLL) throws FileNotFoundException {
		if (!monoDLL.exists())
			throw new FileNotFoundException("No such dll : " + monoDLL);
		monoDLL = monoDLL.getAbsoluteFile();
		System.setProperty("java.library.path", monoDLL.getParent());
		System.setProperty("library.mono", monoDLL.toString());
		System.load(monoDLL.toString());


		final MonoLibrary m = new MonoLibrary();
		final Pointer<MonoDomain> domain = m.mono_jit_init(pointerToCString(monoDLL.toString()));
		if (domain == null)
			throw new RuntimeException("Unable to init Mono's runtime !");

        Pointer<ValuedEnum<MonoLibrary.MonoImageOpenStatus > > loadStatus = allocate(paramType(ValuedEnum.class, MonoLibrary.MonoImageOpenStatus.class));
		Pointer<MonoImage> winforms = m.mono_assembly_get_image(m.mono_assembly_load_with_partial_name(pointerToCString("System.Windows.Forms"), loadStatus));
		
		m.mono_assemblies_init();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				m.mono_jit_cleanup(domain);
			}
		});
		
		Pointer<MonoObject> form = newInstance(m, domain, m.mono_class_from_name(winforms, pointerToCString("System.Windows.Forms"), pointerToCString("Form")));
		setProperty(m, domain, form, "Text", "Mono + JNAerator + JNA");
		
		Pointer<MonoObject> button = newInstance(m, domain, m.mono_class_from_name(winforms, pointerToCString("System.Windows.Forms"), pointerToCString("Button")));
		setProperty(m, domain, button, "Text", "Hello .NET World\n(spawned from Java)");
		//setProperty(m, domain, button, "Dock", 5 .*DockStyle.Fill*.);
		
		invokeMethod(m, domain, getProperty(m, domain, form, "Controls"), "Add", button);
		invokeMethod(m, domain, m.mono_class_from_name(winforms, pointerToCString("System.Windows.Forms"), pointerToCString("Application")), null, "Run", form);
	}
	private static Pointer<MonoObject> invokeMethod(MonoLibrary m, Pointer<MonoDomain> domain, Pointer<MonoObject> ob, String methodName, Pointer<MonoObject>... args) {
		return invokeMethod(m, domain, m.mono_object_get_class(ob), ob, methodName, args);
	}
	@SuppressWarnings("deprecation")
	private static Pointer<MonoObject> invokeMethod(MonoLibrary m, Pointer<MonoDomain> domain, Pointer<MonoClass> cl, Pointer<MonoObject> ob, String methodName, Pointer<MonoObject>... args) {
        /*Pointer<Pointer<?>> pargs = allocatePointers(args.length);
        for (int i = 0; i < args.length; i++)
        pargs.set(i, pointerTo(args[i]));
         */
        Pointer<Pointer<?>> pargs = (Pointer)pointerToPointers(args);
        return m.mono_runtime_invoke(m.mono_class_get_method_from_name(cl, pointerToCString(methodName), args.length), ob == null ? null : ob, pargs, null);
	}

	@SuppressWarnings("deprecation")
	private static Pointer<MonoObject> getProperty(MonoLibrary m, Pointer<MonoDomain> domain,
			Pointer<MonoObject> ob, String propertyName) {
		return m.mono_runtime_invoke(m.mono_property_get_get_method(m.mono_class_get_property_from_name(m.mono_object_get_class(ob), pointerToCString(propertyName))), ob.getPointer(), null, null);
				
	}

	private static Pointer<MonoObject> newInstance(MonoLibrary m, Pointer<MonoDomain> domain,
			Pointer<MonoClass> cl) {
		Pointer<MonoObject> ob = m.mono_object_new(domain, cl);
		m.mono_runtime_class_init(ob.get().vtable());
		m.mono_runtime_object_init(ob);
		
		return ob;
	}

	@SuppressWarnings("deprecation")
	static void setProperty(MonoLibrary m, Pointer<MonoDomain> domain, Pointer<MonoObject> ob, String propertyName, Object value) {
		Pointer<MonoClass> cl = m.mono_object_get_class(ob);

        Pointer<?> mem;
		//Memory mem = new Memory(Native.POINTER_SIZE), memPrims;
		if (value instanceof MonoObject)
            mem = pointerTo((MonoObject)value);
		else if (value instanceof String)
			mem = m.mono_string_new(domain, pointerToCString((String)value));
		else if (value instanceof Integer) {
			mem = pointerToInt((Integer)value);
		} else 
			throw new UnsupportedOperationException("implement me !");
		
		Pointer<Pointer<?>> pargs = allocatePointer();
		pargs.set(mem);
		m.mono_runtime_invoke(m.mono_property_get_set_method(m.mono_class_get_property_from_name(cl, pointerToCString(propertyName))), ob.getPointer(), pargs, null);
	}
}
