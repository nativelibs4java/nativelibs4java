package com.bridj;

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.Arrays;

import com.bridj.Pointer;
import com.bridj.Demangler.MemberRef.Type;
import com.bridj.ann.Constructor;
import com.bridj.ann.Destructor;
import com.bridj.ann.This;

public abstract class Demangler {
	public static class DemanglingException extends Exception {
		public DemanglingException(String mess) {
			super(mess);
		}
	}
	public abstract MemberRef parseSymbol() throws DemanglingException;

	protected final String str;
	protected final int length;
	protected int position = 0;
	protected final NativeLibrary library;
	public Demangler(NativeLibrary library, String str) {
		this.str = str;
		this.length = str.length();
		this.library = library;
	}
	
	protected void expectChars(char... cs) throws DemanglingException {
		for (char c : cs) {
			char cc = consumeChar();
			if (cc != c)
				throw error("Expected char '" + c + "', found '" + cc + "'", -1);
		}
	}
	protected void expectAnyChar(char... cs) throws DemanglingException {
		char cc = consumeChar();
		for (char c : cs) {
			if (cc == c)
				return;
		}
		throw error("Expected any of " + Arrays.toString(cs) + ", found '" + cc + "'", -1);
	}
	public static StringBuilder implode(StringBuilder b, Object[] items, String sep) {
		return implode(b, Arrays.asList(items), sep);
	}
	public static StringBuilder implode(StringBuilder b, Iterable<?> items, String sep) {
		boolean first = true;
		for (Object item : items) {
			if (first)
				first = false;
			else
				b.append(sep);
			b.append(item);
		}
		return b;
	}
	protected char peekChar() {
		return position >= length ? 0 : str.charAt(position);
	}
	protected char lastChar() {
		return position == 0 ? 0 : str.charAt(position - 1);
	}
	protected char consumeChar() {
		char c = peekChar();
		if (c != 0)
			position++;
		return c;
	}
	protected boolean consumeCharIf(char... allowedChars) {
		char c = peekChar();
		for (char allowedChar : allowedChars)
			if (allowedChar == c) {
				position++;
				return true;
			}
		return false;
	}
	protected DemanglingException error(int deltaPosition) {
		return error(null, deltaPosition);
	}
	protected DemanglingException error(String mess, int deltaPosition) {
		StringBuilder err = new StringBuilder(position + 1);
		int position = this.position + deltaPosition;
		for (int i = 0; i < position; i++)
			err.append(' ');
		err.append('^');
		return new DemanglingException("Parsing error at position " + position + (mess == null ? "" : ": " + mess) + " \n\t" + str + "\n\t" + err);
	}
	
	public interface TemplateArg {
		
	}


	public static class Symbol {
		final String symbol;
		long address;
		MemberRef ref;
		boolean refParsed;
		final NativeLibrary library;
		
		public Symbol(String symbol, NativeLibrary library) {
			this.symbol = symbol;
			this.library = library;
			
		}

