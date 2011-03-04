package org.bridj;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.*;

import org.bridj.*;
import org.bridj.NativeEntities.Builder;
import org.bridj.ann.Convention;
import org.bridj.util.Pair;
import org.objectweb.asm.signature.SignatureWriter;

//import org.objectweb.asm.attrs.*;
class CallbackNativeImplementer extends ClassLoader implements Opcodes {

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
	 */
	public synchronized <T extends Callback> Class<? extends T> getCallbackImplType(Class<T> callbackType, NativeLibrary forcedLibrary) {
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
				String methodSignature = mci.getJavaSignature();//mci.getASMSignature();
				
				byte[] byteArray = emitBytes(sourceFile, callbackTypeName, callbackTypeImplName, methodName, methodSignature);
				callbackImplType = defineClass(callbackTypeImplName.replace('/', '.'), byteArray, 0, byteArray.length);
                //Method[] methods = callbackImplType.getDeclaredMethods();
				//Method callbackMethodImpl = callbackImplType.getDeclaredMethod(methodName, parameterTypes);
				//mci.setMethod(callbackMethodImpl);
				//mci.setDeclaringClass(callbackImplType);
				//NativeEntities.Builder builder = new NativeEntities.Builder();
				//builder.addJavaToNativeCallback(mci);
				//nativeEntities.addDefinitions(callbackType, builder);
                implClasses.put(callbackType, callbackImplType);
				runtime.register(callbackImplType, forcedLibrary);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to create implementation class for callback type " + callbackType.getName() + " : " + ex, ex);
			}
		}
		return (Class)callbackImplType;
	}
    protected Map<Pair<NativeLibrary, List<Class<?>>>, DynamicFunctionFactory> dynamicCallbacks = new HashMap<Pair<NativeLibrary, List<Class<?>>>, DynamicFunctionFactory>();
    public synchronized DynamicFunctionFactory getDynamicCallback(NativeLibrary library, Convention.Style callingConvention, Class<?> returnType, Class<?>... paramTypes) {
        List<Class<?>> list = new ArrayList<Class<?>>(paramTypes.length + 1);
        list.add(returnType);
        list.addAll(Arrays.asList(paramTypes));
        Pair<NativeLibrary, List<Class<?>>> key = new Pair<NativeLibrary, List<Class<?>>>(library, list);
        DynamicFunctionFactory cb = dynamicCallbacks.get(key);
        if (cb == null) {
            try {
                StringBuilder javaSig = new StringBuilder("("), desc = new StringBuilder();
                for (Class<?> paramType : paramTypes) {
                    javaSig.append(classSig(paramType));
                    desc.append(typeDesc(paramType));
                }
                javaSig.append(")").append(classSig(returnType));
                desc.append("To").append(typeDesc(returnType));

                String callbackTypeImplName = "org/bridj/dyncallbacks/" + desc;
                String methodName = "apply";

                byte[] byteArray = emitBytes("<anonymous>", DynamicFunction.class.getName().replace(".", "/"), callbackTypeImplName, methodName, javaSig.toString());
                Class<? extends DynamicFunction> callbackImplType = (Class)defineClass(callbackTypeImplName.replace('/', '.'), byteArray, 0, byteArray.length);
                runtime.register(callbackImplType);
                cb = new DynamicFunctionFactory(callbackImplType, callbackImplType.getMethod(methodName, paramTypes), callingConvention);
                dynamicCallbacks.put(key, cb);
            } catch (Throwable th) {
                throw new RuntimeException("Failed to create callback for " + list + " : " + th, th);
            }
        }
        return cb;
    }
    static String classSig(Class c) {
        if (c.isPrimitive()) {
            if (c == void.class)
                return "V";
            if (c == int.class)
                return "I";
            if (c == long.class)
                return "J";
            if (c == short.class)
                return "S";
            if (c == double.class)
                return "D";
            if (c == float.class)
                return "F";
            if (c == byte.class)
                return "B";
            if (c == boolean.class)
                return "Z";
            if (c == char.class)
                return "C";
            throw new RuntimeException("unexpected case");
        } else if (c.isArray()) {
            return "[" + classSig(c.getComponentType());
        }
        return "L" + c.getName().replace('.', '/') + ";";
    }
    static String typeDesc(Class c) {
        if (c.isPrimitive()) {
            String s = c.getSimpleName();
            return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        } else if (c.isArray()) {
            return typeDesc(c.getComponentType()) + "Array";
        }
        return c.getName().replace('.', '_');
    }
	private byte[] emitBytes(String sourceFile, String callbackTypeName,
			String callbackTypeImplName, String methodName,
			String methodSignature) {
		ClassWriter cw = new ClassWriter(0);
		cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER,
				callbackTypeImplName, null,
				callbackTypeName, null);

		cw.visitSource(sourceFile, null);

//		{
//	        AnnotationVisitor av = cw.visitAnnotation(classSig(org.bridj.ann.Runtime.class), true);
//	        av.visit("value", Type.getType(classSig(CRuntime.class)));
//	        av.visitEnd();
//		}
		{
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
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
			MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_NATIVE, methodName, methodSignature, null, null);
			mv.visitEnd();
		}
		cw.visitEnd();
		
		return cw.toByteArray();
	}
}
