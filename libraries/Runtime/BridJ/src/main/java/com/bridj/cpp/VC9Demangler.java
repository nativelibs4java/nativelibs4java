package com.bridj.cpp;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.bridj.Demangler;
import com.bridj.NativeLibrary;
import com.bridj.Pointer;
import com.bridj.Demangler.ClassRef;
import com.bridj.Demangler.DemanglingException;
import com.bridj.Demangler.MemberRef;
import com.bridj.Demangler.NamespaceRef;
import com.bridj.Demangler.TypeRef;
import com.bridj.Demangler.MemberRef.Type;
import com.bridj.ann.CLong;
import com.bridj.ann.Wide;
import com.bridj.c.Callback;

public class VC9Demangler extends Demangler {
	public VC9Demangler(NativeLibrary library, String str) {
		super(library, str);
	}
	public MemberRef parseSymbol() throws DemanglingException {
		MemberRef mr = new MemberRef();
		
		if (consumeCharIf('?')) {
			boolean isFunctionOrMethod = str.endsWith("Z");
			boolean isMemberFunction = false;
			if (consumeCharIf('?')) {
				switch (consumeChar()) {
              	case '0': // "??1"
					mr.type = MemberRef.Type.Constructor;
					break;
				case '1': // "??1"
					mr.type = MemberRef.Type.Destructor;
					break;
                case '2':
                    mr.type = MemberRef.Type.New;
                    break;
                case '3':
				    mr.type = MemberRef.Type.Delete;
                    break;
                case '4':
				    mr.type = MemberRef.Type.OperatorAssign;
                    break;
                case '5':
				    mr.type = MemberRef.Type.OperatorRShift;
                    break;
                case '_': 
                    switch (consumeChar()) {
                        case '0':
                            mr.type = MemberRef.Type.OperatorDivideAssign;
                            break;
                        case '1':
                            mr.type = MemberRef.Type.OperatorModuloAssign;
                            break;
                        case '2':
                            mr.type = MemberRef.Type.OperatorLShiftAssign;
                            break;
                        case '3':
                            mr.type = MemberRef.Type.OperatorRShiftAssign;
                            break;
                        case '4':
                            mr.type = MemberRef.Type.OperatorBitAndAssign;
                            break;
                        case '5':
                            mr.type = MemberRef.Type.OperatorBitOrAssign;
                            break;
                        case '6':
                            mr.type = MemberRef.Type.OperatorXORAssign;
                            break;
                    }
					mr.type = MemberRef.Type.ScalarDeletingDestructor;
					break;
				default:
					throw error(-1);
				}
			}
			
			mr.setMemberName(parseName());
			List<String> ns = parseNames();
			
			if (isFunctionOrMethod) {
				char type = consumeChar();
				switch (type) {
				case 'Y':
					if (mr.type == null)
                        mr.type = MemberRef.Type.CFunction;
					break;
				case 'Q':
				case 'U': // WTF ??
					mr.modifiers = Modifier.PUBLIC;
                    if (mr.type == null)
                        mr.type = MemberRef.Type.InstanceMethod;
                    consumeCharIf('E');
					break;
				case 'A':
					mr.modifiers = Modifier.PRIVATE;
					if (mr.type == null)
                        mr.type = MemberRef.Type.InstanceMethod;
					break;
				case 'C':
					mr.modifiers = Modifier.PRIVATE | Modifier.STATIC;
					if (mr.type == null)
                        mr.type = MemberRef.Type.StaticMethod;
					break;
				case 'I':
					mr.modifiers = Modifier.PROTECTED;
					if (mr.type == null)
                        mr.type = MemberRef.Type.InstanceMethod;
					break;
				case 'K':
					mr.modifiers = Modifier.PROTECTED | Modifier.STATIC;
					if (mr.type == null)
                        mr.type = MemberRef.Type.StaticMethod;
					break;
				case 'S':
					mr.modifiers = Modifier.PUBLIC | Modifier.STATIC;
					if (mr.type == null)
                        mr.type = MemberRef.Type.StaticMethod;
					break;
				default:
					throw error(-1);
				}

                parseStorageMods();

                if (mr.type != MemberRef.Type.Destructor) {
                    mr.setValueType(parseType(true));
                    List<TypeRef> paramTypes = parseParams();
                    mr.paramTypes = paramTypes.toArray(new TypeRef[paramTypes.size()]);
                }
			}
			
			mr.setEnclosingType(reverseNamespace(ns));
		}
		return mr;
	}
	TypeRef parseType(boolean allowVoid) throws DemanglingException {
		switch (consumeChar()) {
		case '_':
			switch (consumeChar()) {
			case 'J':
			case 'K': // unsigned
				return classType(Long.TYPE);
			case 'W':
				return classType(Character.TYPE, Wide.class);
			default:
				throw error(-1);
			}
        case 'O':
            throw error("'long double' type cannot be mapped !", -1);
		case 'D':
		case 'E': // unsigned
		case 'C': // signed
			return classType(Byte.TYPE);
		case 'J':
		case 'K': // unsigned
			return classType(Long.TYPE, CLong.class);
		case 'H':
		case 'I': // unsigned
			return classType(Integer.TYPE);
		case 'F':
			return classType(Short.TYPE);
		case 'X':
            if (!allowVoid)
                return null;
			return classType(Void.TYPE);
		case 'M':
            expectChars('@');
			return classType(Float.TYPE);
		case 'N':
			expectChars('@');
			return classType(Double.TYPE);
		case 'P':
            if (peekChar() == '6') {
                consumeChar();
                parseStorageMods();
                
                return classType(Pointer.class, new java.lang.reflect.Type[] { Callback.class });
            }
        case 'Q': // array
        case 'A': // reference
            //TODO store these values :
            parseStorageMods();
			parseType(allowVoid); // TODO use it
			return classType(Pointer.class);
        case 'V': // class
        case 'U': // struct
        case 'T': // union
			//System.out.println("Found struct, class or union");
            return parseQualifiedTypeName();
		default:
			throw error(-1);
		}
	}
    static NamespaceRef reverseNamespace(List<String> names) {
        if (names == null || names.isEmpty())
            return null;
        Collections.reverse(names);
        return new NamespaceRef(names.toArray(new String[names.size()]));
    }
    TypeRef parseQualifiedTypeName() {
        List<String> names = parseNames();

        ClassRef tr = new ClassRef();
        tr.setSimpleName(names.get(0));
        names.remove(0);
        tr.setEnclosingType(reverseNamespace(names));
        return tr;
    }
	List<String> parseNames() {
		List<String> ns = new ArrayList<String>();
		while (peekChar() != '@')
			ns.add(parseName());

		consumeChar();
		return ns;
	}		
	String parseName() {
		StringBuilder b = new StringBuilder();
		char c;
		
		while ((c = consumeChar()) != '@')
			b.append(c);
		
		return b.toString();
	}

    void parseStorageMods() {
        switch (consumeChar()) {
            case 'E': // static member
                parseStorageMods();
                break;
            case 'I': // __fastcall
            case 'G': // __stdcall
            case 'B': // const
            case 'A': // default ?
                break;
            default:
                error(-1);
        }
    }

    private List<TypeRef> parseParams() throws DemanglingException {
        List<TypeRef> paramTypes = new ArrayList<TypeRef>();
        char c;
        while ((c = peekChar()) != '@' && c != 'Z' && c != 0) {
            TypeRef tr = parseType(false);
            if (tr == null)
                continue;
            paramTypes.add(tr);
        }

        return paramTypes;
    }
}