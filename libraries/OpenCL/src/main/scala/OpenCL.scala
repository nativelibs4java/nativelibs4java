trait OpenCL_scala extends com.nativelibs4java.opencl.OpenCLLibrary {
class pfn_notify_scala(scala_func: (com.sun.jna.ptr.ByteByReference, com.sun.jna.Pointer, com.sun.jna.NativeLong, com.sun.jna.Pointer) => Unit) extends com.nativelibs4java.opencl.OpenCLLibrary.pfn_notify {
	override def invoke(charPtr1: com.sun.jna.ptr.ByteByReference, voidPtr1: com.sun.jna.Pointer, size_t1: com.sun.jna.NativeLong, voidPtr2: com.sun.jna.Pointer): Unit = {
		scala_func(charPtr1, voidPtr1, size_t1, voidPtr2)
	}
}
implicit def scala_func2pfn_notify_scala(scala_func: (com.sun.jna.ptr.ByteByReference, com.sun.jna.Pointer, com.sun.jna.NativeLong, com.sun.jna.Pointer) => Unit) = {
	new pfn_notify_scala(scala_func)
}
class pfn_notify2_scala(scala_func: (com.sun.jna.ptr.ByteByReference, com.sun.jna.Pointer, com.sun.jna.NativeLong, com.sun.jna.Pointer) => Unit) extends com.nativelibs4java.opencl.OpenCLLibrary.pfn_notify2 {
	override def invoke(charPtr1: com.sun.jna.ptr.ByteByReference, voidPtr1: com.sun.jna.Pointer, size_t1: com.sun.jna.NativeLong, voidPtr2: com.sun.jna.Pointer): Unit = {
		scala_func(charPtr1, voidPtr1, size_t1, voidPtr2)
	}
}
implicit def scala_func2pfn_notify2_scala(scala_func: (com.sun.jna.ptr.ByteByReference, com.sun.jna.Pointer, com.sun.jna.NativeLong, com.sun.jna.Pointer) => Unit) = {
	new pfn_notify2_scala(scala_func)
}
class pfn_notify3_scala(scala_func: (com.nativelibs4java.opencl.OpenCLLibrary.cl_program, com.sun.jna.Pointer) => Unit) extends com.nativelibs4java.opencl.OpenCLLibrary.pfn_notify3 {
	override def invoke(cl_program1: com.nativelibs4java.opencl.OpenCLLibrary.cl_program, voidPtr1: com.sun.jna.Pointer): Unit = {
		scala_func(cl_program1, voidPtr1)
	}
}
implicit def scala_func2pfn_notify3_scala(scala_func: (com.nativelibs4java.opencl.OpenCLLibrary.cl_program, com.sun.jna.Pointer) => Unit) = {
	new pfn_notify3_scala(scala_func)
}
class user_func_scala(scala_func: com.sun.jna.Pointer => Unit) extends com.nativelibs4java.opencl.OpenCLLibrary.user_func {
	override def invoke(voidPtr1: com.sun.jna.Pointer): Unit = {
		scala_func(voidPtr1)
	}
}
implicit def scala_func2user_func_scala(scala_func: com.sun.jna.Pointer => Unit) = {
	new user_func_scala(scala_func)
}
class clSetMemObjectDestructorAPPLE_arg1_callback_scala(scala_func: com.sun.jna.Pointer => Unit) extends com.nativelibs4java.opencl.OpenCLLibrary.clSetMemObjectDestructorAPPLE_arg1_callback {
	override def invoke(voidPtr1: com.sun.jna.Pointer): Unit = {
		scala_func(voidPtr1)
	}
}
implicit def scala_func2clSetMemObjectDestructorAPPLE_arg1_callback_scala(scala_func: com.sun.jna.Pointer => Unit) = {
	new clSetMemObjectDestructorAPPLE_arg1_callback_scala(scala_func)
}
}
