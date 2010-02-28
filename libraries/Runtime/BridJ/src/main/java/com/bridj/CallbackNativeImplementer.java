package com.bridj;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import org.objectweb.asm.*;

import com.bridj.*;
import com.bridj.NativeEntities.Builder;

//import org.objectweb.asm.attrs.*;
public class CallbackNativeImplementer extends ClassLoader implements Opcodes {

	Map<Class<? extends Callback>, Class<?>> implClasses = new HashMap<Class<? extends Callback>, Class<?>>();
	String implNameSuffix = "_NativeImpl";
	final NativeEntities nativeEntities;
    final CRuntime runtime;
	public CallbackNativeImplementer(NativeEntities nativeEntities, CRuntime runtime) {
		this.nativeEntities = nativeEntities;
        this.runtime = runtime;
	}
	/**
	 * The class created here is to be used to cast a pointer to a callback
	 * @param callbackType
	 * @return
	 */
	public synchronized <T extends Callback> Class<? extends T> getCallbackImplType(Class<T> callbackType) {
		Class<?> callbackImplType = implClasses.get(callbackType);
		if (callbackImplType == null) {
			try {
				String callbackTypeName = callbackType.getName().replace('.', '/');
				String callbackTypeImplName = callbackTypeName.replace('$', '_') + implNameSuffix;
				String sourceFile = callbackType.getSimpleName() + implNameSuffix + ".java";
				
				Method callbackMethod = null;
				for (Method method : callbackType.getDeclaredMethods()) {
					int modifiers = method.getModifiers();
					if (Modifier.isAbstract(modifiers)) {
						if (callbackMethod == null)
							callbackMethod = method;
						else
							throw new RuntimeException("Callback " + callbackType.getName() + " has more than one abstract method (" + callbackMethod + " and " + method + ")");
					}
				}
				if (callbackMethod == null)
					throw new RuntimeException("Callback " + callbackType.getName() + " doesn't have any abstract method.");
				
				Class<?>[] parameterTypes = callbackMethod.getParameterTypes();
				MethodCallInfo mci = new MethodCallInfo(callbackMethod);
				String methodName = callbackMethod.getName();
				String methodSignature = mci.getJavaSignature();
				
				byte[] byteArray = emitBytes(sourceFile, callbackTypeName, callbackTypeImplName, methodName, methodSignature);
				callbackImplType = defineClass(callbackTypeImplName.replace('/', '.'), byteArray, 0, byteArray.length);
                //Method[] methods = callbackImplType.getDeclaredMethods();
				//Method callbackMethodImpl = callbackImplType.getDeclaredMethod(methodName, parameterTypes);
				//mci.setMethod(callbackMethodImpl);
				//mci.setDeclaringClass(callbackImplType);
				//NativeEntities.Builder builder = new NativeEntities.Builder();
				//builder.addJavaToNativeCallback(mci);
				//nativeEntities.addDefinitions(callbackType, builder);
				runtime.register(callbackImplType);
                implClasses.put(callbackType, callbackImplType);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to create implementation class for callback type " + callbackType.getName(), ex);
			}
		}
		return (Class)callbackImplType;
	}

    static String classSig(Class c) {
        return "L" + c.getName().replace('.', '/') + ";";
    }
	private byte[] emitBytes(String sourceFile, String callbackTypeName,
			String callbackTypeImplName, String methodName,
			String methodSignature) {
		ClassWriter cw = new ClassWriter(0);
		MethodVisitor mv;
		cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER,
				callbackTypeImplName, null,
				callbackTypeName, null);

		cw.visitSource(sourceFile, null);

//		{
//	        AnnotationVisitor av = cw.visitAnnotation(classSig(com.bridj.ann.Runtime.class), true);
//	        av.visit("value", Type.getType(classSig(CRuntime.class)));
//	        av.visitEnd();
//		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			mv.visitLabel(l0);
			mv.visitLineNumber(5, l0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, callbackTypeName,
					"<init>", "()V");
			mv.visitInsn(RETURN);
			Label l1 = new Label();
			mv.visitLabel(l1);
			mv.visitLocalVariable("this",
					"L" + callbackTypeImplName + ";", null, l0, l1, 0);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC + ACC_NATIVE, methodName, methodSignature, null, null);
			mv.visitEnd();
		}
		cw.visitEnd();
		
		return cw.toByteArray();
	}
}
