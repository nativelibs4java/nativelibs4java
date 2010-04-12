package com.bridj;

import com.bridj.ann.Convention.Style;
import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.Arrays;

import com.bridj.Pointer;
import com.bridj.ann.CLong;
import com.bridj.ann.Constructor;
import com.bridj.ann.Ptr;
import com.bridj.util.DefaultParameterizedType;

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
				throw error("Expected char '" + c + "', found '" + cc + "'");
		}
	}
	protected void expectAnyChar(char... cs) throws DemanglingException {
		char cc = consumeChar();
		for (char c : cs) {
			if (cc == c)
				return;
		}
		throw error("Expected any of " + Arrays.toString(cs) + ", found '" + cc + "'");
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
    protected boolean consumeCharsIf(char... nextChars) {
        int initialPosition = position;
        for (char c : nextChars) {
			char cc = consumeChar();
			if (cc != c) {
                position = initialPosition;
                return false;
            }
		}
        return true;
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
    protected DemanglingException error(String mess) {
        return error(mess, -1);
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

        @Override
        public String toString() {
            return symbol + (ref == null ? "" : " (" + ref + ")");
        }


        public long getAddress() {
            if (address == 0)
                address = library.getSymbolAddress(symbol);
            return address;
        }
		public boolean matches(Method method) {
			if (!symbol.contains(method.getName()))
				return false;
		
			//if (!Modifier.isStatic(method.getModifiers()) && !symbol.contains(method.getDeclaringClass().getSimpleName()))
			//	return false;
		
			parse();

            try {
                if (ref != null)
                    return ref.matches(method);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return false;
		}
		public MemberRef getParsedRef() {
			parse();
			return ref;
		}
		void parse() { 
			if (!refParsed) {
				try {
					ref = library.parseSymbol(symbol);
				} catch (DemanglingException ex) {
                    System.err.println(ex);
					//ex.printStackTrace();
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
                if (ref != null) {
                	return ref.matchesConstructor(type);
                }
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
		public boolean matches(Type type) {
			return getQualifiedName(new StringBuilder(), false).toString().equals(getTypeClass(type).getName());
		}
		
	}

	public static class Constant implements TemplateArg {
		Object value;

        public Constant(Object value) {
            this.value = value;
        }


        @Override
        public String toString() {
            return value.toString();
        }
	}

	public static class NamespaceRef extends TypeRef {
		Object[] namespace;
        public NamespaceRef(Object[] namespace) {
            this.namespace = namespace;
        }
		public StringBuilder getQualifiedName(StringBuilder b, boolean generic) {
			return implode(b, namespace, ".");
		}

        @Override
        public String toString() {
            return getQualifiedName(new StringBuilder(), true).toString();
        }

	}

    public static class PointerType {

    }

    protected static TypeRef pointerType(TypeRef tr) {
        return classType(Pointer.class); // TODO
    }
    protected static TypeRef classType(final Class<?> c, Class<? extends Annotation>... annotations) {
        return classType(c, null, annotations);
    }
    protected static TypeRef classType(final Class<?> c, final java.lang.reflect.Type[] genericTypes, Class<? extends Annotation>... annotations) {
		JavaTypeRef tr = new JavaTypeRef();
        if (genericTypes == null)
            tr.type = c;
        else
            tr.type = new DefaultParameterizedType(c, genericTypes);
            
		tr.annotations = annotations;
		return tr;
	}
    protected static TypeRef simpleType(String name) {
		return new ClassRef(name);
	}
    static Class<?> getTypeClass(Type type) {
		
		if (type instanceof Class<?>)
			return (Class<?>)type;
		if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType)type;
			Class<?> c = (Class<?>)pt.getRawType();
			if (ValuedEnum.class.isAssignableFrom(c)) {
				Type[] types = pt.getActualTypeArguments();
				if (types == null || types.length != 1)
					c = int.class;
				else
					c = getTypeClass(pt.getActualTypeArguments()[0]);
			}
			return c;
		}
		throw new UnsupportedOperationException("Unknown type type : " + type.getClass().getName());
	}
	public static class JavaTypeRef extends TypeRef {

		java.lang.reflect.Type type;
        Class<? extends Annotation>[] annotations;
		
		@Override
		public StringBuilder getQualifiedName(StringBuilder b, boolean generic) {
			return b.append(getTypeClass(this.type).getName());
		}
		@Override
		public boolean matches(Type type) {
            Class<?> tc = getTypeClass(this.type);
            Class<?> typec = Demangler.getTypeClass(type);
            if (tc == typec)
                return true;
            
            if ((type == Long.TYPE && Pointer.class.isAssignableFrom(tc)) ||
                    (Pointer.class.isAssignableFrom(typec) && tc == Long.TYPE))
                return true;
            if (tc == CLong.class) {
                if ((typec == int.class || typec == Integer.class) && (JNI.CLONG_SIZE == 4) || typec == long.class || typec == Long.class)
                    return true;
            } else if (tc == SizeT.class) {
                if ((typec == int.class || typec == Integer.class) && (JNI.SIZE_T_SIZE == 4) || typec == long.class || typec == Long.class)
                    return true;
            }
            if ((tc == Character.TYPE || tc == Character.class || tc == short.class || tc == Short.class) && (typec == char.class || typec == Character.class))
                return true;

            if ((tc == Integer.class || tc == int.class) && ValuedEnum.class.isAssignableFrom(typec))
                return true;
            
			return typec.equals(tc); // TODO isAssignableFrom or the opposite, depending on context
		}

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            for (Class<?> ann : annotations)
                b.append(ann.getSimpleName()).append(' ');
            b.append((type instanceof Class<?>) ? ((Class<?>)type).getSimpleName() : type.toString());
            return b.toString();
        }
		
	}
	public static class ClassRef extends TypeRef {
		private TypeRef enclosingType;
		private Object simpleName;
		TemplateArg[] templateArguments;

		public ClassRef() {
			
		}
		public ClassRef(String simpleName) {
			this.simpleName = simpleName;
		}
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

		public void setSimpleName(Object simpleName) {
			this.simpleName = simpleName;
		}

		public Object getSimpleName() {
			return simpleName;
		}

		public void setEnclosingType(TypeRef enclosingType) {
			this.enclosingType = enclosingType;
		}

		public TypeRef getEnclosingType() {
			return enclosingType;
		}

        public void setTemplateArguments(TemplateArg[] templateArguments) {
            this.templateArguments = templateArguments;
        }

        public TemplateArg[] getTemplateArguments() {
            return templateArguments;
        }

        @Override
        public boolean matches(Type type) {
            if (!getTypeClass(type).getSimpleName().equals(simpleName))
                return false;
            
            return true;
        }



        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();

            if (enclosingType != null)
                b.append(enclosingType).append('.');

            b.append(simpleName);
            appendTemplateArgs(b, templateArguments);
            return b.toString();
        }


	}
    static void appendTemplateArgs(StringBuilder b, Object[] params) {
        if (params == null || params.length == 0)
            return;
        appendArgs(b, '<', '>', params);
    }
    static void appendArgs(StringBuilder b, char pre, char post, Object[] params) {
        b.append(pre);
        for (int i = 0; i < params.length; i++) {
            if (i != 0)
                b.append(", ");
            b.append(params[i]);
        }
        b.append(post);
    }
	public static class FunctionTypeRef extends TypeRef {
		MemberRef function;

        public FunctionTypeRef(MemberRef function) {
            this.function = function;
        }
		@Override
		public StringBuilder getQualifiedName(StringBuilder b, boolean generic) {
			// TODO Auto-generated method stub
			return null;
		}


        @Override
        public String toString() {
            return function.toString();
        }
	}
    public enum SpecialName {
        Constructor("", true, true),
        Destructor("", true, true),
        New("new", true, true),
        Delete("delete", true, true),
        VFTable("vftable", false, true),
        VBTable("vftable", false, true),
        VCall("vcall", false, false), // What is that ???
        TypeOf("typeof", false, false),
        ScalarDeletingDestructor("'scalar deleting destructor'", true, true),
        VectorDeletingDestructor("'vector deleting destructor'", true, true),
        
        OperatorAssign("operator=", true, true),
        OperatorRShift("operator>>", true, true),
        OperatorDivideAssign("operator/=", true, true),
        OperatorModuloAssign("operator%=", true, true),
        OperatorRShiftAssign("operator>>=", true, true),
        OperatorLShiftAssign("operator<<=", true, true),
        OperatorBitAndAssign("operator&=", true, true),
        OperatorBitOrAssign("operator|=", true, true),
        OperatorXORAssign("operator^=", true, true),
        OperatorLShift("operator<<", true, true),
        OperatorLogicNot("operator!", true, true),
        OperatorEquals("operator==", true, true),
        OperatorDifferent("operator!=", true, true),
        OperatorSquareBrackets("operator[]", true, true),
        OperatorCast("'some cast operator'", true, true),
        OperatorArrow("operator->", true, true),
        OperatorMultiply("operator*", true, true),
        OperatorIncrement("operator++", true, true),
        OperatorDecrement("operator--", true, true),
        OperatorSubstract("operator-", true, true),
        OperatorAdd("operator+", true, true),
        OperatorBitAnd("operator&=", true, true),
        /// Member pointer selector
        OperatorArrowStar("operator->*", true, true),
        OperatorDivide("operator/", true, true),
        OperatorModulo("operator%", true, true),
        OperatorLower("operator<", true, true),
        OperatorLowerEquals("operator<=", true, true),
        OperatorGreater("operator>", true, true),
        OperatorGreaterEquals("operator>=", true, true),
        OperatorComma("operator,", true, true),
        OperatorParenthesis("operator()", true, true),
        OperatorBitNot("operator~", true, true),
        OperatorXOR("operator^", true, true),
        OperatorBitOr("operator|", true, true),
        OperatorLogicAnd("operator&&", true, true),
        OperatorLogicOr("operator||", true, true),
        OperatorMultiplyAssign("operator*=", true, true),
        OperatorAddAssign("operator+=", true, true),
        OperatorSubstractAssign("operator-=", true, true);

        private SpecialName(String name, boolean isFunction, boolean isMember) {
            this.name = name;
            this.isFunction = isFunction;
            this.isMember = isMember;
        }
        final String name;
        final boolean isFunction;
        final boolean isMember;

        @Override
        public String toString() {
            return name;
        }

        public boolean isFunction() {
            return isFunction;
        }

        public boolean isMember() {
            return isMember;
        }




    }
	public static class MemberRef {

        private Integer argumentsStackSize;
		private TypeRef enclosingType;
		private TypeRef valueType;
		private Object memberName;
		Boolean isStatic, isProtected, isPrivate;
		public int modifiers;
		public TypeRef[] paramTypes, throwTypes;
		TemplateArg[] templateArguments;
        public Style callingConvention;

        public Integer getArgumentsStackSize() {
            return argumentsStackSize;
        }

        public void setArgumentsStackSize(Integer argumentsStackSize) {
            this.argumentsStackSize = argumentsStackSize;
        }
       
		protected boolean matchesConstructor(Class<?> type) {
			if (this.memberName != SpecialName.Constructor)
				return false;
                
			if (getEnclosingType() != null && !getEnclosingType().matches(type))
				return false;
			
			//if (getMemberName() != null && !getMemberName().equals(type.getSimpleName()))
			//	return false;
			
			if (getValueType() != null && !getValueType().matches(Void.TYPE))
				return false;
			
            Type[] methodArgTypes = new Type[] { Long.TYPE };
            if (!matchesArgs(methodArgTypes, null, true))
            	return false;
            
			return true;
		}
        static boolean hasInstance(Object[] array, Class<?>... cs) {
            for (Object o : array)
                for (Class<?> c : cs)
                    if (c.isInstance(o))
                        return true;
            return false;
        }
        static int getArgumentsStackSize(Method method) {
            int total = 0;
            Type[] paramTypes = method.getGenericParameterTypes();
            Annotation[][] anns = method.getParameterAnnotations();
            for (int iArg = 0, nArgs = paramTypes.length; iArg < nArgs; iArg++) {
                Class<?> paramType = getTypeClass(paramTypes[iArg]);
                if (paramType == int.class)
                    total += 4;
                else if (paramType == long.class) {
                    if (hasInstance(anns[iArg], Ptr.class, CLong.class))
                        total += Pointer.SIZE;
                    else
                        total += 8;
                } else if (paramType == float.class)
                    total += 4;
                else if (paramType == double.class)
                    total += 8;
                else if (paramType == byte.class)
                    total += 1;
                else if (paramType == char.class)
                    total += JNI.WCHAR_T_SIZE;
                else if (paramType == short.class)
                    total += 2;
                else if (paramType == boolean.class)
                    total += 1;
                else if (Pointer.class.isAssignableFrom(paramType))
                    total += Pointer.SIZE;
                else if (NativeObject.class.isAssignableFrom(paramType))
                    total += ((CRuntime)BridJ.getRuntime(paramType)).sizeOf((Class<? extends StructObject>) paramType, paramTypes[iArg], null);
                else if (FlagSet.class.isAssignableFrom(paramType))
                    total += 4; // TODO
                else
                    throw new RuntimeException("Type not handled : " + paramType.getName());
            }
            return total;
        }
		protected boolean matches(Method method) {

			if (memberName instanceof SpecialName)
            	return false; // use matchesConstructor... 

            if (getArgumentsStackSize() != null && getArgumentsStackSize().intValue() != getArgumentsStackSize(method))
                return false;
            
			if (getEnclosingType() != null && !getEnclosingType().matches(method.getDeclaringClass()))
				return false;
			
			if (getMemberName() != null && !getMemberName().equals(method.getName()))
				return false;
			
			if (getValueType() != null && !getValueType().matches(method.getReturnType()))
				return false;
			
			Annotation[][] anns = method.getParameterAnnotations();
//            Class<?>[] methodArgTypes = method.getParameterTypes();
            Type[] parameterTypes = method.getGenericParameterTypes();
            boolean hasThisAsFirstArgument = BridJ.hasThisAsFirstArgument(method);//methodArgTypes, anns, true);
            
            if (!matchesArgs(parameterTypes, anns, hasThisAsFirstArgument))
            	return false;
            
            
            //int thisDirac = hasThisAsFirstArgument ? 1 : 0;
            /*
            switch (type) {
            case Constructor:
            case Destructor:
            	Annotation ann = method.getAnnotation(type == SpecialName.Constructor ? Constructor.class : Destructor.class);
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
            }*/
            
            return true;
		}
		private boolean matchesArgs(Type[] parameterTypes, Annotation[][] anns, boolean hasThisAsFirstArgument) {
			int totalArgs = 0;
            for (int i = 0, n = templateArguments == null ? 0 : templateArguments.length; i < n; i++) {
                if (totalArgs >= parameterTypes.length)
                    return false;

                Type paramType = parameterTypes[i];

                TemplateArg arg = templateArguments[i];
                if (arg instanceof TypeRef) {
                    if (!paramType.equals(Class.class))
                        return false;
                } else if (arg instanceof Constant) {
                    try {
                        getTypeClass(paramType).cast(((Constant)arg).value);
                    } catch (ClassCastException ex) {
                        return false;
                    }
                }
                totalArgs++;
            }
            
            if (hasThisAsFirstArgument)
            	totalArgs++;
            
            for (int i = 0, n = paramTypes == null ? 0 : paramTypes.length; i < n; i++) {
                if (totalArgs >= parameterTypes.length)
                    return false;

                if (!paramTypes[i].matches(parameterTypes[totalArgs]))
                    return false;

                totalArgs++;
            }
            if (totalArgs != parameterTypes.length)
                return false;

            return true;
		}

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();

            b.append(valueType).append(' ');
            boolean nameWritten = false;
            if (enclosingType != null) {
                b.append(enclosingType);
                b.append('.');
                if (memberName instanceof SpecialName) {
                    switch ((SpecialName)memberName) {
                        case Destructor:
                            b.append('~');
                        case Constructor:
                            b.append(((ClassRef)enclosingType).simpleName);
                            nameWritten = true;
                            break;
                    }
                }
            }
            if (!nameWritten)
                b.append(memberName);
            
            appendTemplateArgs(b, templateArguments);
            appendArgs(b, '(', ')', paramTypes);
            return b.toString();
        }
        
		public void setMemberName(Object memberName) {
			this.memberName = memberName;
		}
		public Object getMemberName() {
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