        public long getAddress() {
            if (address == 0)
                address = library.getSymbolAddress(symbol);
            return address;
        }
		public boolean matches(Method method) {
			if (!symbol.contains(method.getName()))
				return false;
		
			if (!Modifier.isStatic(method.getModifiers()) && !symbol.contains(method.getDeclaringClass().getSimpleName()))
				return false;
		
			parse();

            try {
                if (ref != null)
                    return ref.matches(method);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return false;
		}
		void parse() { 
			if (!refParsed) {
				try {
					ref = library.parseSymbol(symbol);
				} catch (DemanglingException ex) {
					ex.printStackTrace();
				}
				refParsed = true;
			}
		}

        public String getName() {
            return symbol;
        }

		public boolean matchesConstructor(Class<?> type) {
			if (!symbol.contains(type.getSimpleName()))
				return false;
		
			parse();

            try {
                if (ref != null)
                    return ref.matchesConstructor(type);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return false;
		}

		public boolean isVirtualTable() {
			// TODO Auto-generated method stub
			return false;
		}

	}

	public static abstract class TypeRef implements TemplateArg {
		public abstract StringBuilder getQualifiedName(StringBuilder b, boolean generic);
		public boolean matches(Class<?> type) {
			return getQualifiedName(new StringBuilder(), false).toString().equals(type.getName());
		}
		
	}

	public static class Constant implements TemplateArg {
		Object value;
	}

	public static class NamespaceRef extends TypeRef {
		String[] namespace;
        public NamespaceRef(String[] namespace) {
            this.namespace = namespace;
        }
		public StringBuilder getQualifiedName(StringBuilder b, boolean generic) {
			return implode(b, namespace, ".");
		}
	}

    protected static TypeRef classType(final Class<?> c, Class<? extends Annotation>... annotations) {
        return classType(c, null, annotations);
    }
	protected static TypeRef classType(final Class<?> c, final java.lang.reflect.Type[] genericTypes, Class<? extends Annotation>... annotations) {
		JavaTypeRef tr = new JavaTypeRef();
        if (genericTypes == null)
            tr.type = c;
        else
            tr.type = new ParameterizedType() {

                @Override
                public java.lang.reflect.Type[] getActualTypeArguments() {
                    return genericTypes;
                }

                @Override
                public java.lang.reflect.Type getOwnerType() {
                    return null;
                }

                @Override
                public java.lang.reflect.Type getRawType() {
                    return c;
                }
            };
            
		tr.annotations = annotations;
		return tr;
	}
	public static class JavaTypeRef extends TypeRef {

		java.lang.reflect.Type type;
        Class<? extends Annotation>[] annotations;
		
		Class<?> getTypeClass() {
			if (type instanceof Class<?>)
				return (Class<?>)type;
			if (type instanceof ParameterizedType)
				return (Class<?>)((ParameterizedType)type).getRawType();
			throw new UnsupportedOperationException("Unknown type type : " + type.getClass().getName());
		}
		@Override
		public StringBuilder getQualifiedName(StringBuilder b, boolean generic) {
			return b.append(getTypeClass().getName());
		}
		@Override
		public boolean matches(Class<?> type) {
            Class<?> tc = getTypeClass();
            if ((type == Long.TYPE && Pointer.class.isAssignableFrom(tc)) ||
                    (Pointer.class.isAssignableFrom(type) && tc == Long.TYPE))
                return true;
            
			return type.equals(getTypeClass()); // TODO isAssignableFrom or the opposite, depending on context
		}
		
	}
	public static class ClassRef extends TypeRef {
		private TypeRef enclosingType;
		private String simpleName;
		TemplateArg[] templateArguments;
		
		public StringBuilder getQualifiedName(StringBuilder b, boolean generic) {
			if (getEnclosingType() instanceof ClassRef) {
				getEnclosingType().getQualifiedName(b, generic).append('$');
			} else if (getEnclosingType() instanceof NamespaceRef) {
				getEnclosingType().getQualifiedName(b, generic).append('.');
			}
			b.append(getSimpleName());
			if (generic && templateArguments != null) {
				int args = 0;
				for (int i = 0, n = templateArguments.length; i < n; i++) {
					TemplateArg arg = templateArguments[i];
					if (!(arg instanceof TypeRef))
						continue;
					
					if (args == 0)
						b.append('<');
					else
						b.append(", ");
					((TypeRef)arg).getQualifiedName(b, generic);
					args++;
				}
				if (args > 0)
					b.append('>');
			}
			return b;
		}

		public void setSimpleName(String simpleName) {
			this.simpleName = simpleName;
		}

		public String getSimpleName() {
			return simpleName;
		}

		public void setEnclosingType(TypeRef enclosingType) {
			this.enclosingType = enclosingType;
		}

		public TypeRef getEnclosingType() {
			return enclosingType;
		}
	}

	public static void constructorPattern(@This long thisPtr) {}
	static Annotation[][] constructorPatternAnnotations;
	static {
		try {
			Demangler.class.getMethod("constructorPattern", Long.TYPE).getAnnotations();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	public static class FunctionTypeRef extends TypeRef {
		MemberRef function;

		@Override
		public StringBuilder getQualifiedName(StringBuilder b, boolean generic) {
			// TODO Auto-generated method stub
			return null;
		}
	}
	public static class MemberRef {
		public enum Type {
			Constructor, 
            InstanceMethod,
            StaticMethod,
            Destructor,
            New,
            Delete,
            OperatorAssign,
            OperatorRShift,
            OperatorDivideAssign,
            OperatorModuloAssign,
            OperatorRShiftAssign,
            OperatorLShiftAssign,
            OperatorBitAndAssign,
            OperatorBitOrAssign,
            OperatorXORAssign,
            VFTable,
            VBTable,
            VCall, // What is that ???
            TypeOf,
            
            CFunction,
            Field,
            ScalarDeletingDestructor
		}
		private TypeRef enclosingType;
		private TypeRef valueType;
		private String memberName;
		Boolean isStatic, isProtected, isPrivate;
		public Type type;
		public int modifiers;
		public TypeRef[] paramTypes;
		TemplateArg[] templateArguments;
		
		protected boolean matchesConstructor(Class<?> type) {
			
			if (getEnclosingType() != null && !getEnclosingType().matches(type))
				return false;
			
			if (getMemberName() != null && !getMemberName().equals(type.getSimpleName()))
				return false;
			
			if (getValueType() != null && !getValueType().matches(Void.TYPE))
				return false;
			
			Annotation[][] anns = constructorPatternAnnotations;
            Class<?>[] methodArgTypes = new Class[] { Long.TYPE };
            
            if (!matchesArgs(methodArgTypes, anns, true))
            	return false;
            
			return true;
		}
		protected boolean matches(Method method) {
			
			if (type == null)
            	return false;
            
			if (getEnclosingType() != null && !getEnclosingType().matches(method.getDeclaringClass()))
				return false;
			
			if (getMemberName() != null && !getMemberName().equals(method.getName()))
				return false;
			
			if (getValueType() != null && !getValueType().matches(method.getReturnType()))
				return false;
			
			Annotation[][] anns = method.getParameterAnnotations();
            Class<?>[] methodArgTypes = method.getParameterTypes();
            boolean hasThisAsFirstArgument = BridJ.hasThisAsFirstArgument(methodArgTypes, anns, true);
            
            if (!matchesArgs(methodArgTypes, anns, hasThisAsFirstArgument))
            	return false;
            
            
            int thisDirac = hasThisAsFirstArgument ? 1 : 0;
            switch (type) {
            case Constructor:
            case Destructor:
            	Annotation ann = method.getAnnotation(type == Type.Constructor ? Constructor.class : Destructor.class);
            	if (ann == null)
            		return false;
            	if (!hasThisAsFirstArgument)
            		return false;
            	if (methodArgTypes.length - thisDirac != 0 )
            		return false;
            	break;
            case InstanceMethod:
            	if (!hasThisAsFirstArgument)
            		return false;
            	break;
            case StaticMethod:
            	if (hasThisAsFirstArgument)
            		return false;
            	break;
            }
            return true;
		}
		private boolean matchesArgs(Class<?>[] methodArgTypes, Annotation[][] anns, boolean hasThisAsFirstArgument) {
			int totalArgs = 0;
            for (int i = 0, n = templateArguments == null ? 0 : templateArguments.length; i < n; i++) {
                if (totalArgs >= methodArgTypes.length)
                    return false;

                Class<?> paramType = methodArgTypes[i];

                TemplateArg arg = templateArguments[i];
                if (arg instanceof TypeRef) {
                    if (!paramType.equals(Class.class))
                        return false;
                } else if (arg instanceof Constant) {
                    try {
                        paramType.cast(((Constant)arg).value);
                    } catch (ClassCastException ex) {
                        return false;
                    }
                }
                totalArgs++;
            }
            
            if (hasThisAsFirstArgument)
            	totalArgs++;
            
            for (int i = 0, n = paramTypes == null ? 0 : paramTypes.length; i < n; i++) {
                if (totalArgs >= methodArgTypes.length)
                    return false;

                if (!paramTypes[i].matches(methodArgTypes[totalArgs]))
                    return false;

                totalArgs++;
            }
            return totalArgs == methodArgTypes.length;
		}
		public void setMemberName(String memberName) {
			this.memberName = memberName;
		}
		public String getMemberName() {
			return memberName;
		}
		public void setValueType(TypeRef valueType) {
			this.valueType = valueType;
		}
		public TypeRef getValueType() {
			return valueType;
		}
		public void setEnclosingType(TypeRef enclosingType) {
			this.enclosingType = enclosingType;
		}
		public TypeRef getEnclosingType() {
			return enclosingType;
		}
	}
}
