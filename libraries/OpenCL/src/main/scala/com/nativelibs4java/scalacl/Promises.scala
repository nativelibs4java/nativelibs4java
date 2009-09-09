package com.nativelibs4java.scalacl



/*
	object Test extends Application {
		class VarRef[T](var ref: => T)

		implicit def var2VarRef[T](ref: => T) = VarRef(ref)

	}
	trait Value[T] {
		var value: T;
	}
	trait DirectValue[T](override value: T) extends Value[T]
	                                                      implicit def 
	                                                      trait AsyncValue[T](promise: => T) extends Value[T] {
		private var val: T;
	var t = new Thread { override def run() = { val = promise() } }
	t.start();

	override def value = {
			t.join();
			return val;
	}
	}
	object AsyncValue {
		implicit def value2async[T](v: T) = AsyncValue[T](v)
		implicit def async2value[T](v: AsyncValue[T]) = v.value
	}
 */
