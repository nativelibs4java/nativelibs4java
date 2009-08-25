import java.io.File;
import java.io.FileNotFoundException;
import java.nio.IntBuffer;

import com.nativelibs4java.mono.MonoLibrary;
import com.nativelibs4java.mono.MonoObject;
import com.nativelibs4java.mono.MonoLibrary.MonoClass;
import com.nativelibs4java.mono.MonoLibrary.MonoDomain;
import com.nativelibs4java.mono.MonoLibrary.MonoImage;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.ptr.PointerByReference;

public class HelloDotNetWorld {
	public static void main(String[] args) {
		try {
			String path = "C:\\Program Files\\Mono-2.4.2.3\\bin\\mono.dll";
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
		
		final MonoLibrary m = MonoLibrary.INSTANCE;
		final MonoDomain domain = m.mono_jit_init(monoDLL.toString());
		if (domain == null)
			throw new RuntimeException("Unable to init Mono's runtime !");

		IntBuffer loadStatus = IntBuffer.wrap(new int[1]);
		MonoImage winforms = m.mono_assembly_get_image(m.mono_assembly_load_with_partial_name("System.Windows.Forms", loadStatus));
		
		m.mono_assemblies_init();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				m.mono_jit_cleanup(domain);
			}
		});
		
		MonoObject form = newInstance(m, domain, m.mono_class_from_name(winforms, "System.Windows.Forms", "Form"));
		setProperty(m, domain, form, "Text", "Mono + JNAerator + JNA");
		
		MonoObject button = newInstance(m, domain, m.mono_class_from_name(winforms, "System.Windows.Forms", "Button"));
		setProperty(m, domain, button, "Text", "Hello .NET World\n(spawned from Java)");
		setProperty(m, domain, button, "Dock", 5 /*DockStyle.Fill*/);
		
		invokeMethod(m, domain, getProperty(m, domain, form, "Controls"), "Add", button);
		invokeMethod(m, domain, m.mono_class_from_name(winforms, "System.Windows.Forms", "Application"), null, "Run", form);
	}
	private static MonoObject invokeMethod(MonoLibrary m, MonoDomain domain, MonoObject ob, String methodName, MonoObject... args) {
		return invokeMethod(m, domain, m.mono_object_get_class(ob), ob, methodName, args);
	}
	private static MonoObject invokeMethod(MonoLibrary m, MonoDomain domain, MonoClass cl, MonoObject ob, String methodName, MonoObject... args) {
		Memory mem = new Memory(args.length * Native.POINTER_SIZE);
		for (int i = 0; i < args.length; i++)
			mem.setPointer(i * Native.POINTER_SIZE, args[i].getPointer());

		PointerByReference pargs = new PointerByReference();
		pargs.setPointer(mem.share(0));
		return m.mono_runtime_invoke(m.mono_class_get_method_from_name(cl, methodName, args.length), ob == null ? null : ob.getPointer(), pargs, (PointerByReference)null);
	}

	private static MonoObject getProperty(MonoLibrary m, MonoDomain domain,
			MonoObject ob, String propertyName) {
		return m.mono_runtime_invoke(m.mono_property_get_get_method(m.mono_class_get_property_from_name(m.mono_object_get_class(ob), propertyName)), ob.getPointer(), null, (PointerByReference)null);
				
	}

	private static MonoObject newInstance(MonoLibrary m, MonoDomain domain,
			MonoClass cl) {
		MonoObject ob = m.mono_object_new(domain, cl);
		m.mono_runtime_class_init(ob.vtable);
		m.mono_runtime_object_init(ob);
		
		return ob;
	}

	static void setProperty(MonoLibrary m, MonoDomain domain, MonoObject ob, String propertyName, Object value) {
		MonoClass cl = m.mono_object_get_class(ob);
		
		Memory mem = new Memory(Native.POINTER_SIZE), memPrims;
		if (value instanceof MonoObject)
			mem.setPointer(0, ((MonoObject)value).getPointer());
		else if (value instanceof String)
			mem.setPointer(0, m.mono_string_new(domain, (String)value).object.getPointer());
		else if (value instanceof Integer) {
			memPrims = new Memory(8);
			memPrims.setInt(0, (Integer)value);
			mem.setPointer(0, memPrims.share(0));
		} else 
			throw new UnsupportedOperationException("implement me !");
		
		PointerByReference pargs = new PointerByReference();
		pargs.setPointer(mem.share(0));
		m.mono_runtime_invoke(m.mono_property_get_set_method(m.mono_class_get_property_from_name(cl, propertyName)), ob.getPointer(), pargs, (PointerByReference)null);
	}
}
